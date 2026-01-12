package com.example.auth_service.service;

import com.example.auth_service.model.User;
import com.example.auth_service.repository.UserRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;

    // --- RĘCZNY KONSTRUKTOR ---
    // Jawnie inicjalizujemy wszystkie pola finalne.
    // To naprawi błąd "not initialized in the default constructor".
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       RabbitTemplate rabbitTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.rabbitTemplate = rabbitTemplate;
    }

    public String generateToken(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPassword()) && user.isActive()) {

                // Logowanie do RabbitMQ (Audit) - Używamy poprawnej nazwy "audit-queue"
                try {
                    String logMessage = "LOGIN_SUCCESS|" + username + "|Użytkownik zalogował się do systemu";
                    rabbitTemplate.convertAndSend("audit-queue", logMessage);
                } catch (Exception e) {
                    System.err.println("Błąd wysyłania logu audit: " + e.getMessage());
                }

                return "generated-jwt-token-for-" + username;
            }
        }
        return null;
    }
}