package com.mental_health_app.mental_health.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * Represents a Therapist user in the system.
 *
 * A therapist can register/login and:
 *  - View their assigned patients
 *  - Manage appointment schedules
 *  - View patient assessments
 */
@Entity
@Table(name = "therapists")
public class Therapist {

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

    // Therapist-specific fields

    // e.g. "Clinical Psychology", "Cognitive Behavioural Therapy"
    private String specialization;

    // e.g. "LMHC-2024-XYZ" - license number issued by medical board
    private String licenseNumber;

    // Short professional bio shown to patients
    private String bio;

    // How many years they have been practicing
    private Integer yearsOfExperience;

    // --- Role ---

    @Enumerated(EnumType.STRING)  // stores "THERAPIST" as text in DB
    @Column(nullable = false)
    private Role role = Role.THERAPIST;

    // --- Account Status ---

    @Column(nullable = false)
    private boolean enabled = true;  // admin can disable unverified therapists

    // --- Timestamps ---

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // -------------------------
    // Constructors
    // -------------------------

    public Therapist() {
        // required by JPA
    }

    public Therapist(Long id, String name, String email, String password,
                     String phoneNumber, String specialization, String licenseNumber,
                     String bio, Integer yearsOfExperience, Role role,
                     boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.specialization = specialization;
        this.licenseNumber = licenseNumber;
        this.bio = bio;
        this.yearsOfExperience = yearsOfExperience;
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

    public String getSpecialization() { return specialization; }

    public String getLicenseNumber() { return licenseNumber; }

    public String getBio() { return bio; }

    public Integer getYearsOfExperience() { return yearsOfExperience; }

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

    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public void setBio(String bio) { this.bio = bio; }

    public void setYearsOfExperience(Integer yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }

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
        return "Therapist{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", specialization='" + specialization + '\'' +
                ", role=" + role +
                ", enabled=" + enabled +
                ", createdAt=" + createdAt +
                '}';
    }
}
