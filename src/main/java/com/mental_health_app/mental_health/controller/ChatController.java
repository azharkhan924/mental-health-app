package com.mental_health_app.mental_health.controller;

import com.mental_health_app.mental_health.dto.ChatMessage;
import com.mental_health_app.mental_health.dto.ChatPersona;
import com.mental_health_app.mental_health.entity.Patient;
import com.mental_health_app.mental_health.service.ChatService;
import com.mental_health_app.mental_health.service.PatientService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * CHAT CONTROLLER
 * ───────────────
 * Serves a WhatsApp Web style 1-on-1 messaging interface with real human-like profiles:
 * Kabir (Bro), Aanya (Bestie), Dr. Priya (Psychologist), Rohan Sir (Mentor), and Meera (Zen Guide).
 */
@Controller
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    private final PatientService patientService;

    public ChatController(ChatService chatService, PatientService patientService) {
        this.chatService = chatService;
        this.patientService = patientService;
    }

    /**
     * Show WhatsApp Web style chat interface.
     */
    @GetMapping
    public String showChatPage(@RequestParam(defaultValue = "KABIR") ChatPersona persona,
                               @AuthenticationPrincipal UserDetails userDetails,
                               HttpSession session,
                               Model model) {

        Optional<Patient> patient = patientService.findByEmail(userDetails.getUsername());
        patient.ifPresent(p -> model.addAttribute("userName", p.getName()));

        // Active persona
        model.addAttribute("currentPersona", persona);
        model.addAttribute("personas", ChatPersona.values());

        // Maps containing last message snippet and dynamic timestamp for sidebar
        Map<String, String> lastMessageMap = new HashMap<>();
        Map<String, String> sidebarTimeMap = new HashMap<>();
        java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");

        for (ChatPersona p : ChatPersona.values()) {
            List<ChatMessage> hist = getChatHistory(session, p);
            if (!hist.isEmpty()) {
                ChatMessage last = hist.get(hist.size() - 1);
                String prefix = "user".equalsIgnoreCase(last.getRole()) ? "You: " : "";
                String preview = prefix + last.getContent();
                if (preview.length() > 38) {
                    preview = preview.substring(0, 35) + "...";
                }
                lastMessageMap.put(p.name(), preview);
                sidebarTimeMap.put(p.name(), last.getTimestamp().format(timeFmt));
            } else {
                lastMessageMap.put(p.name(), p.getSubtitle());
                sidebarTimeMap.put(p.name(), "recently");
            }
        }
        model.addAttribute("lastMessageMap", lastMessageMap);
        model.addAttribute("sidebarTimeMap", sidebarTimeMap);

        // Active conversation messages
        List<ChatMessage> chatHistory = getChatHistory(session, persona);
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
     * Send a message to the active companion.
     */
    @PostMapping("/send")
    public String sendMessage(@RequestParam String message,
                              @RequestParam(defaultValue = "KABIR") ChatPersona persona,
                              HttpSession session) {

        if (message == null || message.trim().isEmpty()) {
            return "redirect:/chat?persona=" + persona.name();
        }

        List<ChatMessage> chatHistory = getChatHistory(session, persona);

        // Get AI response using persona-specific system prompt
        String aiResponse = chatService.sendMessage(message.trim(), chatHistory, persona);

        // Append user and reply
        chatHistory.add(new ChatMessage("user", message.trim()));
        chatHistory.add(new ChatMessage("assistant", aiResponse));

        session.setAttribute("chatHistory_" + persona.name(), chatHistory);

        return "redirect:/chat?persona=" + persona.name();
    }

    /**
     * Clear chat conversation with the active companion.
     */
    @PostMapping("/clear")
    public String clearChat(@RequestParam(defaultValue = "KABIR") ChatPersona persona,
                            HttpSession session) {
        session.removeAttribute("chatHistory_" + persona.name());
        return "redirect:/chat?persona=" + persona.name();
    }

    /**
     * Helper: Get session chat history for a specific persona.
     */
    @SuppressWarnings("unchecked")
    private List<ChatMessage> getChatHistory(HttpSession session, ChatPersona persona) {
        String sessionKey = "chatHistory_" + persona.name();
        List<ChatMessage> history = (List<ChatMessage>) session.getAttribute(sessionKey);
        if (history == null) {
            history = new ArrayList<>();
            session.setAttribute(sessionKey, history);
        }
        return history;
    }
}
