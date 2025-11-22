package com.example.contact_service.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "app_users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;
    private String password; // Teraz tu będzie hash!
    private String email; // <--- NOWE POLE
    private String role;

    private boolean changePasswordNextLogin = false; // Nowe pole
}