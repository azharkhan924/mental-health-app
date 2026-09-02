package com.mental_health_app.mental_health.dto;

import java.time.LocalDateTime;

/**
 * Simple POJO to represent a single chat message.
 * Stored in the user's HTTP session (not database).
 * 
 * role = "user"      → message sent by the patient
 * role = "assistant"  → response from the AI
 */
public class ChatMessage {

    private String role;       // "user" or "assistant"
    private String content;    // the actual message text
    private LocalDateTime timestamp;

    public ChatMessage() {
    }

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    // --- Getters & Setters ---

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
