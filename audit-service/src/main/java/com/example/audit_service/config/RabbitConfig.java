package com.example.audit_service.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String AUDIT_QUEUE = "audit-queue";
    public static final String AUDIT_EXCHANGE = "audit-exchange";
    public static final String AUDIT_ROUTING_KEY = "audit-routing-key";

    @Bean
    public Queue auditQueue() {
        return new Queue(AUDIT_QUEUE, false);
    }

    @Bean
    public TopicExchange auditExchange() {
        return new TopicExchange(AUDIT_EXCHANGE);
    }

    @Bean
    public Binding auditBinding(Queue auditQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(auditQueue).to(auditExchange).with(AUDIT_ROUTING_KEY);
    }
}