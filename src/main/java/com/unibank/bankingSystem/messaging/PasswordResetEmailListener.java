package com.unibank.bankingSystem.messaging;

import com.unibank.bankingSystem.config.RabbitMQConfig;
import com.unibank.bankingSystem.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// Consumes password-reset-email messages published by AuthService and actually
// sends them. Runs asynchronously and independently of the forgot-password HTTP
// request - if EmailService throws (SMTP down, auth failure, etc.), Spring's
// listener retry (spring.rabbitmq.listener.simple.retry.* in application.properties)
// retries this method a few times with backoff before the message is dead-lettered.
@Component
@RequiredArgsConstructor
public class PasswordResetEmailListener {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.PASSWORD_RESET_EMAIL_QUEUE)
    public void handle(PasswordResetEmailMessage message) {
        emailService.sendPasswordResetEmail(message.getEmail(), message.getResetLink());
    }
}
