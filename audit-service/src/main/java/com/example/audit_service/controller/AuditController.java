package com.example.audit_service.controller;

import com.example.audit_service.model.AuditLog;
import com.example.audit_service.repository.AuditRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditRepository repository;
    private final RabbitTemplate rabbitTemplate;

    public AuditController(AuditRepository repository, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @GetMapping
    public List<AuditLog> getLogs() {
        return repository.findAll();
    }

    @PostMapping("/external")
    public void logExternalEvent(@RequestBody String message) {
        rabbitTemplate.convertAndSend("audit-queue", message);
    }
}