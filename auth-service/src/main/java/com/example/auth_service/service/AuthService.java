package com.example.auth_service.service;

import com.example.auth_service.model.User;
import com.example.auth_service.repository.UserRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate; // <--- To jest niezbędne
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate; // <--- Dodajemy pole do obsługi RabbitMQ

    // RĘCZNY KONSTRUKTOR - Inicjalizuje wszystkie pola (w tym rabbitTemplate)
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
            // Sprawdzamy hasło i czy konto jest aktywne
            if (passwordEncoder.matches(password, user.getPassword()) && user.isActive()) {

                // --- TUTAJ DODAJEMY WYSYŁANIE LOGU ---
                try {
                    // UWAGA: Używamy nazwy "audit-queue" (z myślnikiem), bo taką ma AuditService
                    String logMessage = "LOGIN_SUCCESS|" + username + "|Użytkownik zalogował się do systemu";
                    rabbitTemplate.convertAndSend("audit-queue", logMessage);
                    System.out.println(" [AuthService] Wysłano log do RabbitMQ: " + logMessage);
                } catch (Exception e) {
                    System.err.println(" [AuthService] Błąd wysyłania logu: " + e.getMessage());
                }
                // -------------------------------------

                return "generated-jwt-token-for-" + username;
            }
        }
        return null;
    }
}