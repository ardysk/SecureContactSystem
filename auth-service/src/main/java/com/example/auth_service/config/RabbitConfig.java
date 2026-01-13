package com.example.auth_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public Queue auditQueue() {
        return new Queue("audit-queue", false); // Zmień audit.queue na audit-queue
    }
}