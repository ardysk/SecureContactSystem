package com.example.contact_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // Tworzymy kolejkę o nazwie "contact-created-queue"
    @Bean
    public Queue contactQueue() {
        return new Queue("contact-created-queue", false);
    }
}