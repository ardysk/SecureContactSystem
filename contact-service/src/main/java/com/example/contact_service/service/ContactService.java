package com.example.contact_service.service;

import com.example.contact_service.model.AgifyResponse;
import com.example.contact_service.model.Contact;
import com.example.contact_service.repository.ContactRepository;
import com.example.contact_service.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate;

    public Contact createContact(Contact contact) {
        // 1. Zewnętrzne API (wiek)
        if (contact.getAge() == 0) {
            try {
                String url = "https://api.agify.io?name=" + contact.getName();
                AgifyResponse response = restTemplate.getForObject(url, AgifyResponse.class);
                if (response != null) {
                    contact.setAge(response.getAge());
                }
            } catch (Exception e) {
                contact.setAge(18);
            }
        }

        // 2. Szyfrowanie
        String originalAddress = contact.getAddress();
        contact.setAddress(EncryptionUtil.encrypt(originalAddress));

        // 3. Zapis
        Contact savedContact = repository.save(contact);

        // 4. RabbitMQ -> Notification Service (Mail)
        String message = "Utworzono nowy kontakt: " + savedContact.getEmail() + " (Wiek: " + savedContact.getAge() + ")";
        rabbitTemplate.convertAndSend("contact-created-queue", message);

        // 5. RabbitMQ -> Audit Service (NOWOŚĆ: Logowanie zdarzenia)
        // Format: TYP|USER|TREŚĆ
        String auditMsg = "ADD_CONTACT|System|" + savedContact.getName();
        rabbitTemplate.convertAndSend("audit-queue", auditMsg);

        // 6. Return
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

        // Logujemy pobranie listy (opcjonalne, może generować duży ruch)
        // rabbitTemplate.convertAndSend("audit-queue", "GET_CONTACTS|System|Pobrano listę");

        return contacts;
    }
}