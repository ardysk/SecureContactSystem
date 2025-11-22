package com.example.contact_service.service;

import com.example.contact_service.model.AgifyResponse; // <--- Nowe
import com.example.contact_service.model.Contact;
import com.example.contact_service.repository.ContactRepository;
import com.example.contact_service.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate; // <--- Nowe

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate; // <--- Narzędzie do łączenia z netem

    public Contact createContact(Contact contact) {
        // --- 1. INTEGRACJA Z ZEWNĘTRZNYM API ---
        // Jeśli użytkownik nie podał wieku, zgadujemy go na podstawie imienia
        if (contact.getAge() == 0) {
            try {
                String url = "https://api.agify.io?name=" + contact.getName();
                // Strzał do zewnętrznego serwisu
                AgifyResponse response = restTemplate.getForObject(url, AgifyResponse.class);

                if (response != null) {
                    contact.setAge(response.getAge());
                    System.out.println(" [API] Pobranno wiek dla " + contact.getName() + ": " + response.getAge());
                }
            } catch (Exception e) {
                System.err.println("Błąd zewnętrznego API: " + e.getMessage());
                contact.setAge(18); // Wartość domyślna w razie błędu
            }
        }

        // --- 2. Szyfrowanie adresu ---
        String originalAddress = contact.getAddress();
        contact.setAddress(EncryptionUtil.encrypt(originalAddress));

        // --- 3. Zapis do bazy ---
        Contact savedContact = repository.save(contact);

        // --- 4. RabbitMQ (Async) ---
        String message = "Utworzono nowy kontakt: " + savedContact.getEmail() + " (Wiek: " + savedContact.getAge() + ")";
        rabbitTemplate.convertAndSend("contact-created-queue", message);

        // --- 5. Return ---
        savedContact.setAddress(originalAddress);
        return savedContact;
    }

    public List<Contact> getAllContacts() {
        List<Contact> contacts = repository.findAll();
        for (Contact c : contacts) {
            try {
                c.setAddress(EncryptionUtil.decrypt(c.getAddress()));
            } catch (Exception e) { }
        }
        return contacts;
    }
}