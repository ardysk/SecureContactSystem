package com.example.audit_service.controller;

import com.example.audit_service.model.AuditLog;
import com.example.audit_service.repository.AuditRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditRepository repository;

    // --- JAWNY KONSTRUKTOR (TO NAPRAWIA BŁĄD) ---
    public AuditController(AuditRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<AuditLog> getLogs() {
        return repository.findAll();
    }
}