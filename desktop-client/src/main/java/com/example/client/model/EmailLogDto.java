package com.example.client.model;

public class EmailLogDto {
    private Long id;
    private String sender;
    private String recipient;
    private String subject;
    private String body;
    private String sentAt;
    private boolean read; // <--- NOWE POLE

    public EmailLogDto() {}

    public EmailLogDto(Long id, String sender, String recipient, String subject, String body, String sentAt, boolean read) {
        this.id = id;
        this.sender = sender;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.sentAt = sentAt;
        this.read = read;
    }

    // Gettery
    public Long getId() { return id; }
    public String getSender() { return sender; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public String getSentAt() { return sentAt; }
    public boolean isRead() { return read; } // <--- TO NAPRAWI BŁĄD

    // Settery
    public void setId(Long id) { this.id = id; }
    public void setSender(String sender) { this.sender = sender; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setBody(String body) { this.body = body; }
    public void setSentAt(String sentAt) { this.sentAt = sentAt; }
    public void setRead(boolean read) { this.read = read; }
}