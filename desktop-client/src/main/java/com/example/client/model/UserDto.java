package com.example.client.model;

public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String role;
    private boolean active;
    private String firstName;
    private String lastName;

    // Gettery
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public boolean isActive() { return active; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }

    // Settery
    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role) { this.role = role; }
    public void setActive(boolean active) { this.active = active; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
}