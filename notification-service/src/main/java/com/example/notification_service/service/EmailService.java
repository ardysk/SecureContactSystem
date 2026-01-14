package com.example.notification_service.service;

import com.example.notification_service.model.EmailLog;
import com.example.notification_service.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final RabbitTemplate rabbitTemplate; // Automatycznie wstrzyknięte przez Lombok
    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;

    public void sendEmail(String to, String subject, String body, String senderUsername) {
        // Rozdzielamy odbiorców po przecinku
        String[] recipients = to.split(",");

        for (String recipient : recipients) {
            String cleanRecipient = recipient.trim();
            if (cleanRecipient.isEmpty()) continue;

            // 1. Fizyczne wysłanie wiadomości (SMTP)
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(cleanRecipient);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
            } catch (Exception e) {
                System.err.println("Nie udało się wysłać do: " + cleanRecipient + ". Błąd: " + e.getMessage());
            }

            // 2. Zapisanie logu w bazie danych (notification_db)
            EmailLog log = new EmailLog();
            log.setRecipient(cleanRecipient);
            log.setSubject(subject);
            log.setBody(body);
            log.setSender(senderUsername != null ? senderUsername : "SYSTEM");
            log.setSentAt(LocalDateTime.now());
            log.setRead(false);

            emailLogRepository.save(log);
        }

        // 3. Wysłanie powiadomienia do systemu audytu (widoczne w Dashboardzie)
        // Wysyłamy po zakończeniu pętli, aby uniknąć spamu w logach przy wielu odbiorcach
        try {
            String sender = (senderUsername != null ? senderUsername : "SYSTEM");
            String auditMessage = "EMAIL_SENT|" + sender + "|Wysłano wiadomość do: " + to;

            // Używamy giełdy audit-exchange, którą wcześniej konfigurowaliśmy
            rabbitTemplate.convertAndSend("audit-exchange", "audit-routing-key", auditMessage);
            System.out.println(" [AUDIT] Wysłano log o wysyłce maila dla użytkownika: " + sender);
        } catch (Exception e) {
            System.err.println("Błąd wysyłania do RabbitMQ: " + e.getMessage());
        }
    }
}