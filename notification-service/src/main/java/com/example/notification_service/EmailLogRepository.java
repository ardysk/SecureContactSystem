package com.example.notification_service.repository;

import com.example.notification_service.model.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findBySender(String sender);
    List<EmailLog> findByRecipient(String recipient);

    long countByRecipientAndIsReadFalse(String recipient);
}