package com.example.audit_service.repository;

import com.example.audit_service.model.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditRepository extends MongoRepository<AuditLog, String> {
    // MongoRepository daje nam metody save(), findAll() za darmo
}