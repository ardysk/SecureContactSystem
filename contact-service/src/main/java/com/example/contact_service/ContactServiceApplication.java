package com.example.contact_service;

import com.example.contact_service.model.User;
import com.example.contact_service.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder; // <--- Ważny import
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class ContactServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ContactServiceApplication.class, args);
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	CommandLineRunner initUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			if (userRepository.findByUsername("admin").isEmpty()) {
				User admin = new User();
				admin.setUsername("admin");
				admin.setPassword(passwordEncoder.encode("admin123"));
				admin.setRole("ADMIN");
				admin.setEmail("admin@localhost"); // <--- EMAIL ADMINA
				admin.setChangePasswordNextLogin(false);
				userRepository.save(admin);
			}

			if (userRepository.findByUsername("user").isEmpty()) {
				User user = new User();
				user.setUsername("user");
				user.setPassword(passwordEncoder.encode("user123"));
				user.setRole("USER");
				user.setEmail("user@localhost"); // <--- EMAIL USERA
				user.setChangePasswordNextLogin(true);
				userRepository.save(user);
			}
		};
	}
}