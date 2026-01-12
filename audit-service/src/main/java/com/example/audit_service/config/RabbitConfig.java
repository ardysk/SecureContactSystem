package com.example.audit_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // Nazwa kolejki, na którą inne serwisy będą wysyłać logi
    @Bean
    public Queue auditQueue() {
        return new Queue("audit-queue", false);
    }
}