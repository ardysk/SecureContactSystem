package com.example.auth_service.controller;

import com.example.auth_service.model.User;
import com.example.auth_service.repository.UserRepository;
import com.example.auth_service.service.AuthService;
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
    private final AuthService authService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuthService authService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
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

        String token = authService.generateToken(username, password);

        if (token != null) {
            return userRepository.findByUsername(username).map(u -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "OK");
                response.put("role", u.getRole());
                response.put("userId", u.getId());
                response.put("email", u.getEmail());
                response.put("token", token);
                return ResponseEntity.ok((Object) response);
            }).orElse(ResponseEntity.status(401).build());
        }

        return ResponseEntity.status(401).body(Map.of("message", "Błędne dane lub konto nieaktywne"));
    }

    @PutMapping("/profile/{username}")
    public ResponseEntity<?> updateProfile(@PathVariable String username, @RequestBody Map<String, String> data) {
        return userRepository.findByUsername(username)
                .map(u -> {
                    if (data.containsKey("firstName")) u.setFirstName(data.get("firstName"));
                    if (data.containsKey("lastName")) u.setLastName(data.get("lastName"));
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
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

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
    @PutMapping("/user/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, String> updates) {
        return userRepository.findById(id).map(user -> {
            if (updates.containsKey("firstName")) user.setFirstName(updates.get("firstName"));
            if (updates.containsKey("lastName")) user.setLastName(updates.get("lastName"));
            // Emaila nie pozwalamy edytować tutaj
            userRepository.save(user);
            return ResponseEntity.ok("Zaktualizowano dane.");
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.ok("Konto usunięte.");
        }
        return ResponseEntity.notFound().build();
    }
}