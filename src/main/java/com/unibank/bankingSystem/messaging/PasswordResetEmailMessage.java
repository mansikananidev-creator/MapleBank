package com.unibank.bankingSystem.messaging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

// The payload published to the password-reset-email queue. Needs a no-arg
// constructor + getters/setters for Jackson (via Jackson2JsonMessageConverter, see
// RabbitMQConfig) to serialize/deserialize it.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetEmailMessage implements Serializable {
    private String email;
    private String resetLink;
}
