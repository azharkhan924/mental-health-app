package com.mental_health_app.mental_health.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents an AI Pre-Appointment Diagnostic Task assigned by a therapist to a patient.
 * The AI conducts a guided conversation following the therapist's instructions to collect
 * diagnostic information before the consultation. The full conversation and AI-generated
 * summary are stored for therapist review.
 */
@Entity
@Table(name = "pre_appointment_tasks")
public class PreAppointmentTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "therapist_id", nullable = false)
    private Therapist therapist;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(nullable = false)
    private String title;

    // Therapist's custom instructions for the AI (e.g., "Ask about sleep, anxiety triggers, mood")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String instructions;

    // Status: PENDING, IN_PROGRESS, COMPLETED
    @Column(nullable = false)
    private String status = "PENDING";

    // Full AI conversation stored as JSON array: [{"role":"user","content":"..."},{"role":"assistant","content":"..."}]
    @Column(columnDefinition = "TEXT")
    private String conversationJson;

    // AI-generated diagnostic summary for therapist review
    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    public PreAppointmentTask() {
    }

    public PreAppointmentTask(Therapist therapist, Patient patient, String title, String instructions) {
        this.therapist = therapist;
        this.patient = patient;
        this.title = title;
        this.instructions = instructions;
        this.status = "PENDING";
        this.conversationJson = "[]";
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.conversationJson == null) {
            this.conversationJson = "[]";
        }
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Therapist getTherapist() { return therapist; }
    public void setTherapist(Therapist therapist) { this.therapist = therapist; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getConversationJson() { return conversationJson; }
    public void setConversationJson(String conversationJson) { this.conversationJson = conversationJson; }

    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
