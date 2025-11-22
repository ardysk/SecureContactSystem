package com.example.notification_service;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor // To nam wstrzyknie JavaMailSender automatycznie
public class NotificationListener {

    private final JavaMailSender mailSender;

    @RabbitListener(queues = "contact-created-queue")
    public void handleMessage(String message) {
        System.out.println(" [Notification Service] Otrzymano: " + message);

        if (message.startsWith("EMAIL_REQ|")) {
            // To jest żądanie wysłania konkretnego maila od użytkownika
            String[] parts = message.split("\\|", 4); // Dzielimy na 4 części
            if (parts.length == 4) {
                sendEmail(parts[1], parts[2], parts[3]); // to, subject, body
            }
        } else {
            // To jest standardowe powiadomienie o nowym kontakcie
            String emailAddress = "admin@system.pl";
            // Prosta logika wyciągania maila z komunikatu "Utworzono nowy kontakt: x@y.pl..."
            if(message.contains(": ")) {
                String temp = message.split(": ")[1];
                if(temp.contains(" ")) emailAddress = temp.split(" ")[0];
                else emailAddress = temp;
            }
            sendEmail(emailAddress, "Nowy Kontakt w Systemie", "System zarejestrował zdarzenie:\n" + message);
        }
    }

    private void sendEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(to);
            email.setSubject(subject);
            email.setText(content);
            email.setFrom("system@secure-app.com");
            mailSender.send(email);
            System.out.println(" [SMTP] Wysłano maila do: " + to);
        } catch (Exception e) {
            System.err.println("Błąd wysyłania maila: " + e.getMessage());
        }
    }

    private void sendEmail(String to, String content) {
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(to);
            email.setSubject("Nowy Kontakt w Systemie!");
            email.setText("Witaj!\n\nSystem zarejestrował zdarzenie:\n" + content + "\n\nPozdrawiamy, Zespół.");
            email.setFrom("noreply@contactsystem.com");

            mailSender.send(email);
            System.out.println(" [SMTP] Email wysłany do: " + to);
        } catch (Exception e) {
            System.err.println("Błąd wysyłania maila: " + e.getMessage());
        }
    }
}