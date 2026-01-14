package com.example.contact_service.controller;

import com.example.contact_service.model.Contact;
import com.example.contact_service.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService service;
    private final RabbitTemplate rabbitTemplate;

    // --- TYLKO LOGIKA KONTAKTÓW ---

    @GetMapping
    public List<Contact> getAllContacts() {
        return service.getAllContacts();
    }

    @PostMapping
    public ResponseEntity<?> createContact(@Valid @RequestBody Contact contact) {
        // Tu w przyszłości dodamy walidację tokena z Auth Service (lub zrobi to Gateway)
        return ResponseEntity.ok(service.createContact(contact));
    }

    // Endpoint do wysyłania maili (przekazuje do Notification Service)
    @PostMapping("/send-email")
    public ResponseEntity<?> sendEmail(@RequestBody Map<String, String> payload) {
        String to = payload.get("to");
        String subject = payload.get("subject");
        String body = payload.get("body");

        // "EMAIL_REQ|DO|TEMAT|TRESC"
        String queueMsg = "EMAIL_REQ|" + to + "|" + subject + "|" + body;
        rabbitTemplate.convertAndSend("contact-created-queue", queueMsg);

        // Logujemy to zdarzenie do Audit Service!
        rabbitTemplate.convertAndSend("audit-queue", "EMAIL_SENT|System|" + to);

        return ResponseEntity.ok("Zlecono wysyłkę maila.");
    }
}