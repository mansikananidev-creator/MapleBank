package com.unibank.bankingSystem.service;

import com.unibank.bankingSystem.config.RabbitMQConfig;
import com.unibank.bankingSystem.dto.AuthResponse;
import com.unibank.bankingSystem.dto.ForgotPasswordRequest;
import com.unibank.bankingSystem.dto.LoginRequest;
import com.unibank.bankingSystem.dto.ResetPasswordRequest;
import com.unibank.bankingSystem.exception.AccountLockedException;
import com.unibank.bankingSystem.exception.BadRequestException;
import com.unibank.bankingSystem.messaging.PasswordResetEmailMessage;
import com.unibank.bankingSystem.model.PasswordResetToken;
import com.unibank.bankingSystem.model.User;
import com.unibank.bankingSystem.repository.PasswordResetTokenRepository;
import com.unibank.bankingSystem.repository.UserRepository;
import com.unibank.bankingSystem.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Covers the account-lockout feature: 5 failed logins should lock the account for
// 15 minutes, a locked account should refuse even a correct password, and a
// successful login should reset the failure counter.
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private RateLimiter rateLimiter;

    @InjectMocks
    private AuthService authService;

    private User user;
    private LoginRequest request;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setFailedLoginAttempts(0);

        request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("correct-password");

        ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:5173");
    }

    @Test
    void login_throwsAccountLockedException_whenAccountIsCurrentlyLocked() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        user.setLockedUntil(LocalDateTime.now().plusMinutes(10));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountLockedException.class);

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_locksAccountAfterFifthFailedAttempt() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        user.setFailedLoginAttempts(4);
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getLockedUntil()).isNotNull();
        assertThat(user.getLockedUntil()).isAfter(LocalDateTime.now());
        verify(userRepository).save(user);
    }

    @Test
    void login_doesNotLockAccountBeforeFifthFailedAttempt() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        user.setFailedLoginAttempts(1);
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(2);
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    void login_resetsFailedAttempts_onSuccessfulLogin() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        user.setFailedLoginAttempts(3);
        when(jwtService.generateToken(user)).thenReturn("fake-jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("fake-jwt-token");
        assertThat(user.getFailedLoginAttempts()).isZero();
        verify(userRepository).save(user);
    }

    @Test
    void forgotPassword_doesNothing_whenEmailIsNotRegistered() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("nobody@example.com");

        // Should not throw, and should not create a token or publish a message -
        // this is what stops the endpoint from being usable to discover valid emails.
        authService.forgotPassword(request);

        verify(passwordResetTokenRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void forgotPassword_createsTokenAndPublishesResetLink_whenUserExists() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(rateLimiter.allow(eq("forgot-password:user@example.com"), any(Duration.class))).thenReturn(true);

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@example.com");

        authService.forgotPassword(request);

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();

        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getToken()).isNotBlank();
        assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(savedToken.isUsed()).isFalse();

        ArgumentCaptor<PasswordResetEmailMessage> messageCaptor = ArgumentCaptor.forClass(PasswordResetEmailMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE),
                eq(RabbitMQConfig.PASSWORD_RESET_EMAIL_ROUTING_KEY),
                messageCaptor.capture()
        );
        PasswordResetEmailMessage message = messageCaptor.getValue();
        assertThat(message.getEmail()).isEqualTo("user@example.com");
        assertThat(message.getResetLink()).isEqualTo("http://localhost:5173/reset-password?token=" + savedToken.getToken());
    }

    @Test
    void forgotPassword_skipsTokenAndMessage_whenRateLimiterDisallows() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(rateLimiter.allow(eq("forgot-password:user@example.com"), any(Duration.class))).thenReturn(false);

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@example.com");

        // Should not throw - the caller sees the same response as always, it just
        // doesn't get another email since one was already sent recently.
        authService.forgotPassword(request);

        verify(passwordResetTokenRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void resetPassword_rejectsUnknownToken() {
        when(passwordResetTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("bad-token");
        request.setNewPassword("NewPassword1!");

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void resetPassword_rejectsExpiredToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken("expired-token");
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        token.setUsed(false);

        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("expired-token");
        request.setNewPassword("NewPassword1!");

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void resetPassword_rejectsAlreadyUsedToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken("used-token");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        token.setUsed(true);

        when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("used-token");
        request.setNewPassword("NewPassword1!");

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void resetPassword_updatesPasswordAndClearsAnyLockout_whenTokenIsValid() {
        user.setFailedLoginAttempts(4);
        user.setLockedUntil(LocalDateTime.now().plusMinutes(10));

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken("valid-token");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        token.setUsed(false);

        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPassword1!")).thenReturn("hashed-password");

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-token");
        request.setNewPassword("NewPassword1!");

        authService.resetPassword(request);

        assertThat(user.getPassword()).isEqualTo("hashed-password");
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(token.isUsed()).isTrue();
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(token);
    }
}
