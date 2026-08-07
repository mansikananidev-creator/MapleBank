package com.unibank.bankingSystem.messaging;

import com.unibank.bankingSystem.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PasswordResetEmailListenerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordResetEmailListener listener;

    @Test
    void handle_sendsTheEmailDescribedByTheMessage() {
        PasswordResetEmailMessage message = new PasswordResetEmailMessage(
                "user@example.com", "http://localhost:5173/reset-password?token=abc123"
        );

        listener.handle(message);

        verify(emailService).sendPasswordResetEmail(
                "user@example.com", "http://localhost:5173/reset-password?token=abc123"
        );
    }
}
