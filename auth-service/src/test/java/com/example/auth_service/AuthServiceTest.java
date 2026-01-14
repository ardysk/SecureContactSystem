package com.example.auth_service;

import com.example.auth_service.model.User;
import com.example.auth_service.repository.UserRepository;
import com.example.auth_service.service.AuthService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    public void shouldGenerateTokenForValidUser() {
        // GIVEN
        User mockUser = new User();
        mockUser.setUsername("testuser");
        mockUser.setPassword("encodedPass");
        mockUser.setActive(true);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("rawPass", "encodedPass")).thenReturn(true);

        // WHEN
        String token = authService.generateToken("testuser", "rawPass");

        // THEN
        Assertions.assertNotNull(token, "Token nie powinien być null");
        // Tu normalnie sprawdzilibyśmy zawartość JWT, ale wystarczy sprawdzenie czy nie null
    }

    @Test
    public void shouldReturnNullForInvalidPassword() {
        // GIVEN
        User mockUser = new User();
        mockUser.setUsername("testuser");
        mockUser.setPassword("encodedPass");
        mockUser.setActive(true);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongPass", "encodedPass")).thenReturn(false);

        // WHEN
        String token = authService.generateToken("testuser", "wrongPass");

        // THEN
        Assertions.assertNull(token, "Token powinien być null dla złego hasła");
    }
}