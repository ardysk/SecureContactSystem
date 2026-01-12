package com.example.client.session;

public class UserSession {
    private static UserSession instance;

    private String username;
    private String role;
    private Long id;
    private String email;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) instance = new UserSession();
        return instance;
    }


    public void cleanUserSession() {
        username = null;
        role = null;
        id = null;
        email = null;
    }

    // Gettery i Settery
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}