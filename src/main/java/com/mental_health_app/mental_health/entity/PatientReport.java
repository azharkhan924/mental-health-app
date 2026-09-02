package com.mental_health_app.mental_health.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents an AI-generated analysis or clinical summary for a Patient.
 *
 * Types:
 * 1. ASSESSMENT: Generated after taking PHQ-9, GAD-7, etc. Visible to patient and therapist.
 * 2. CHAT_BEHAVIORAL: Generated from patient's interactions with AI companions.
 *    Hidden from the patient (visibleToPatient = false), visible to authorized therapists.
 */
@Entity
@Table(name = "patient_reports")
public class PatientReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The patient this report evaluates
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType reportType;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String summary;

    // Detailed AI clinical & behavioral analysis
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // Severity rating: LOW, MILD, MODERATE, HIGH, CRITICAL
    @Column(nullable = false)
    private String severity;

    // Patient visibility control
    @Column(nullable = false)
    private boolean visibleToPatient = true;

    // Optional link to specific Assessment if this report was generated from a test
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id")
    private Assessment assessment;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    public PatientReport() {
    }

    public PatientReport(Patient patient, ReportType reportType, String title, String summary,
                         String content, String severity, boolean visibleToPatient, Assessment assessment) {
        this.patient = patient;
        this.reportType = reportType;
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.severity = severity;
        this.visibleToPatient = visibleToPatient;
        this.assessment = assessment;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --- Getters and Setters ---

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

    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public boolean isVisibleToPatient() {
        return visibleToPatient;
    }

    public void setVisibleToPatient(boolean visibleToPatient) {
        this.visibleToPatient = visibleToPatient;
    }

    public Assessment getAssessment() {
        return assessment;
    }

    public void setAssessment(Assessment assessment) {
        this.assessment = assessment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
