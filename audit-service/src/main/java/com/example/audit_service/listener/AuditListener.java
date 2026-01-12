package com.example.audit_service.listener;

import com.example.audit_service.model.AuditLog;
import com.example.audit_service.repository.AuditRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AuditListener {

    private final AuditRepository repository;

    public AuditListener(AuditRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = "audit-queue")
    public void handleAuditMessage(String message) {
        System.out.println(" [AUDIT] Odebrano log: " + message);

        AuditLog log = new AuditLog();

        if (message.contains("|")) {
            String[] parts = message.split("\\|", 3);
            if (parts.length >= 3) {
                log.setEventType(parts[0]);
                log.setUsername(parts[1]);
                log.setMessage(parts[2]);
            } else {
                log.setEventType("UNKNOWN");
                log.setMessage(message);
            }
        } else {
            log.setEventType("SYSTEM_EVENT");
            log.setMessage(message);
        }

        repository.save(log);
        System.out.println(" [MONGO] Zapisano w bazie.");
    }
}