package com.unibank.bankingSystem.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

// EmailService intentionally lets send failures propagate rather than swallowing them,
// now that it only ever runs inside PasswordResetEmailListener - RabbitMQ's listener
// retry is what's responsible for resilience here, and it only kicks in if the
// exception actually reaches it.
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendPasswordResetEmail_sendsAMessageAddressedToTheGivenEmail() {
        emailService.sendPasswordResetEmail("user@example.com", "http://localhost:5173/reset-password?token=abc123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("user@example.com");
        assertThat(sent.getText()).contains("http://localhost:5173/reset-password?token=abc123");
    }

    @Test
    void sendPasswordResetEmail_propagatesFailure_soTheListenerCanRetry() {
        doThrow(new MailSendException("SMTP auth failed")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() ->
                emailService.sendPasswordResetEmail("user@example.com", "http://localhost:5173/reset-password?token=abc123")
        ).isInstanceOf(MailSendException.class);
    }
}
