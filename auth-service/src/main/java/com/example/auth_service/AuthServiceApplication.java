package com.example.auth_service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner versionCheck() {
        return args -> {
            System.out.println("=======================================================");
            System.out.println("   >>> AUTH SERVICE ZALADOWANY POPRAWNIE (VER 2.0) <<<");
            System.out.println("   >>> Sprawdzam polaczenie RabbitMQ...            <<<");
            System.out.println("=======================================================");
        };
    }
}