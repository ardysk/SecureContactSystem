package com.example.notification_service.controller;

import com.example.notification_service.model.EmailLog;
import com.example.notification_service.repository.EmailLogRepository;
import com.example.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;
    private final EmailLogRepository emailLogRepository;

    @PostMapping("/send")
    public String sendEmail(@RequestBody Map<String, String> request) {
        emailService.sendEmail(request.get("to"), request.get("subject"), request.get("body"), request.get("sender"));
        return "Email wysłany.";
    }

    @GetMapping("/inbox")
    public List<EmailLog> getInbox(@RequestParam String email) {
        return emailLogRepository.findByRecipient(email);
    }

    @GetMapping("/history/{username}")
    public List<EmailLog> getUserHistory(@PathVariable String username) {
        return emailLogRepository.findBySender(username);
    }

    // --- NOWE ENDPOINTY ---

    // 1. Pobierz liczbę nieprzeczytanych
    @GetMapping("/unread-count")
    public long getUnreadCount(@RequestParam String email) {
        return emailLogRepository.countByRecipientAndIsReadFalse(email);
    }

    @GetMapping("/all")
    public List<EmailLog> getAllEmailsGlobal() {
        // Sortujemy od najnowszych
        return emailLogRepository.findAll(Sort.by(Sort.Direction.DESC, "sentAt"));
    }

    // 2. Oznacz maila jako przeczytanego
    @PutMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        emailLogRepository.findById(id).ifPresent(email -> {
            email.setRead(true);
            emailLogRepository.save(email);
        });
    }

    // 3. Usuń maila (Dla Admina)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEmail(@PathVariable Long id) {
        if (emailLogRepository.existsById(id)) {
            emailLogRepository.deleteById(id);
            return ResponseEntity.ok("Usunięto.");
        }
        return ResponseEntity.notFound().build();
    }
}