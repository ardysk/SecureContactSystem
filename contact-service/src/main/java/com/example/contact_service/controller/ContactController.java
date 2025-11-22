package com.example.contact_service.controller;

import com.example.contact_service.model.Contact;
import com.example.contact_service.service.ContactService;
import com.example.contact_service.model.User;
import com.example.contact_service.repository.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService service;
    private final RabbitTemplate rabbitTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Wstrzykujemy enkoder

    // --- ENDPOINTY LOGOWANIA I REJESTRACJI ---

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Użytkownik istnieje!");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        user.setEmail(user.getUsername() + "@localhost");
        user.setChangePasswordNextLogin(false);
        userRepository.save(user);
        return ResponseEntity.ok("Zarejestrowano. Twój email to: " + user.getEmail());
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> creds) {
        String username = creds.get("username");
        String password = creds.get("password");

        return userRepository.findByUsername(username)
                .filter(u -> passwordEncoder.matches(password, u.getPassword())) // Sprawdzamy hash
                .map(u -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "OK");
                    response.put("role", u.getRole());
                    response.put("forceChange", u.isChangePasswordNextLogin());
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.status(401).body(Map.of("status", "ERROR", "message", "Błędne dane")));
    }

    @PostMapping("/auth/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> data) {
        String username = data.get("username");
        String newPass = data.get("newPassword");

        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPass)); // Zapisz nowy hash
            user.setChangePasswordNextLogin(false); // Zdejmij flagę
            userRepository.save(user);
            return ResponseEntity.ok("Hasło zmienione.");
        }
        return ResponseEntity.badRequest().body("Błąd użytkownika.");
    }

    @GetMapping
    public List<Contact> getAllContacts() {
        return service.getAllContacts();
    }

    // @Valid uruchamia sprawdzanie adnotacji z modelu Contact
    @PostMapping
    public ResponseEntity<?> createContact(@Valid @RequestBody Contact contact) {
        return ResponseEntity.ok(service.createContact(contact));
    }

    // Nowy endpoint: Wyślij email do użytkownika
    @PostMapping("/send-email")
    public ResponseEntity<?> sendEmail(@RequestBody Map<String, String> payload) {
        String to = payload.get("to");
        String subject = payload.get("subject");
        String body = payload.get("body");

        // Formatujemy wiadomość jako JSON-String, żeby notification-service zrozumiał
        // Używamy specjalnego formatu: "EMAIL_REQ|DO|TEMAT|TRESC" dla uproszczenia parsowania
        String queueMsg = "EMAIL_REQ|" + to + "|" + subject + "|" + body;

        rabbitTemplate.convertAndSend("contact-created-queue", queueMsg);

        return ResponseEntity.ok("Zlecono wysyłkę maila.");
    }
    // 1. Pobierz listę wszystkich użytkowników (Dla Admina)
    @GetMapping("/auth/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    // 2. Usuń użytkownika (Dla Admina)
    @DeleteMapping("/auth/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.ok("Użytkownik usunięty.");
        }
        return ResponseEntity.status(404).body("Nie znaleziono użytkownika.");
    }

    // 3. Reset hasła przez Admina (wymusza zmianę przy logowaniu)
    @PostMapping("/auth/admin-reset-password")
    public ResponseEntity<?> adminResetPassword(@RequestBody Map<String, String> data) {
        Long userId = Long.valueOf(data.get("id"));
        String tempPass = data.get("tempPassword");

        var userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            u.setPassword(passwordEncoder.encode(tempPass));
            u.setChangePasswordNextLogin(true); // Wymuś zmianę!
            userRepository.save(u);
            return ResponseEntity.ok("Hasło zresetowane.");
        }
        return ResponseEntity.status(404).body("Błąd użytkownika.");
    }
}