package com.example.client.model;

import lombok.Data;

@Data
public class ContactDto {
    private Long id;
    private String name;
    private String email;
    private String address;
    private int age;

    // Dodajemy gettery ręcznie, na wypadek gdyby Lombok zaszwankował przy kompilacji
    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getAddress() { return address; }
}