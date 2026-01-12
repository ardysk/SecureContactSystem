package com.example.client.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuditLogDto {
    private String id;
    private String eventType;
    private String message;
    private String username;
    private LocalDateTime timestamp;
}