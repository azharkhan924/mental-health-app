package com.mental_health_app.mental_health.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a completed mental health self-assessment taken by a Patient.
 */
@Entity
@Table(name = "assessments")
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The patient who took this assessment
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // Type: PHQ-9 (Depression) or GAD-7 (Anxiety)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssessmentType type;

    // Calculated total score
    @Column(nullable = false)
    private int totalScore;

    // Clinical severity category (e.g., "Mild", "Moderate", "Severe")
    @Column(nullable = false)
    private String severity;

    // Tailored suggestions based on score
    @Column(length = 1000)
    private String recommendation;

    // Date & time the assessment was submitted
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public Assessment() {
    }

    public Assessment(Patient patient, AssessmentType type, int totalScore, 
                      String severity, String recommendation) {
        this.patient = patient;
        this.type = type;
        this.totalScore = totalScore;
        this.severity = severity;
        this.recommendation = recommendation;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public AssessmentType getType() {
        return type;
    }

    public void setType(AssessmentType type) {
        this.type = type;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
