package com.example.auth_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // Definiujemy kolejkę audit.queue, na której nasłuchuje audit-service
    @Bean
    public Queue auditQueue() {
        return new Queue("audit.queue", false);
    }
}