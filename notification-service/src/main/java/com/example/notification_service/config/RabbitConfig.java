package com.example.notification_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public Queue contactQueue() {
        return new Queue("contact-created-queue", false);
    }

    // Dodaj to, aby naprawić błąd 'no exchange audit-exchange'
    @Bean
    public TopicExchange auditExchange() {
        return new TopicExchange("audit-exchange");
    }
}