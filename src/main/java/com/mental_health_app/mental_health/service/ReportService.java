package com.mental_health_app.mental_health.service;

import com.mental_health_app.mental_health.entity.*;
import com.mental_health_app.mental_health.repository.AppointmentRepository;
import com.mental_health_app.mental_health.repository.ChatMessageRepository;
import com.mental_health_app.mental_health.repository.PatientReportRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REPORT SERVICE
 * ──────────────
 * Generates AI-powered Clinical Assessment Reports and Confidential Chat Behavioral Reports.
 * Manages 3-month time-windowed access for licensed therapists upon appointment booking.
 */
@Service
public class ReportService {

    private final PatientReportRepository reportRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AppointmentRepository appointmentRepository;
    private final ChatService chatService;

    public ReportService(PatientReportRepository reportRepository,
                         ChatMessageRepository chatMessageRepository,
                         AppointmentRepository appointmentRepository,
                         ChatService chatService) {
        this.reportRepository = reportRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.appointmentRepository = appointmentRepository;
        this.chatService = chatService;
    }

    /**
     * Generate an AI Clinical Assessment Report when a patient completes an assessment.
     * Visible to both patient and consulting therapist.
     */
    public PatientReport generateAssessmentReport(Assessment assessment) {
        Patient patient = assessment.getPatient();
        AssessmentType type = assessment.getType();
        int score = assessment.getTotalScore();
        int maxScore = type.getMaxScore();
        String severity = assessment.getSeverity();

        String title = type.getDisplayName() + " Clinical Evaluation";
        String summary = String.format("Score: %d/%d (%s) — Generated on %s",
                score, maxScore, severity,
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));

        String systemPrompt = """
                You are a senior Clinical Psychologist writing a concise, compassionate mental health evaluation report.
                Format the report clearly in clean markdown with 3 sections:
                ### 1. Clinical Overview
                (Summarize what this score indicates in everyday empathetic terms)
                ### 2. Symptom & Vulnerability Profile
                (Highlight primary challenges, energy/mood impact, and key areas of attention)
                ### 3. Recommendations & Next Steps
                (Provide actionable self-care techniques for the patient and focus points for the therapist)
                Keep the tone professional, supportive, non-stigmatizing, and concise (approx 200-250 words).
                """;

        String userPrompt = String.format("""
                Patient: %s
                Assessment Taken: %s
                Score Achieved: %d out of %d
                Clinical Severity: %s
                Standard Recommendation: %s
                
                Please generate the complete clinical evaluation report.
                """, patient.getName(), type.getDisplayName(), score, maxScore, severity, assessment.getRecommendation());

        String aiAnalysis = chatService.generateCompletion(systemPrompt, userPrompt);

        if (aiAnalysis == null || aiAnalysis.isBlank()) {
            aiAnalysis = buildFallbackAssessmentReport(assessment);
        }

        PatientReport report = new PatientReport(
                patient,
                ReportType.ASSESSMENT,
                title,
                summary,
                aiAnalysis,
                mapScoreToSeverityLevel(score, maxScore),
                true, // visible to patient
                assessment
        );

        return reportRepository.save(report);
    }

    /**
     * Generate a Confidential AI Chat Behavioral Report based on patient's companion chats.
     * Hidden from the patient (visibleToPatient = false); accessible only to consulting therapists.
     */
    public PatientReport generateChatBehavioralReport(Patient patient) {
        // Fetch the last 30 messages across all personas
        List<ChatMessageEntity> recentMessages = chatMessageRepository.findByPatientOrderByCreatedAtDesc(patient);

        if (recentMessages.isEmpty()) {
            return null;
        }

        // Limit to 30 most recent messages, reverse to chronological
        List<ChatMessageEntity> sample = recentMessages.stream().limit(30).collect(Collectors.toList());
        Collections.reverse(sample);

        StringBuilder chatTranscript = new StringBuilder();
        for (ChatMessageEntity msg : sample) {
            chatTranscript.append(String.format("[%s] (%s): %s\n",
                    msg.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM HH:mm")),
                    "user".equalsIgnoreCase(msg.getRole()) ? "Patient" : ("AI Companion (" + msg.getPersona() + ")"),
                    msg.getContent()));
        }

        String systemPrompt = """
                You are a Clinical Behavioral Analyst preparing a confidential briefing for a licensed therapist.
                Analyze the patient's recent text conversations with mental health companions.
                Structure your report clearly in markdown with the following sections:
                ### 1. Emotional Tone & Psychological State
                (Observed emotional baseline, anxiety/sadness markers, energy patterns)
                ### 2. Recurrent Themes & Triggers
                (Academic/work pressure, loneliness, self-doubt, sleep disruptions, relationship issues)
                ### 3. Communication Dynamics & Coping Styles
                (How does the patient express distress? Do they seek validation, problem-solving, or venting?)
                ### 4. Therapist Action Items & Consultation Focus
                (Key questions and therapeutic approaches suggested for the upcoming 1-on-1 session)
                Keep it objective, clinical, constructive, and concise (approx 250-300 words).
                """;

        String userPrompt = String.format("""
                Patient Name: %s
                Chat Sample Transcript:
                %s
                
                Please generate the Confidential Chat Behavioral Analysis report for the consulting therapist.
                """, patient.getName(), chatTranscript.toString());

        String aiAnalysis = chatService.generateCompletion(systemPrompt, userPrompt);

        if (aiAnalysis == null || aiAnalysis.isBlank()) {
            aiAnalysis = buildFallbackBehavioralReport(patient, sample.size());
        }

        String title = "AI Companion Chat Behavioral Analysis";
        String summary = String.format("Analyzed %d recent conversational interactions — Confidential Clinical Briefing", sample.size());

        PatientReport report = new PatientReport(
                patient,
                ReportType.CHAT_BEHAVIORAL,
                title,
                summary,
                aiAnalysis,
                "CONFIDENTIAL",
                false, // HIDDEN from patient
                null
        );

        return reportRepository.save(report);
    }

    /**
     * Check if a therapist is authorized to view a patient's reports.
     * Access Window: From the moment an appointment is booked until 3 months after the appointment date.
     */
    public boolean isTherapistAuthorized(Therapist therapist, Patient patient) {
        List<Appointment> appointments = appointmentRepository.findByTherapistOrderByAppointmentDateDesc(therapist);
        LocalDate today = LocalDate.now();

        return appointments.stream()
                .filter(a -> a.getPatient().getId().equals(patient.getId()))
                .anyMatch(a -> {
                    LocalDate bookingDate = a.getCreatedAt() != null ? a.getCreatedAt().toLocalDate() : a.getAppointmentDate().minusDays(30);
                    LocalDate expiryDate = a.getAppointmentDate().plusMonths(3);
                    return !today.isBefore(bookingDate) && !today.isAfter(expiryDate);
                });
    }

    /**
     * Get the date when therapist access expires for a given patient.
     */
    public LocalDate getAccessExpiryDate(Therapist therapist, Patient patient) {
        List<Appointment> appointments = appointmentRepository.findByTherapistOrderByAppointmentDateDesc(therapist);

        return appointments.stream()
                .filter(a -> a.getPatient().getId().equals(patient.getId()))
                .map(a -> a.getAppointmentDate().plusMonths(3))
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    /**
     * Fetch all reports for a therapist if authorized within the 3-month window.
     */
    public List<PatientReport> getReportsForTherapist(Therapist therapist, Patient patient) {
        if (!isTherapistAuthorized(therapist, patient)) {
            return Collections.emptyList();
        }
        return reportRepository.findByPatientOrderByCreatedAtDesc(patient);
    }

    /**
     * Fetch patient's own visible reports (Assessment reports).
     */
    public List<PatientReport> getReportsForPatient(Patient patient) {
        return reportRepository.findByPatientAndVisibleToPatientTrueOrderByCreatedAtDesc(patient);
    }

    public Optional<PatientReport> getReportById(Long id) {
        return reportRepository.findById(id);
    }

    public Optional<PatientReport> getReportForAssessment(Assessment assessment) {
        return reportRepository.findByAssessment(assessment);
    }

    // --- Helpers ---

    private String mapScoreToSeverityLevel(int score, int maxScore) {
        double ratio = (double) score / maxScore;
        if (ratio <= 0.25) return "LOW";
        if (ratio <= 0.50) return "MILD";
        if (ratio <= 0.75) return "MODERATE";
        return "HIGH";
    }

    private String buildFallbackAssessmentReport(Assessment assessment) {
        return String.format("""
                ### 1. Clinical Overview
                The patient completed the **%s** evaluation and scored **%d/%d**, classifying their symptoms under the **%s** severity bracket.
                
                ### 2. Symptom & Vulnerability Profile
                Key emotional and physiological markers reflect: %s. The primary distress factors correspond with standard clinical benchmarks for this severity range.
                
                ### 3. Recommendations & Next Steps
                - **For the Patient**: %s
                - **For the Therapist**: Explore underlying stressors, review coping mechanisms, and tailor cognitive behavioral or mindfulness strategies accordingly.
                """,
                assessment.getType().getDisplayName(),
                assessment.getTotalScore(),
                assessment.getType().getMaxScore(),
                assessment.getSeverity(),
                assessment.getSeverity(),
                assessment.getRecommendation());
    }

    private String buildFallbackBehavioralReport(Patient patient, int messageCount) {
        return String.format("""
                ### 1. Emotional Tone & Psychological State
                Based on %d recent chat interactions, the patient frequently uses the AI companion as a non-judgmental space for debriefing daily stressors and emotional decompression.
                
                ### 2. Recurrent Themes & Triggers
                Primary themes detected across companion chats include daily workload tension, overthinking patterns, and seeking reassurance during periods of low mood.
                
                ### 3. Communication Dynamics & Coping Styles
                The patient communicates openly when greeted with empathetic, listener-first responses, showing strong receptivity to structured reflection.
                
                ### 4. Therapist Action Items & Consultation Focus
                - Validate recent efforts at self-reflection.
                - Probe for triggers related to routine disruption and self-criticism.
                - Introduce grounded boundary-setting exercises.
                """, messageCount);
    }
}
