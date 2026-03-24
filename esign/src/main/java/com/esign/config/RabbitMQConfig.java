package com.esign.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EMAIL_QUEUE    = "email.queue";
    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String EMAIL_KEY      = "email.routing.key";

    @Bean
    public Queue emailQueue() {
        // durable = true → queue tidak hilang saat RabbitMQ restart
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", "email.dlx") // gagal → DLQ
                .build();
    }

    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(EMAIL_EXCHANGE);
    }

    @Bean
    public Binding emailBinding() {
        return BindingBuilder.bind(emailQueue())
                .to(emailExchange())
                .with(EMAIL_KEY);
    }

    // Dead Letter Queue — tampung email yang gagal dikirim
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("email.dlq").build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("email.dlx");
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(EMAIL_QUEUE);
    }
}
