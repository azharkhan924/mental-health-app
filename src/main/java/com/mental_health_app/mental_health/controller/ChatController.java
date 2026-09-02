package com.mental_health_app.mental_health.controller;

import com.mental_health_app.mental_health.dto.ChatMessage;
import com.mental_health_app.mental_health.dto.ChatPersona;
import com.mental_health_app.mental_health.entity.ChatMessageEntity;
import com.mental_health_app.mental_health.entity.Patient;
import com.mental_health_app.mental_health.repository.ChatMessageRepository;
import com.mental_health_app.mental_health.service.ChatService;
import com.mental_health_app.mental_health.service.PatientService;
import com.mental_health_app.mental_health.service.ReportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * CHAT CONTROLLER
 * ───────────────
 * Serves a WhatsApp Web style 1-on-1 messaging interface with persistent memory:
 * Kabir (Bro), Aanya (Bestie), Dr. Priya (Psychologist), Rohan Sir (Mentor), and Meera (Zen Guide).
 * Automatically updates confidential behavioral reports for consulting therapists.
 */
@Controller
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    private final PatientService patientService;
    private final ChatMessageRepository chatMessageRepository;
    private final ReportService reportService;

    public ChatController(ChatService chatService,
                          PatientService patientService,
                          ChatMessageRepository chatMessageRepository,
                          ReportService reportService) {
        this.chatService = chatService;
        this.patientService = patientService;
        this.chatMessageRepository = chatMessageRepository;
        this.reportService = reportService;
    }

    /**
     * Show WhatsApp Web style chat interface with persistent message history.
     */
    @GetMapping
    public String showChatPage(@RequestParam(defaultValue = "KABIR") ChatPersona persona,
                               @AuthenticationPrincipal UserDetails userDetails,
                               HttpSession session,
                               Model model) {

        Optional<Patient> patientOpt = patientService.findByEmail(userDetails.getUsername());
        patientOpt.ifPresent(p -> model.addAttribute("userName", p.getName()));

        // Active persona
        model.addAttribute("currentPersona", persona);
        model.addAttribute("personas", ChatPersona.values());

        // Maps containing last message snippet and dynamic timestamp for sidebar
        Map<String, String> lastMessageMap = new HashMap<>();
        Map<String, String> sidebarTimeMap = new HashMap<>();
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");

        for (ChatPersona p : ChatPersona.values()) {
            List<ChatMessage> hist = loadChatHistory(patientOpt.orElse(null), session, p);
            if (!hist.isEmpty()) {
                ChatMessage last = hist.get(hist.size() - 1);
                String prefix = "user".equalsIgnoreCase(last.getRole()) ? "You: " : "";
                String preview = prefix + last.getContent();
                if (preview.length() > 38) {
                    preview = preview.substring(0, 35) + "...";
                }
                lastMessageMap.put(p.name(), preview);
                sidebarTimeMap.put(p.name(), last.getTimestamp() != null ? last.getTimestamp().format(timeFmt) : "today");
            } else {
                lastMessageMap.put(p.name(), p.getSubtitle());
                sidebarTimeMap.put(p.name(), "recently");
            }
        }
        model.addAttribute("lastMessageMap", lastMessageMap);
        model.addAttribute("sidebarTimeMap", sidebarTimeMap);

        // Active conversation messages
        List<ChatMessage> chatHistory = loadChatHistory(patientOpt.orElse(null), session, persona);
        model.addAttribute("messages", chatHistory);

        // Crisis detection
        boolean showCrisisAlert = false;
        for (ChatMessage msg : chatHistory) {
            if ("user".equals(msg.getRole()) && chatService.containsCrisisKeywords(msg.getContent())) {
                showCrisisAlert = true;
                break;
            }
        }
        model.addAttribute("showCrisisAlert", showCrisisAlert);

        return "chat";
    }

    /**
     * Send a message to the active companion with persistent contextual memory.
     */
    @PostMapping("/send")
    public String sendMessage(@RequestParam String message,
                              @RequestParam(defaultValue = "KABIR") ChatPersona persona,
                              @AuthenticationPrincipal UserDetails userDetails,
                              HttpSession session) {

        if (message == null || message.trim().isEmpty()) {
            return "redirect:/chat?persona=" + persona.name();
        }

        Optional<Patient> patientOpt = patientService.findByEmail(userDetails.getUsername());
        Patient patient = patientOpt.orElse(null);

        List<ChatMessage> chatHistory = loadChatHistory(patient, session, persona);

        // Get AI response using persona-specific system prompt and past conversation context
        String aiResponse = chatService.sendMessage(message.trim(), chatHistory, persona);

        ChatMessage userMsg = new ChatMessage("user", message.trim());
        ChatMessage assistantMsg = new ChatMessage("assistant", aiResponse);

        chatHistory.add(userMsg);
        chatHistory.add(assistantMsg);
        session.setAttribute("chatHistory_" + persona.name(), chatHistory);

        // Persist to Database for continuity and therapist behavioral analysis
        if (patient != null) {
            try {
                chatMessageRepository.save(new ChatMessageEntity(patient, persona.name(), "user", message.trim()));
                chatMessageRepository.save(new ChatMessageEntity(patient, persona.name(), "assistant", aiResponse));

                // Trigger behavioral report update if sufficient conversation data is recorded
                long count = chatMessageRepository.countByPatientAndRole(patient, "user");
                if (count >= 3 && count % 4 == 0) {
                    reportService.generateChatBehavioralReport(patient);
                }
            } catch (Exception e) {
                // Log and continue gracefully
            }
        }

        return "redirect:/chat?persona=" + persona.name();
    }

    /**
     * Clear chat conversation with the active companion.
     */
    @PostMapping("/clear")
    @Transactional
    public String clearChat(@RequestParam(defaultValue = "KABIR") ChatPersona persona,
                            @AuthenticationPrincipal UserDetails userDetails,
                            HttpSession session) {
        session.removeAttribute("chatHistory_" + persona.name());

        Optional<Patient> patientOpt = patientService.findByEmail(userDetails.getUsername());
        patientOpt.ifPresent(patient -> chatMessageRepository.deleteByPatientAndPersona(patient, persona.name()));

        return "redirect:/chat?persona=" + persona.name();
    }

    /**
     * Helper: Load chat history prioritizing persistent DB, fallback to session.
     */
    @SuppressWarnings("unchecked")
    private List<ChatMessage> loadChatHistory(Patient patient, HttpSession session, ChatPersona persona) {
        String sessionKey = "chatHistory_" + persona.name();

        if (patient != null) {
            List<ChatMessageEntity> dbMsgs = chatMessageRepository.findByPatientAndPersonaOrderByCreatedAtAsc(patient, persona.name());
            if (!dbMsgs.isEmpty()) {
                List<ChatMessage> converted = new ArrayList<>();
                for (ChatMessageEntity entity : dbMsgs) {
                    ChatMessage msg = new ChatMessage(entity.getRole(), entity.getContent());
                    msg.setTimestamp(entity.getCreatedAt());
                    converted.add(msg);
                }
                session.setAttribute(sessionKey, converted);
                return converted;
            }
        }

        List<ChatMessage> history = (List<ChatMessage>) session.getAttribute(sessionKey);
        if (history == null) {
            history = new ArrayList<>();
            session.setAttribute(sessionKey, history);
        }
        return history;
    }
}

