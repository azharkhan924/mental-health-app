package com.mental_health_app.mental_health.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * Represents a Patient user in the system.
 *
 * A patient can register/login and use the app to:
 *  - Chat with AI
 *  - Book appointments with therapists
 *  - Fill mental health assessments
 */
@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Personal Info ---

    @NotBlank(message = "Name is required")
    @Column(nullable = false)
    private String name;

    @Email(message = "Please provide a valid email")
    @NotBlank(message = "Email is required")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String password;  // stored as bcrypt hash, never plain text

    private String phoneNumber;

    private String bio;

    // --- Role ---

    @Enumerated(EnumType.STRING)  // stores "PATIENT" as text in DB
    @Column(nullable = false)
    private Role role = Role.PATIENT;

    // --- Account Status ---

    @Column(nullable = false)
    private boolean enabled = true;

    // --- Timestamps ---

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // -------------------------
    // Constructors
    // -------------------------

    public Patient() {
        // required by JPA
    }

    public Patient(Long id, String name, String email, String password,
                   String phoneNumber, String bio, Role role,
                   boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.bio = bio;
        this.role = role;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // -------------------------
    // Getters
    // -------------------------

    public Long getId() { return id; }

    public String getName() { return name; }

    public String getEmail() { return email; }

    public String getPassword() { return password; }

    public String getPhoneNumber() { return phoneNumber; }

    public String getBio() { return bio; }

    public Role getRole() { return role; }

    public boolean isEnabled() { return enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // -------------------------
    // Setters
    // -------------------------

    public void setId(Long id) { this.id = id; }

    public void setName(String name) { this.name = name; }

    public void setEmail(String email) { this.email = email; }

    public void setPassword(String password) { this.password = password; }

    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public void setBio(String bio) { this.bio = bio; }

    public void setRole(Role role) { this.role = role; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // -------------------------
    // Lifecycle hooks
    // -------------------------

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // -------------------------
    // toString (useful for debugging)
    // -------------------------

    @Override
    public String toString() {
        return "Patient{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", enabled=" + enabled +
                ", createdAt=" + createdAt +
                '}';
    }
}
