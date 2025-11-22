package com.example.contact_service.service;

import com.example.contact_service.model.Contact;
import com.example.contact_service.repository.ContactRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository repository;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ContactService contactService;

    @Test
    void createContact_ShouldEncryptAddressAndNotify() {
        // Given
        Contact inputContact = new Contact();
        inputContact.setName("John Doe");
        inputContact.setEmail("john@test.com");
        inputContact.setAddress("Real Address");
        inputContact.setAge(30);

        // Symulujemy zachowanie repozytorium (zwraca obiekt po zapisie)
        when(repository.save(any(Contact.class))).thenReturn(inputContact);

        // When
        Contact result = contactService.createContact(inputContact);

        // Then
        // 1. Sprawdzamy, czy repozytorium zostało wywołane
        verify(repository, times(1)).save(any(Contact.class));

        // 2. Sprawdzamy, czy wysłano wiadomość do RabbitMQ
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString());

        // 3. Sprawdzamy, czy zwrócony obiekt ma czytelny adres (serwis powinien go odszyfrować dla użytkownika)
        Assertions.assertEquals("Real Address", result.getAddress());
    }
}