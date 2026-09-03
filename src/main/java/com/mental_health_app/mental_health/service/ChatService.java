package com.mental_health_app.mental_health.service;

import com.mental_health_app.mental_health.dto.ChatMessage;
import com.mental_health_app.mental_health.dto.ChatPersona;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * MULTI-PROVIDER & MULTI-PERSONA AI CHAT SERVICE
 * ─────────────────────────────────────────────
 * Dynamically applies specialized human-like system prompts for Saathi, Dr. Care,
 * Margdarshak, and Shanti while maintaining multi-key rotation and multi-provider failover.
 */
@Service
public class ChatService {

    @Value("${ai.provider:groq}")
    private String defaultProvider;

    @Value("${ai.groq.keys:}")
    private String groqKeysConfig;

    @Value("${ai.groq.model:openai/gpt-oss-20b}")
    private String groqModel;

    @Value("${ai.gemini.keys:}")
    private String geminiKeysConfig;

    @Value("${ai.gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    private final RestTemplate restTemplate = new RestTemplate();

    private int currentGroqKeyIndex = 0;
    private int currentGeminiKeyIndex = 0;

    /**
     * Send a message to AI using the selected persona's dedicated system prompt.
     */
    public String sendMessage(String userMessage, List<ChatMessage> history, ChatPersona persona) {
        return sendMessage(userMessage, history, persona, null);
    }

    /**
     * Send a message to AI using persona and patient demographics (age, gender, name)
     * so that AI responses are personalized, age-appropriate, and gender-sensitive.
     */
    public String sendMessage(String userMessage, List<ChatMessage> history, ChatPersona persona, com.mental_health_app.mental_health.entity.Patient patient) {
        String basePrompt = (persona != null) ? persona.getSystemPrompt() : ChatPersona.KABIR.getSystemPrompt();
        StringBuilder systemPrompt = new StringBuilder(basePrompt);

        if (patient != null) {
            systemPrompt.append("\n\nPATIENT DEMOGRAPHIC CONTEXT:");
            if (patient.getName() != null) systemPrompt.append("\n- Name: ").append(patient.getName());
            if (patient.getAge() != null) systemPrompt.append("\n- Age: ").append(patient.getAge()).append(" years old");
            if (patient.getGender() != null && !patient.getGender().isBlank()) systemPrompt.append("\n- Gender: ").append(patient.getGender());
        }

        String prompt = systemPrompt.toString();

        // 1. Try Groq with Key Rotation (350 tokens for conversational chat)
        List<String> groqKeys = getKeysList(groqKeysConfig);
        if (!groqKeys.isEmpty()) {
            String groqReply = tryGroqWithKeyRotation(userMessage, history, groqKeys, prompt, 350);
            if (groqReply != null && !groqReply.isBlank()) {
                return groqReply;
            }
        }

        // 2. Fallback: Try Gemini with Key Rotation
        List<String> geminiKeys = getKeysList(geminiKeysConfig);
        if (!geminiKeys.isEmpty()) {
            String geminiReply = tryGeminiWithKeyRotation(userMessage, history, geminiKeys, prompt, 350);
            if (geminiReply != null && !geminiReply.isBlank()) {
                return geminiReply;
            }
        }

        // 3. Graceful Final Fallback
        return "I hear you, and I am right here with you. I am experiencing a temporary connection hiccup, "
                + "but please take a slow, gentle breath. You can also explore our licensed therapists in the Book Session tab "
                + "or reach out to the 988 Crisis Lifeline anytime.";
    }

    /**
     * Generic completion generator for conversational use (350 tokens).
     */
    public String generateCompletion(String systemPrompt, String userPrompt) {
        return generateCompletionWithTokens(systemPrompt, userPrompt, 350);
    }

    /**
     * Report-specific completion generator with higher token budget (1024 tokens)
     * to prevent truncation of clinical reports and behavioral analyses.
     */
    public String generateReportCompletion(String systemPrompt, String userPrompt) {
        return generateCompletionWithTokens(systemPrompt, userPrompt, 1024);
    }

    /**
     * Custom token budget completion generator for specialized use cases
     * (e.g., pre-appointment diagnostic screening at 512 tokens).
     */
    public String generateCompletionWithCustomTokens(String systemPrompt, String userPrompt, int maxTokens) {
        return generateCompletionWithTokens(systemPrompt, userPrompt, maxTokens);
    }

    private String generateCompletionWithTokens(String systemPrompt, String userPrompt, int maxTokens) {
        List<ChatMessage> emptyHistory = Collections.emptyList();
        List<String> groqKeys = getKeysList(groqKeysConfig);
        if (!groqKeys.isEmpty()) {
            String groqReply = tryGroqWithKeyRotation(userPrompt, emptyHistory, groqKeys, systemPrompt, maxTokens);
            if (groqReply != null && !groqReply.isBlank()) {
                return groqReply;
            }
        }

        List<String> geminiKeys = getKeysList(geminiKeysConfig);
        if (!geminiKeys.isEmpty()) {
            String geminiReply = tryGeminiWithKeyRotation(userPrompt, emptyHistory, geminiKeys, systemPrompt, maxTokens);
            if (geminiReply != null && !geminiReply.isBlank()) {
                return geminiReply;
            }
        }
        return null;
    }

    /**
     * Call Groq API with round-robin key rotation on rate-limits / failures.
     */
    private String tryGroqWithKeyRotation(String userMessage, List<ChatMessage> history, List<String> keys, String systemPrompt, int maxTokens) {
        String url = "https://api.groq.com/openai/v1/chat/completions";
        int totalKeys = keys.size();

        // Take last 12 messages to keep strong short-term memory without context drift
        List<ChatMessage> recentHistory = (history != null && history.size() > 12)
                ? history.subList(history.size() - 12, history.size())
                : (history != null ? history : Collections.emptyList());

        for (int i = 0; i < totalKeys; i++) {
            int keyIndex = (currentGroqKeyIndex + i) % totalKeys;
            String apiKey = keys.get(keyIndex);

            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey.trim());

                Map<String, Object> payload = new HashMap<>();
                payload.put("model", groqModel);
                payload.put("temperature", 0.5); // Grounded, low-hallucination conversational temperature
                payload.put("max_tokens", maxTokens);

                List<Map<String, String>> messages = new ArrayList<>();
                messages.add(Map.of("role", "system", "content", systemPrompt));

                for (ChatMessage msg : recentHistory) {
                    String role = "user".equalsIgnoreCase(msg.getRole()) ? "user" : "assistant";
                    messages.add(Map.of("role", role, "content", msg.getContent()));
                }
                messages.add(Map.of("role", "user", "content", userMessage));

                payload.put("messages", messages);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map body = response.getBody();
                    List choices = (List) body.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map firstChoice = (Map) choices.get(0);
                        Map messageMap = (Map) firstChoice.get("message");
                        currentGroqKeyIndex = keyIndex; // lock on working key
                        return (String) messageMap.get("content");
                    }
                }

            } catch (Exception e) {
                currentGroqKeyIndex = (keyIndex + 1) % totalKeys;
            }
        }
        return null;
    }

    /**
     * Call Gemini API with round-robin key rotation on rate-limits / failures.
     */
    private String tryGeminiWithKeyRotation(String userMessage, List<ChatMessage> history, List<String> keys, String systemPrompt, int maxTokens) {
        int totalKeys = keys.size();

        // Take last 12 messages for grounded context
        List<ChatMessage> recentHistory = (history != null && history.size() > 12)
                ? history.subList(history.size() - 12, history.size())
                : (history != null ? history : Collections.emptyList());

        for (int i = 0; i < totalKeys; i++) {
            int keyIndex = (currentGeminiKeyIndex + i) % totalKeys;
            String apiKey = keys.get(keyIndex);
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + geminiModel + ":generateContent?key=" + apiKey.trim();

            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> payload = new HashMap<>();
                Map<String, Object> sysInst = Map.of("parts", List.of(Map.of("text", systemPrompt)));
                payload.put("system_instruction", sysInst);

                Map<String, Object> genConfig = Map.of("temperature", 0.5, "maxOutputTokens", maxTokens);
                payload.put("generationConfig", genConfig);

                List<Map<String, Object>> contents = new ArrayList<>();
                for (ChatMessage msg : recentHistory) {
                    String role = "assistant".equalsIgnoreCase(msg.getRole()) ? "model" : "user";
                    contents.add(Map.of("role", role, "parts", List.of(Map.of("text", msg.getContent()))));
                }
                contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", userMessage))));
                payload.put("contents", contents);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map body = response.getBody();
                    List candidates = (List) body.get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        Map firstCandidate = (Map) candidates.get(0);
                        Map content = (Map) firstCandidate.get("content");
                        List parts = (List) content.get("parts");
                        currentGeminiKeyIndex = keyIndex;
                        return (String) ((Map) parts.get(0)).get("text");
                    }
                }
            } catch (Exception e) {
                currentGeminiKeyIndex = (keyIndex + 1) % totalKeys;
            }
        }
        return null;
    }

    /**
     * Helper to parse comma-separated API keys.
     */
    private List<String> getKeysList(String keysConfig) {
        if (keysConfig == null || keysConfig.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(keysConfig.split(","))
                .map(String::trim)
                .filter(k -> !k.isBlank() && !k.startsWith("YOUR_") && !k.contains("your_"))
                .toList();
    }

    /**
     * Crisis keyword detector for emergency helpline trigger (Bilingual: English & Hinglish).
     */
    public boolean containsCrisisKeywords(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        String[] crisisWords = {
            "suicide", "kill myself", "end my life", "self-harm",
            "want to die", "hurt myself", "no reason to live", "better off dead",
            "marne ka mann", "mar jaana chahta", "jaan de dunga", "khatam karna chahta", "zindagi khatam"
        };
        for (String word : crisisWords) {
            if (lower.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
