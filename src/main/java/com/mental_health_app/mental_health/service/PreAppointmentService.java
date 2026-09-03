package com.mental_health_app.mental_health.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mental_health_app.mental_health.entity.*;
import com.mental_health_app.mental_health.repository.PreAppointmentTaskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * PRE-APPOINTMENT DIAGNOSTIC SERVICE
 * ───────────────────────────────────
 * Manages AI-guided pre-appointment conversations where the AI follows therapist-defined
 * instructions to collect diagnostic information from patients before consultations.
 * Includes patient demographic awareness (age, gender) in AI context.
 */
@Service
public class PreAppointmentService {

    private final PreAppointmentTaskRepository taskRepository;
    private final ChatService chatService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PreAppointmentService(PreAppointmentTaskRepository taskRepository,
                                  ChatService chatService) {
        this.taskRepository = taskRepository;
        this.chatService = chatService;
    }

    /**
     * Create a new pre-appointment diagnostic task.
     */
    public PreAppointmentTask createTask(Therapist therapist, Patient patient,
                                          String title, String instructions, Appointment appointment) {
        PreAppointmentTask task = new PreAppointmentTask(therapist, patient, title, instructions);
        if (appointment != null) {
            task.setAppointment(appointment);
        }
        return taskRepository.save(task);
    }

    /**
     * Send a message in the pre-appointment diagnostic conversation.
     * The AI uses the therapist's instructions as the system prompt, with patient
     * demographic context (name, age, gender) for clinically aware responses.
     */
    public Map<String, Object> sendMessage(Long taskId, String userMessage) {
        PreAppointmentTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        if ("COMPLETED".equals(task.getStatus())) {
            throw new IllegalStateException("This pre-appointment check-in has already been completed.");
        }

        // Mark as in-progress on first message
        if ("PENDING".equals(task.getStatus())) {
            task.setStatus("IN_PROGRESS");
        }

        // Load existing conversation
        List<Map<String, String>> conversation = loadConversation(task);

        // Build system prompt with therapist instructions + patient demographics
        Patient patient = task.getPatient();
        String demographicContext = buildDemographicContext(patient);

        String systemPrompt = String.format("""
                You are a compassionate clinical screening assistant conducting a pre-appointment 
                diagnostic interview on behalf of Dr. %s (%s).
                
                PATIENT INFORMATION:
                %s
                
                THERAPIST'S INSTRUCTIONS FOR THIS SESSION:
                %s
                
                BEHAVIORAL GUIDELINES:
                - Follow the therapist's instructions carefully to gather the requested diagnostic information
                - Be warm, empathetic, and non-judgmental — you are speaking to a real patient
                - Ask ONE focused question at a time, then wait for the patient's response
                - Adapt follow-up questions based on their answers
                - Use age-appropriate and gender-sensitive language
                - If the patient shares something concerning (crisis/self-harm), acknowledge it compassionately and note it will be flagged for immediate clinical attention
                - Keep responses concise (2-4 sentences max per turn)
                - Do NOT provide diagnoses or clinical recommendations — your role is to COLLECT information for the therapist
                - After 6-8 meaningful exchanges, gently let the patient know they can click "Complete & Submit" when they feel ready
                """,
                task.getTherapist().getName(),
                task.getTherapist().getSpecialization() != null ? task.getTherapist().getSpecialization() : "Mental Health",
                demographicContext,
                task.getInstructions());

        // Convert conversation history to ChatMessage format for the AI
        List<com.mental_health_app.mental_health.dto.ChatMessage> chatHistory = new ArrayList<>();
        for (Map<String, String> msg : conversation) {
            chatHistory.add(new com.mental_health_app.mental_health.dto.ChatMessage(msg.get("role"), msg.get("content")));
        }

        // Get AI response with higher token budget for diagnostic screening (512 tokens)
        String aiResponse = chatService.sendMessage(userMessage, chatHistory, null);

        // Override with direct completion using our custom system prompt
        aiResponse = chatService.generateCompletionWithCustomTokens(systemPrompt, buildConversationPrompt(conversation, userMessage), 512);

        if (aiResponse == null || aiResponse.isBlank()) {
            aiResponse = "I appreciate you sharing that with me. Could you tell me a bit more about how this has been affecting your day-to-day life?";
        }

        // Append both messages to conversation
        conversation.add(Map.of("role", "user", "content", userMessage));
        conversation.add(Map.of("role", "assistant", "content", aiResponse));

        // Save updated conversation
        try {
            task.setConversationJson(objectMapper.writeValueAsString(conversation));
        } catch (Exception ignored) {
        }
        taskRepository.save(task);

        Map<String, Object> result = new HashMap<>();
        result.put("aiResponse", aiResponse);
        result.put("messageCount", conversation.size());
        return result;
    }

    /**
     * Complete the task and generate an AI summary for the therapist.
     */
    public PreAppointmentTask completeTask(Long taskId) {
        PreAppointmentTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        List<Map<String, String>> conversation = loadConversation(task);

        // Build transcript for AI summary
        StringBuilder transcript = new StringBuilder();
        for (Map<String, String> msg : conversation) {
            String speaker = "user".equals(msg.get("role")) ? "Patient" : "AI Screener";
            transcript.append(String.format("[%s]: %s\n", speaker, msg.get("content")));
        }

        Patient patient = task.getPatient();
        String demographicContext = buildDemographicContext(patient);

        String systemPrompt = """
                You are a Clinical Psychologist preparing a pre-appointment diagnostic briefing for a therapist.
                Analyze the screening conversation and produce a concise, structured summary.
                You MUST output clean HTML (no markdown). Structure exactly as follows:
                
                <h3>📋 Pre-Appointment Screening Summary</h3>
                <p>2-3 sentence overview of what was discussed and the patient's general presentation.</p>
                
                <h3>🔍 Key Symptoms & Concerns Identified</h3>
                <ul>
                <li><strong>Symptom/Concern</strong> — Description with patient's own words where relevant</li>
                </ul>
                (3-5 bullet points)
                
                <h3>⚠️ Risk Flags & Clinical Alerts</h3>
                <ul>
                <li><strong>Flag</strong> — Any concerning patterns, crisis indicators, or urgent items</li>
                </ul>
                (If none, state "No immediate risk flags identified in this screening.")
                
                <h3>💡 Suggested Session Focus Areas</h3>
                <ul>
                <li><strong>Area</strong> — Recommended topics for the therapist to explore during the appointment</li>
                </ul>
                (3-4 bullet points)
                
                Rules:
                - Be objective, clinical, and concise (max 350 words)
                - Include the patient's demographic context in your analysis
                - Output ONLY the HTML, no markdown, no code fences
                """;

        String userPrompt = String.format("""
                PATIENT DEMOGRAPHICS:
                %s
                
                THERAPIST'S ORIGINAL INSTRUCTIONS:
                %s
                
                SCREENING CONVERSATION TRANSCRIPT:
                %s
                """, demographicContext, task.getInstructions(), transcript.toString());

        String aiSummary = chatService.generateReportCompletion(systemPrompt, userPrompt);

        if (aiSummary == null || aiSummary.isBlank()) {
            aiSummary = String.format("""
                    <h3>📋 Pre-Appointment Screening Summary</h3>
                    <p>Patient %s completed a pre-appointment screening conversation with %d exchanges. 
                    The conversation followed Dr. %s's instructions and covered the requested diagnostic areas.</p>
                    
                    <h3>🔍 Key Symptoms & Concerns Identified</h3>
                    <ul>
                    <li><strong>Self-reported concerns</strong> — Patient engaged with the screening questions and provided responses for clinical review</li>
                    <li><strong>Conversation engagement</strong> — Patient showed willingness to communicate about their experiences</li>
                    </ul>
                    
                    <h3>💡 Suggested Session Focus Areas</h3>
                    <ul>
                    <li><strong>Review full transcript</strong> — The complete conversation is available for detailed analysis</li>
                    <li><strong>Follow up on key themes</strong> — Explore topics the patient raised during screening</li>
                    </ul>
                    """, patient.getName(), conversation.size(), task.getTherapist().getName());
        }

        task.setAiSummary(aiSummary);
        task.setStatus("COMPLETED");
        task.setCompletedAt(LocalDateTime.now());

        return taskRepository.save(task);
    }

    // --- Query Methods ---

    public List<PreAppointmentTask> getTasksForPatient(Patient patient) {
        return taskRepository.findByPatientOrderByCreatedAtDesc(patient);
    }

    public List<PreAppointmentTask> getPendingTasksForPatient(Patient patient) {
        List<PreAppointmentTask> pending = taskRepository.findByPatientAndStatusOrderByCreatedAtDesc(patient, "PENDING");
        List<PreAppointmentTask> inProgress = taskRepository.findByPatientAndStatusOrderByCreatedAtDesc(patient, "IN_PROGRESS");
        List<PreAppointmentTask> combined = new ArrayList<>(pending);
        combined.addAll(inProgress);
        return combined;
    }

    public List<PreAppointmentTask> getTasksForTherapist(Therapist therapist) {
        return taskRepository.findByTherapistOrderByCreatedAtDesc(therapist);
    }

    public Optional<PreAppointmentTask> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    // --- Helpers ---

    private String buildDemographicContext(Patient patient) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("Name: ").append(patient.getName());
        if (patient.getAge() != null) {
            ctx.append("\nAge: ").append(patient.getAge()).append(" years old");
        }
        if (patient.getGender() != null && !patient.getGender().isBlank()) {
            ctx.append("\nGender: ").append(patient.getGender());
        }
        if (patient.getPhoneNumber() != null && !patient.getPhoneNumber().isBlank()) {
            ctx.append("\nContact: ").append(patient.getPhoneNumber());
        }
        return ctx.toString();
    }

    private List<Map<String, String>> loadConversation(PreAppointmentTask task) {
        try {
            if (task.getConversationJson() != null && !task.getConversationJson().isBlank()) {
                return objectMapper.readValue(task.getConversationJson(), new TypeReference<List<Map<String, String>>>() {});
            }
        } catch (Exception ignored) {
        }
        return new ArrayList<>();
    }

    private String buildConversationPrompt(List<Map<String, String>> conversation, String latestMessage) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> msg : conversation) {
            String speaker = "user".equals(msg.get("role")) ? "Patient" : "You (Screener)";
            sb.append(speaker).append(": ").append(msg.get("content")).append("\n");
        }
        sb.append("Patient: ").append(latestMessage).append("\n");
        sb.append("You (Screener):");
        return sb.toString();
    }
}
