package com.mental_health_app.mental_health.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Stores clinical notes, prescriptions, and observations recorded by the therapist
 * during or after a patient appointment. AI generates a structured clinical summary
 * from the therapist's raw notes.
 */
@Entity
@Table(name = "appointment_notes")
public class AppointmentNotes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "therapist_id", nullable = false)
    private Therapist therapist;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // Primary reason for visit
    @Column(length = 500)
    private String chiefComplaint;

    // Therapist's clinical observations during the session
    @Column(columnDefinition = "TEXT")
    private String clinicalObservations;

    // Medications, therapy, or interventions prescribed
    @Column(columnDefinition = "TEXT")
    private String prescription;

    // Follow-up plan and next steps
    @Column(columnDefinition = "TEXT")
    private String followUpPlan;

    // AI-generated structured clinical summary from the raw notes
    @Column(columnDefinition = "TEXT")
    private String aiClinicalSummary;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public AppointmentNotes() {
    }

    public AppointmentNotes(Appointment appointment, Therapist therapist, Patient patient) {
        this.appointment = appointment;
        this.therapist = therapist;
        this.patient = patient;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }

    public Therapist getTherapist() { return therapist; }
    public void setTherapist(Therapist therapist) { this.therapist = therapist; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public String getChiefComplaint() { return chiefComplaint; }
    public void setChiefComplaint(String chiefComplaint) { this.chiefComplaint = chiefComplaint; }

    public String getClinicalObservations() { return clinicalObservations; }
    public void setClinicalObservations(String clinicalObservations) { this.clinicalObservations = clinicalObservations; }

    public String getPrescription() { return prescription; }
    public void setPrescription(String prescription) { this.prescription = prescription; }

    public String getFollowUpPlan() { return followUpPlan; }
    public void setFollowUpPlan(String followUpPlan) { this.followUpPlan = followUpPlan; }

    public String getAiClinicalSummary() { return aiClinicalSummary; }
    public void setAiClinicalSummary(String aiClinicalSummary) { this.aiClinicalSummary = aiClinicalSummary; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
