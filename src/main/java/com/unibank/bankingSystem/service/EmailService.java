package com.unibank.bankingSystem.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // Failures are intentionally left to propagate rather than being caught here.
    // This method now only ever runs inside PasswordResetEmailListener, on a RabbitMQ
    // listener thread - Spring's listener retry (spring.rabbitmq.listener.simple.retry.*
    // in application.properties) is what's responsible for resilience now, and it only
    // retries if the exception actually reaches it. Swallowing it here would silently
    // defeat that retry and the dead-letter queue behind it.
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Reset your Maple Bank password");
        message.setText(
                "We received a request to reset your Maple Bank password.\n\n" +
                "Click the link below to choose a new password. This link expires in 30 minutes:\n\n" +
                resetLink + "\n\n" +
                "If you didn't request this, you can safely ignore this email - your password won't change."
        );
        mailSender.send(message);
    }
}
