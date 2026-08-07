package com.unibank.bankingSystem.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sets up the queue password-reset emails travel through. Publishing (AuthService)
 * and consuming (PasswordResetEmailListener) are decoupled through RabbitMQ so a
 * slow or unreachable SMTP server never blocks the forgot-password request itself.
 *
 * The queue is bound with a dead-letter exchange: once a message has been retried
 * the configured number of times (see spring.rabbitmq.listener.simple.retry.* in
 * application.properties) and still fails, RabbitMQ routes it to the dead-letter
 * queue instead of losing it or retrying it forever.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "maple-bank.exchange";
    public static final String PASSWORD_RESET_EMAIL_QUEUE = "password-reset-email-queue";
    public static final String PASSWORD_RESET_EMAIL_ROUTING_KEY = "password-reset-email";

    private static final String DEAD_LETTER_EXCHANGE = "maple-bank.dlx";
    private static final String PASSWORD_RESET_EMAIL_DLQ = "password-reset-email-queue.dlq";

    @Bean
    public DirectExchange mapleBankExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue passwordResetEmailQueue() {
        return QueueBuilder.durable(PASSWORD_RESET_EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", PASSWORD_RESET_EMAIL_DLQ)
                .build();
    }

    @Bean
    public Queue passwordResetEmailDeadLetterQueue() {
        return QueueBuilder.durable(PASSWORD_RESET_EMAIL_DLQ).build();
    }

    @Bean
    public Binding passwordResetEmailBinding() {
        return BindingBuilder.bind(passwordResetEmailQueue())
                .to(mapleBankExchange())
                .with(PASSWORD_RESET_EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding passwordResetEmailDeadLetterBinding() {
        return BindingBuilder.bind(passwordResetEmailDeadLetterQueue())
                .to(deadLetterExchange())
                .with(PASSWORD_RESET_EMAIL_DLQ);
    }

    // Spring Boot's auto-configured RabbitTemplate picks up any MessageConverter bean
    // automatically, so messages are sent/received as readable JSON instead of Java's
    // default serialized-object format.
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
