package com.unibank.bankingSystem.service;

import com.unibank.bankingSystem.config.RabbitMQConfig;
import com.unibank.bankingSystem.dto.AuthResponse;
import com.unibank.bankingSystem.dto.ForgotPasswordRequest;
import com.unibank.bankingSystem.dto.LoginRequest;
import com.unibank.bankingSystem.dto.RegisterRequest;
import com.unibank.bankingSystem.dto.ResetPasswordRequest;
import com.unibank.bankingSystem.exception.AccountLockedException;
import com.unibank.bankingSystem.exception.BadRequestException;
import com.unibank.bankingSystem.exception.DuplicateResourceException;
import com.unibank.bankingSystem.exception.ResourceNotFoundException;
import com.unibank.bankingSystem.messaging.PasswordResetEmailMessage;
import com.unibank.bankingSystem.model.PasswordResetToken;
import com.unibank.bankingSystem.model.Role;
import com.unibank.bankingSystem.model.User;
import com.unibank.bankingSystem.repository.PasswordResetTokenRepository;
import com.unibank.bankingSystem.repository.UserRepository;
import com.unibank.bankingSystem.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 30;
    private static final Duration FORGOT_PASSWORD_COOLDOWN = Duration.ofSeconds(60);

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RateLimiter rateLimiter;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public AuthResponse register(RegisterRequest request) {
        if(userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use");
        }

        if(userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already in use");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setRole(Role.CUSTOMER);

        userRepository.save(user);

        return new AuthResponse(jwtService.generateToken(user));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            String until = user.getLockedUntil().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            throw new AccountLockedException(
                    "Too many failed login attempts. Account is locked until " + until + "."
            );
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {
            registerFailedAttempt(user);
            throw ex;
        }

        if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }

        return new AuthResponse(jwtService.generateToken(user));
    }

    private void registerFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
        }

        userRepository.save(user);
    }

    // Deliberately returns the same result whether or not the email is registered -
    // an attacker probing this endpoint shouldn't be able to tell which emails exist
    // in the system just by seeing a different response. The rate-limit check lives
    // inside the ifPresent block (rather than before the lookup) on purpose: an unknown
    // email is already free to "request" as often as it likes since nothing gets sent,
    // so there's nothing to throttle there. Real accounts, on the other hand, could
    // otherwise be spammed with reset emails - so once found, a real user only gets a
    // fresh reset email once per cooldown window, regardless of how many requests come in.
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String rateLimitKey = "forgot-password:" + user.getEmail().toLowerCase();
            if (!rateLimiter.allow(rateLimitKey, FORGOT_PASSWORD_COOLDOWN)) {
                return;
            }

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRY_MINUTES));
            passwordResetTokenRepository.save(resetToken);

            String resetLink = frontendUrl + "/reset-password?token=" + resetToken.getToken();
            publishPasswordResetEmail(user.getEmail(), resetLink);
        });
    }

    // Publishing (instead of calling EmailService directly) means the actual SMTP
    // call happens on a RabbitMQ listener thread, not on this request - a slow or
    // unreachable mail server no longer makes the caller wait, and PasswordResetEmailListener
    // retries automatically on failure. If the broker itself is unreachable, the reset
    // token is already saved either way, so we log and let the caller see the same
    // response as always rather than surfacing a 500 for what looks like "did nothing."
    private void publishPasswordResetEmail(String email, String resetLink) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.PASSWORD_RESET_EMAIL_ROUTING_KEY,
                    new PasswordResetEmailMessage(email, resetLink)
            );
        } catch (AmqpException ex) {
            log.warn("Failed to publish password reset email message for {}: {}", email, ex.getMessage());
        }
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("This reset link is invalid or has expired"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This reset link is invalid or has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // A successful reset is also a legitimate way back in after a lockout.
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

}
