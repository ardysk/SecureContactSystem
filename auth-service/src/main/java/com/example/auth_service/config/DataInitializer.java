package com.example.auth_service.config;

import com.example.auth_service.model.User;
import com.example.auth_service.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Sprawdzamy, czy admin już istnieje, żeby nie nadpisywać go przy każdym restarcie (jeśli nie czyścimy voluminów)
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123")); // Tutaj bezpiecznie haszujemy hasło
            admin.setRole("ADMIN");
            admin.setEmail("admin@secure-system.com");
            admin.setActive(true); // Admin jest od razu aktywny

            // Dodajemy nowe pola, które wprowadziliśmy wcześniej
            admin.setFirstName("Super");
            admin.setLastName("Admin");
            admin.setAddress("Serwerownia 1");
            admin.setAge(99);

            userRepository.save(admin);
            System.out.println(">>> AUTOMATYCZNA INICJALIZACJA: Konto 'admin' zostało utworzone.");
        } else {
            System.out.println(">>> AUTOMATYCZNA INICJALIZACJA: Konto 'admin' już istnieje.");
        }
    }
}