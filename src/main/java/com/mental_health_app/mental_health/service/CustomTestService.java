package com.mental_health_app.mental_health.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mental_health_app.mental_health.entity.*;
import com.mental_health_app.mental_health.repository.CustomTestAssignmentRepository;
import com.mental_health_app.mental_health.repository.CustomTestRepository;
import com.mental_health_app.mental_health.repository.PatientRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * CUSTOM TEST SERVICE
 * ───────────────────
 * Supports multi-format document upload (PDF, DOCX, TXT, CSV),
 * AI-powered psychometric question structuring, reusable test template storage,
 * multi-patient assignment, and automated scoring & evaluation.
 */
@Service
public class CustomTestService {

    private final CustomTestRepository customTestRepository;
    private final CustomTestAssignmentRepository assignmentRepository;
    private final PatientRepository patientRepository;
    private final ChatService chatService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CustomTestService(CustomTestRepository customTestRepository,
                             CustomTestAssignmentRepository assignmentRepository,
                             PatientRepository patientRepository,
                             ChatService chatService) {
        this.customTestRepository = customTestRepository;
        this.assignmentRepository = assignmentRepository;
        this.patientRepository = patientRepository;
        this.chatService = chatService;
    }

    /**
     * Extract raw text from any uploaded file format (.pdf, .docx, .txt, .csv, .json, .md).
     */
    public String extractTextFromFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }

        String filename = (file.getOriginalFilename() != null) ? file.getOriginalFilename().toLowerCase() : "";

        try {
            if (filename.endsWith(".pdf")) {
                try (PDDocument document = Loader.loadPDF(file.getBytes())) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    return stripper.getText(document);
                }
            } else if (filename.endsWith(".docx")) {
                try (InputStream is = file.getInputStream();
                     XWPFDocument doc = new XWPFDocument(is);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                    return extractor.getText();
                }
            } else {
                // Fallback for .txt, .csv, .json, .md or raw text files
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not read uploaded document: " + e.getMessage(), e);
        }
    }

    /**
     * Ask AI to structure and format unstructured questions into a standardized test template JSON.
     */
    public Map<String, Object> parseQuestionsWithAI(String rawText, String userTitle) {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("Question text or document content cannot be empty.");
        }

        String systemPrompt = """
                You are a senior Psychometrician and Clinical Assessment Designer.
                The user will provide text, notes, or questions from a document.
                Your job is to extract, clean, and organize all questions into a standardized psychological self-assessment test.
                
                You MUST return STRICT JSON ONLY (no markdown code fences, no introductory text, no trailing comments).
                
                JSON Schema:
                {
                  "title": "String (use provided title or synthesize a professional clinical title)",
                  "description": "String (1-2 sentences explaining what this test measures and instructions for the patient)",
                  "category": "String (e.g. Stress, Anxiety, Emotional Wellbeing, Habits, Burnout, Relationships)",
                  "questions": [
                    {
                      "id": 1,
                      "question": "Clean question text",
                      "options": [
                        {"label": "Not at all", "score": 0},
                        {"label": "Several days", "score": 1},
                        {"label": "More than half the days", "score": 2},
                        {"label": "Nearly every day", "score": 3}
                      ]
                    }
                  ],
                  "scoringRules": [
                    {"minScore": 0, "maxScore": 4, "severity": "Low", "interpretation": "Minimal symptoms within normal baseline range."},
                    {"minScore": 5, "maxScore": 9, "severity": "Mild", "interpretation": "Mild signs indicating attention to self-care is helpful."},
                    {"minScore": 10, "maxScore": 15, "severity": "Moderate", "interpretation": "Moderate clinical impact. Consultation recommended."},
                    {"minScore": 16, "maxScore": 30, "severity": "High", "interpretation": "Significant clinical distress. Active clinical support strongly advised."}
                  ]
                }
                
                Ensure every question has at least 2 to 5 clear options with numeric scores starting from 0.
                """;

        String userPrompt = "Custom Title Hint: " + (userTitle != null ? userTitle : "Clinical Assessment") + "\n\n"
                + "Raw Document Text:\n" + rawText;

        String aiResponse = chatService.generateReportCompletion(systemPrompt, userPrompt);

        if (aiResponse != null) {
            String cleaned = cleanJsonOutput(aiResponse);
            try {
                return objectMapper.readValue(cleaned, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                // Ignore and fall back to manual heuristic parser
            }
        }

        // Graceful Fallback: Build structured template using text lines
        return buildFallbackParsedQuestions(rawText, userTitle);
    }

    /**
     * Save a generated test as a reusable template on the therapist's profile.
     */
    public CustomTest saveCustomTest(Therapist therapist, String title, String description, 
                                     String category, String questionsJson, String scoringRulesJson) {
        int questionCount = 0;
        int maxScore = 0;

        try {
            List<Map<String, Object>> questions = objectMapper.readValue(questionsJson, new TypeReference<List<Map<String, Object>>>() {});
            questionCount = questions.size();

            for (Map<String, Object> q : questions) {
                List<Map<String, Object>> options = (List<Map<String, Object>>) q.get("options");
                if (options != null && !options.isEmpty()) {
                    int maxOptionScore = 0;
                    for (Map<String, Object> opt : options) {
                        Number sc = (Number) opt.get("score");
                        if (sc != null && sc.intValue() > maxOptionScore) {
                            maxOptionScore = sc.intValue();
                        }
                    }
                    maxScore += maxOptionScore;
                }
            }
        } catch (Exception ignored) {
            questionCount = 5;
            maxScore = 15;
        }

        CustomTest test = new CustomTest(
                therapist,
                title,
                description,
                category,
                questionsJson,
                scoringRulesJson,
                maxScore,
                questionCount
        );

        return customTestRepository.save(test);
    }

    /**
     * Assign a custom test to multiple patients.
     */
    public List<CustomTestAssignment> assignTestToPatients(Long testId, Therapist therapist, List<Long> patientIds) {
        CustomTest test = customTestRepository.findById(testId)
                .orElseThrow(() -> new IllegalArgumentException("Test not found"));

        List<CustomTestAssignment> assignments = new ArrayList<>();

        for (Long patientId : patientIds) {
            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            if (patientOpt.isPresent()) {
                Patient patient = patientOpt.get();
                // Avoid redundant pending duplicate assignments
                if (!assignmentRepository.existsByCustomTestAndPatientAndStatus(test, patient, "PENDING")) {
                    CustomTestAssignment assignment = new CustomTestAssignment(test, patient, therapist);
                    assignments.add(assignmentRepository.save(assignment));
                }
            }
        }
        return assignments;
    }

    /**
     * Evaluate submitted answers for a custom test assignment, calculate total score & severity,
     * and generate an AI Clinical Interpretation.
     */
    public CustomTestAssignment evaluateAndCompleteAssignment(Long assignmentId, Map<String, String> answersMap) {
        CustomTestAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        CustomTest test = assignment.getCustomTest();
        int totalScore = 0;
        List<Map<String, Object>> questionList = Collections.emptyList();

        try {
            questionList = objectMapper.readValue(test.getQuestionsJson(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ignored) {
        }

        Map<String, String> recordedAnswers = new HashMap<>();
        for (Map<String, Object> q : questionList) {
            Object qId = q.get("id");
            String key = "q" + qId;
            String val = answersMap.get(key);
            if (val != null) {
                try {
                    int scoreVal = Integer.parseInt(val);
                    totalScore += scoreVal;
                    recordedAnswers.put(String.valueOf(qId), String.valueOf(scoreVal));
                } catch (Exception ignored) {
                }
            }
        }

        // Determine severity level based on rules
        String severity = "Moderate";
        String interpretationNote = "Symptoms observed warrant structured follow-up.";

        try {
            if (test.getScoringRulesJson() != null && !test.getScoringRulesJson().isBlank()) {
                List<Map<String, Object>> rules = objectMapper.readValue(test.getScoringRulesJson(), new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> rule : rules) {
                    Number min = (Number) rule.get("minScore");
                    Number max = (Number) rule.get("maxScore");
                    if (min != null && max != null && totalScore >= min.intValue() && totalScore <= max.intValue()) {
                        severity = (String) rule.get("severity");
                        if (rule.containsKey("interpretation")) {
                            interpretationNote = (String) rule.get("interpretation");
                        }
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // Generate AI clinical analysis for this specific custom test
        String systemPrompt = """
                You are a senior Clinical Psychologist reviewing a completed custom psychological evaluation.
                Provide an empathetic, structured clinical summary in clean HTML format:
                <h3>🔍 Evaluation Summary</h3>
                <p>2-3 sentences explaining what this score reflects.</p>
                <h3>⚡ Key Observations</h3>
                <ul>
                  <li><strong>Area</strong> — Description of functional impact</li>
                </ul>
                <h3>💡 Recommendations</h3>
                <ul>
                  <li><strong>Next Step</strong> — Practical self-care and therapeutic guidance</li>
                </ul>
                Do NOT output markdown. Return ONLY the HTML.
                """;

        String userPrompt = String.format("""
                Patient: %s
                Assessment: %s (%s)
                Score: %d out of %d
                Severity: %s
                Interpretation Benchmarks: %s
                """, assignment.getPatient().getName(), test.getTitle(), test.getCategory(),
                totalScore, test.getMaxScore(), severity, interpretationNote);

        String aiEvaluation = chatService.generateReportCompletion(systemPrompt, userPrompt);
        if (aiEvaluation == null || aiEvaluation.isBlank()) {
            aiEvaluation = String.format("""
                    <h3>🔍 Evaluation Summary</h3>
                    <p>You completed the <strong>%s</strong> evaluation with a calculated score of <strong>%d/%d</strong> (%s severity bracket).</p>
                    <h3>⚡ Key Observations</h3>
                    <ul>
                      <li><strong>Symptom Level</strong> — Responses indicate %s clinical impact across surveyed markers.</li>
                      <li><strong>Assessment Focus</strong> — %s</li>
                    </ul>
                    <h3>💡 Recommendations</h3>
                    <ul>
                      <li><strong>Therapeutic Consultation</strong> — Discuss these findings with Dr. %s during your next session.</li>
                      <li><strong>Self-Care Routine</strong> — Prioritize consistent sleep, gentle mindfulness, and open communication.</li>
                    </ul>
                    """, test.getTitle(), totalScore, test.getMaxScore(), severity,
                    severity, interpretationNote, assignment.getAssignedBy().getName());
        }

        try {
            assignment.setPatientAnswersJson(objectMapper.writeValueAsString(recordedAnswers));
        } catch (Exception ignored) {
        }

        assignment.setTotalScore(totalScore);
        assignment.setSeverity(severity);
        assignment.setAiEvaluation(aiEvaluation);
        assignment.setStatus("COMPLETED");
        assignment.setCompletedAt(LocalDateTime.now());

        return assignmentRepository.save(assignment);
    }

    public List<CustomTest> getTestsByTherapist(Therapist therapist) {
        return customTestRepository.findByTherapistOrderByCreatedAtDesc(therapist);
    }

    public List<CustomTestAssignment> getAssignmentsForPatient(Patient patient) {
        return assignmentRepository.findByPatientOrderByAssignedAtDesc(patient);
    }

    public List<CustomTestAssignment> getPendingAssignmentsForPatient(Patient patient) {
        return assignmentRepository.findByPatientAndStatusOrderByAssignedAtDesc(patient, "PENDING");
    }

    public List<CustomTestAssignment> getAssignmentsByTherapist(Therapist therapist) {
        return assignmentRepository.findByAssignedByOrderByAssignedAtDesc(therapist);
    }

    public Optional<CustomTestAssignment> getAssignmentById(Long id) {
        return assignmentRepository.findById(id);
    }

    public Optional<CustomTest> getTestById(Long id) {
        return customTestRepository.findById(id);
    }

    // --- Helpers ---

    private String cleanJsonOutput(String text) {
        String cleaned = text.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private Map<String, Object> buildFallbackParsedQuestions(String rawText, String userTitle) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", (userTitle != null && !userTitle.isBlank()) ? userTitle : "Custom Psychological Assessment");
        map.put("description", "Please reflect on your experiences over the past two weeks and rate each statement honestly.");
        map.put("category", "General Wellbeing");

        List<Map<String, Object>> questions = new ArrayList<>();
        String[] lines = rawText.split("\\r?\\n");
        int count = 1;

        List<Map<String, Object>> defaultOptions = List.of(
                Map.of("label", "Not at all", "score", 0),
                Map.of("label", "Several days", "score", 1),
                Map.of("label", "More than half the days", "score", 2),
                Map.of("label", "Nearly every day", "score", 3)
        );

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() > 5 && !trimmed.toLowerCase().startsWith("title") && !trimmed.toLowerCase().startsWith("instructions")) {
                questions.add(Map.of(
                        "id", count,
                        "question", trimmed.replaceFirst("^[0-9]+[.\\)]\\s*", ""),
                        "options", defaultOptions
                ));
                count++;
                if (count > 15) break;
            }
        }

        if (questions.isEmpty()) {
            questions.add(Map.of("id", 1, "question", "Feeling nervous, anxious, or overwhelmed by daily commitments", "options", defaultOptions));
            questions.add(Map.of("id", 2, "question", "Trouble winding down or experiencing restful sleep", "options", defaultOptions));
            questions.add(Map.of("id", 3, "question", "Feeling low energy or reduced motivation for routine tasks", "options", defaultOptions));
        }

        map.put("questions", questions);

        int max = questions.size() * 3;
        List<Map<String, Object>> rules = List.of(
                Map.of("minScore", 0, "maxScore", max / 4, "severity", "Low", "interpretation", "Symptoms within mild, manageable range."),
                Map.of("minScore", (max / 4) + 1, "maxScore", max / 2, "severity", "Mild", "interpretation", "Mild vulnerability observed."),
                Map.of("minScore", (max / 2) + 1, "maxScore", (max * 3) / 4, "severity", "Moderate", "interpretation", "Moderate clinical impact; consultation advised."),
                Map.of("minScore", ((max * 3) / 4) + 1, "maxScore", max, "severity", "High", "interpretation", "Elevated distress. Direct therapeutic discussion recommended.")
        );
        map.put("scoringRules", rules);

        return map;
    }
}
