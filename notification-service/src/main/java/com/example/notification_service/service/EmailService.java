package com.example.notification_service.service;

import com.example.notification_service.model.EmailLog;
import com.example.notification_service.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository; // Wstrzykujemy repozytorium

    public void sendEmail(String to, String subject, String body, String senderUsername) {
        // Rozdzielamy odbiorców po przecinku (np. "jan@a.pl, adam@b.pl")
        String[] recipients = to.split(",");

        for (String recipient : recipients) {
            String cleanRecipient = recipient.trim();
            if (cleanRecipient.isEmpty()) continue;

            // 1. Fizyczne wysłanie (Mailtrap/GreenMail)
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(cleanRecipient);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
            } catch (Exception e) {
                System.err.println("Nie udało się wysłać do: " + cleanRecipient);
            }

            // 2. Zapisanie osobnego logu dla każdego odbiorcy (dzięki temu każdy ma swoją flagę isRead)
            EmailLog log = new EmailLog();
            log.setRecipient(cleanRecipient);
            log.setSubject(subject);
            log.setBody(body);
            log.setSender(senderUsername != null ? senderUsername : "SYSTEM");
            log.setSentAt(LocalDateTime.now());
            log.setRead(false);

            emailLogRepository.save(log);
        }
    }
}