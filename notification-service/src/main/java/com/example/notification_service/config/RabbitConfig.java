package com.example.notification_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // To sprawi, że Notification Service sam sobie utworzy kolejkę, jeśli jej nie ma
    @Bean
    public Queue contactQueue() {
        return new Queue("contact-created-queue", false);
    }
}