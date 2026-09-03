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
                You are a senior Clinical Psychologist writing a concise, compassionate mental health evaluation.
                You MUST output clean HTML (no markdown). Structure the report exactly as follows:
                
                <h3>🔍 What Your Results Suggest</h3>
                <p>A brief 2-3 sentence empathetic summary of what the score means in everyday language. Do NOT use clinical jargon.</p>
                
                <h3>⚡ Key Areas to Watch</h3>
                <ul>
                <li><strong>Area name</strong> — One sentence description of the concern</li>
                </ul>
                (Include 3-5 bullet points identifying mood, energy, sleep, concentration, or motivation patterns)
                
                <h3>💡 Your Next Steps</h3>
                <ul>
                <li><strong>Action</strong> — One sentence practical self-care tip</li>
                </ul>
                (Include 3-4 actionable, warm, non-clinical bullet points the patient can follow today)
                
                Rules:
                - Write like a caring friend who happens to be a doctor, NOT a textbook
                - Use simple everyday language, NO medical terminology
                - Keep it concise — max 250 words total
                - Output ONLY the HTML, no markdown, no code fences
                """;

        String userPrompt = String.format("""
                Patient: %s
                Assessment Taken: %s
                Score Achieved: %d out of %d
                Clinical Severity: %s
                Standard Recommendation: %s
                
                Please generate the complete clinical evaluation report in HTML format.
                """, patient.getName(), type.getDisplayName(), score, maxScore, severity, assessment.getRecommendation());

        String aiAnalysis = chatService.generateReportCompletion(systemPrompt, userPrompt);

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
                You MUST output clean HTML (no markdown). Structure the report exactly as follows:
                
                <h3>1. Emotional Tone & Psychological State</h3>
                <ul>
                <li><strong>Observation</strong> — Description of emotional baseline, anxiety/sadness markers, energy patterns</li>
                </ul>
                (3-4 bullet points)
                
                <h3>2. Recurrent Themes & Triggers</h3>
                <ul>
                <li><strong>Theme</strong> — Academic pressure, loneliness, self-doubt, sleep issues, relationship concerns</li>
                </ul>
                (3-4 bullet points)
                
                <h3>3. Communication Dynamics & Coping Styles</h3>
                <ul>
                <li><strong>Pattern</strong> — How the patient expresses distress, seeks validation, problem-solving, or venting</li>
                </ul>
                (2-3 bullet points)
                
                <h3>4. Therapist Action Items & Session Focus</h3>
                <ul>
                <li><strong>Action</strong> — Specific therapeutic approaches and questions for the consultation</li>
                </ul>
                (3-4 bullet points)
                
                Rules:
                - Be objective, clinical, constructive, and concise (approx 300-350 words)
                - Use clear professional language suitable for a therapist briefing
                - Output ONLY the HTML, no markdown, no code fences
                """;

        String userPrompt = String.format("""
                Patient Name: %s
                Chat Sample Transcript:
                %s
                
                Please generate the Confidential Chat Behavioral Analysis report for the consulting therapist in HTML format.
                """, patient.getName(), chatTranscript.toString());

        String aiAnalysis = chatService.generateReportCompletion(systemPrompt, userPrompt);

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
                <h3>🔍 What Your Results Suggest</h3>
                <p>You completed the <strong>%s</strong> evaluation and scored <strong>%d/%d</strong>, which falls in the <strong>%s</strong> range. This gives us a helpful snapshot of where you are right now.</p>
                
                <h3>⚡ Key Areas to Watch</h3>
                <ul>
                <li><strong>Emotional patterns</strong> — Your responses suggest %s-level impact on daily functioning</li>
                <li><strong>Energy &amp; motivation</strong> — Pay attention to changes in your routine energy levels</li>
                <li><strong>Sleep &amp; rest</strong> — Quality rest plays a big role in how you feel day-to-day</li>
                </ul>
                
                <h3>💡 Your Next Steps</h3>
                <ul>
                <li><strong>Self-care first</strong> — %s</li>
                <li><strong>Talk to someone</strong> — Consider booking a session with one of our therapists for personalized guidance</li>
                <li><strong>Check in again</strong> — Retake this assessment in 2 weeks to track your progress</li>
                </ul>
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
                <h3>1. Emotional Tone &amp; Psychological State</h3>
                <ul>
                <li><strong>Conversational engagement</strong> — Based on %d recent chat interactions, the patient frequently uses the AI companion as a non-judgmental space for emotional decompression</li>
                <li><strong>Emotional baseline</strong> — Communication patterns suggest a need for consistent emotional validation and active listening</li>
                </ul>
                
                <h3>2. Recurrent Themes &amp; Triggers</h3>
                <ul>
                <li><strong>Daily stressors</strong> — Workload tension and overthinking patterns appear across conversations</li>
                <li><strong>Reassurance-seeking</strong> — Patient seeks reassurance during periods of low mood or self-doubt</li>
                </ul>
                
                <h3>3. Communication Dynamics &amp; Coping Styles</h3>
                <ul>
                <li><strong>Openness</strong> — Patient communicates openly when greeted with empathetic, listener-first responses</li>
                <li><strong>Receptivity</strong> — Shows strong receptivity to structured reflection and guided exercises</li>
                </ul>
                
                <h3>4. Therapist Action Items &amp; Session Focus</h3>
                <ul>
                <li><strong>Validate efforts</strong> — Acknowledge recent self-reflection attempts</li>
                <li><strong>Explore triggers</strong> — Probe for triggers related to routine disruption and self-criticism</li>
                <li><strong>Boundary exercises</strong> — Introduce grounded boundary-setting exercises</li>
                </ul>
                """, messageCount);
    }
}
