package com.mental_health_app.mental_health.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Tracks a custom test assigned by a therapist to a specific patient.
 * Records completion status, total score, answers, and AI-generated clinical evaluation.
 */
@Entity
@Table(name = "custom_test_assignments")
public class CustomTestAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "custom_test_id", nullable = false)
    private CustomTest customTest;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_by_therapist_id", nullable = false)
    private Therapist assignedBy;

    // Status: "PENDING", "COMPLETED"
    @Column(nullable = false)
    private String status = "PENDING";

    @Column(updatable = false)
    private LocalDateTime assignedAt;

    private LocalDateTime completedAt;

    private Integer totalScore;

    private String severity; // Low, Mild, Moderate, High, Severe

    @Column(columnDefinition = "TEXT")
    private String patientAnswersJson;

    @Column(columnDefinition = "TEXT")
    private String aiEvaluation;

    public CustomTestAssignment() {
    }

    public CustomTestAssignment(CustomTest customTest, Patient patient, Therapist assignedBy) {
        this.customTest = customTest;
        this.patient = patient;
        this.assignedBy = assignedBy;
        this.status = "PENDING";
    }

    @PrePersist
    protected void onCreate() {
        this.assignedAt = LocalDateTime.now();
    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CustomTest getCustomTest() {
        return customTest;
    }

    public void setCustomTest(CustomTest customTest) {
        this.customTest = customTest;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Therapist getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(Therapist assignedBy) {
        this.assignedBy = assignedBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getPatientAnswersJson() {
        return patientAnswersJson;
    }

    public void setPatientAnswersJson(String patientAnswersJson) {
        this.patientAnswersJson = patientAnswersJson;
    }

    public String getAiEvaluation() {
        return aiEvaluation;
    }

    public void setAiEvaluation(String aiEvaluation) {
        this.aiEvaluation = aiEvaluation;
    }
}
