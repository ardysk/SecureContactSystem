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

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       RabbitTemplate rabbitTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.rabbitTemplate = rabbitTemplate;
    }

    public User register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);

        try {
            String auditMsg = "USER_REGISTER|SYSTEM|Zarejestrowano nowego użytkownika: " + savedUser.getUsername();
            rabbitTemplate.convertAndSend("audit-exchange", "audit-routing-key", auditMsg);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return savedUser;
    }

    public String generateToken(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPassword()) && user.isActive()) {

                try {
                    String logMessage = "LOGIN_SUCCESS|" + username + "|Użytkownik zalogował się do systemu";
                    rabbitTemplate.convertAndSend("audit-exchange", "audit-routing-key", logMessage);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }

                return "generated-jwt-token-for-" + username;
            }
        }
        return null;
    }
}