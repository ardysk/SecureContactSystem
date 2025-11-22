package com.example.contact_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Entity
@Table(name = "contacts")
public class Contact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Imię nie może być puste")
    private String name;

    @NotBlank(message = "Email jest wymagany")
    @Email(message = "Niepoprawny format email")
    private String email;

    private String address;
    private int age;
}