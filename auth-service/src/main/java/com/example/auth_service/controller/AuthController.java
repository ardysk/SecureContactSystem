package com.example.auth_service.controller;

import com.example.auth_service.model.User;
import com.example.auth_service.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Użytkownik już istnieje!");
        }

        // Szyfrowanie hasła
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Logika aktywności: Admin jest aktywny od razu, User musi czekać
        if ("ADMIN".equals(user.getRole())) {
            user.setActive(true);
        } else {
            user.setActive(false);
        }

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Zarejestrowano. Oczekuj na akceptację administratora."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> creds) {
        String username = creds.get("username");
        String password = creds.get("password");

        return userRepository.findByUsername(username)
                .filter(u -> passwordEncoder.matches(password, u.getPassword()))
                .map(u -> {
                    // SPRAWDZENIE CZY KONTO AKTYWNE
                    if (!u.isActive()) {
                        return ResponseEntity.status(403).body((Object) Map.of("status", "PENDING", "message", "Konto nieaktywne. Skontaktuj się z administratorem."));
                    }

                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "OK");
                    response.put("role", u.getRole());
                    response.put("userId", u.getId());
                    // Dodatkowo zwracamy imię i nazwisko przy logowaniu, jeśli są ustawione
                    response.put("firstName", u.getFirstName());
                    response.put("lastName", u.getLastName());
                    response.put("email", u.getEmail());

                    return ResponseEntity.ok((Object) response);
                })
                .orElse(ResponseEntity.status(401).body(Map.of("status", "ERROR", "message", "Błędne dane")));
    }

    // --- Endpointy dla Użytkownika (Moje Konto) ---

    @PutMapping("/profile/{username}")
    public ResponseEntity<?> updateProfile(@PathVariable String username, @RequestBody Map<String, String> data) {
        return userRepository.findByUsername(username)
                .map(u -> {
                    if (data.containsKey("firstName")) u.setFirstName(data.get("firstName"));
                    if (data.containsKey("lastName")) u.setLastName(data.get("lastName"));
                    // Opcjonalnie zmiana hasła itp.
                    userRepository.save(u);
                    return ResponseEntity.ok("Profil zaktualizowany");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/profile/{username}")
    public ResponseEntity<?> getProfile(@PathVariable String username) {
        return userRepository.findByUsername(username)
                .map(u -> ResponseEntity.ok(u))
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Endpointy dla Admina (Zarządzanie) ---

    // 1. Pobieranie WSZYSTKICH użytkowników (dla panelu Admina i widoku Kontaktów)
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    // 2. Edycja dowolnego użytkownika przez Admina (CRUD)
    @PutMapping("/admin/users/{id}")
    public ResponseEntity<?> updateUserByAdmin(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        return userRepository.findById(id)
                .map(user -> {
                    if (updates.containsKey("firstName")) user.setFirstName((String) updates.get("firstName"));
                    if (updates.containsKey("lastName")) user.setLastName((String) updates.get("lastName"));
                    if (updates.containsKey("email")) user.setEmail((String) updates.get("email"));
                    if (updates.containsKey("role")) user.setRole((String) updates.get("role"));
                    if (updates.containsKey("active")) user.setActive((Boolean) updates.get("active"));
                    if (updates.containsKey("age")) user.setAge(Integer.parseInt(updates.get("age").toString()));
                    userRepository.save(user);
                    return ResponseEntity.ok("Użytkownik zaktualizowany przez Admina.");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pending")
    public List<User> getPendingUsers() {
        return userRepository.findAll().stream().filter(u -> !u.isActive()).toList();
    }

    @PostMapping("/activate/{username}")
    public ResponseEntity<?> activateUser(@PathVariable String username) {
        return userRepository.findByUsername(username)
                .map(u -> {
                    u.setActive(true);
                    userRepository.save(u);
                    return ResponseEntity.ok("Użytkownik " + username + " aktywowany.");
                })
                .orElse(ResponseEntity.notFound().build());
    }
}