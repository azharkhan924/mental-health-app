package com.mental_health_app.mental_health.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mental_health_app.mental_health.entity.CustomTest;
import com.mental_health_app.mental_health.entity.CustomTestAssignment;
import com.mental_health_app.mental_health.entity.Patient;
import com.mental_health_app.mental_health.entity.Therapist;
import com.mental_health_app.mental_health.repository.TherapistRepository;
import com.mental_health_app.mental_health.service.AppointmentService;
import com.mental_health_app.mental_health.service.CustomTestService;
import com.mental_health_app.mental_health.service.PatientService;
import com.mental_health_app.mental_health.service.TherapistService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.util.*;

/**
 * CONTROLLER: CUSTOM TESTS, SCHEDULING & THERAPIST PROFILE MANAGEMENT
 * ───────────────────────────────────────────────────────────────────
 * Manages document uploads for custom test creation, multi-patient assignment,
 * dynamic quiz taking, availability schedules, and therapist clinic profiles.
 */
@Controller
public class CustomTestController {

    private final CustomTestService customTestService;
    private final TherapistService therapistService;
    private final TherapistRepository therapistRepository;
    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CustomTestController(CustomTestService customTestService,
                                TherapistService therapistService,
                                TherapistRepository therapistRepository,
                                PatientService patientService,
                                AppointmentService appointmentService) {
        this.customTestService = customTestService;
        this.therapistService = therapistService;
        this.therapistRepository = therapistRepository;
        this.patientService = patientService;
        this.appointmentService = appointmentService;
    }

    // ─────────────────────────────────────────────────────────────
    // THERAPIST: FILE UPLOAD & AI TEST GENERATION
    // ─────────────────────────────────────────────────────────────

    /**
     * AJAX endpoint: Upload a file (PDF, DOCX, TXT, CSV) or paste raw questions,
     * and receive formatted JSON with psychometric questions and scoring rules from AI.
     */
    @PostMapping(value = "/therapist/tests/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> generateTestFromDocument(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "rawText", required = false) String rawText,
            @RequestParam(value = "title", required = false) String title,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            String extractedContent = "";
            if (file != null && !file.isEmpty()) {
                extractedContent = customTestService.extractTextFromFile(file);
            }
            if ((extractedContent == null || extractedContent.isBlank()) && rawText != null && !rawText.isBlank()) {
                extractedContent = rawText;
            }

            if (extractedContent == null || extractedContent.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Please upload a valid document or enter question text."));
            }

            Map<String, Object> parsedTest = customTestService.parseQuestionsWithAI(extractedContent, title);
            return ResponseEntity.ok(parsedTest);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to parse questions: " + e.getMessage()));
        }
    }

    /**
     * Save a generated test template as a reusable custom test on the therapist's profile.
     */
    @PostMapping("/therapist/tests/save")
    public String saveCustomTest(@RequestParam String title,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(required = false, defaultValue = "General Wellbeing") String category,
                                 @RequestParam String questionsJson,
                                 @RequestParam(required = false) String scoringRulesJson,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {

        Optional<Therapist> therapistOpt = therapistService.findByEmail(userDetails.getUsername());
        if (therapistOpt.isEmpty()) {
            return "redirect:/auth?mode=login";
        }

        try {
            customTestService.saveCustomTest(
                    therapistOpt.get(),
                    title.trim(),
                    description != null ? description.trim() : "",
                    category.trim(),
                    questionsJson,
                    scoringRulesJson
            );
            redirectAttributes.addFlashAttribute("success", "Custom assessment template '" + title + "' saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not save test template: " + e.getMessage());
        }

        return "redirect:/therapist/dashboard#tests-section";
    }

    /**
     * Assign a custom test to multiple patients.
     */
    @PostMapping("/therapist/tests/assign")
    public String assignTestToPatients(@RequestParam Long testId,
                                       @RequestParam List<Long> patientIds,
                                       @AuthenticationPrincipal UserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {

        Optional<Therapist> therapistOpt = therapistService.findByEmail(userDetails.getUsername());
        if (therapistOpt.isEmpty()) {
            return "redirect:/auth?mode=login";
        }

        try {
            List<CustomTestAssignment> assignments = customTestService.assignTestToPatients(testId, therapistOpt.get(), patientIds);
            redirectAttributes.addFlashAttribute("success", "Successfully assigned test to " + assignments.size() + " patient(s)!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Assignment failed: " + e.getMessage());
        }

        return "redirect:/therapist/dashboard#tests-section";
    }

    // ─────────────────────────────────────────────────────────────
    // THERAPIST: SCHEDULE AVAILABILITY CONFIGURATION
    // ─────────────────────────────────────────────────────────────

    /**
     * Update working hours & time slots for a specific day of the week.
     */
    @PostMapping("/therapist/schedule/save")
    public String updateSchedule(@RequestParam DayOfWeek dayOfWeek,
                                 @RequestParam(defaultValue = "false") boolean active,
                                 @RequestParam String timeSlots,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {

        Optional<Therapist> therapistOpt = therapistService.findByEmail(userDetails.getUsername());
        if (therapistOpt.isEmpty()) {
            return "redirect:/auth?mode=login";
        }

        try {
            appointmentService.updateDayAvailability(therapistOpt.get(), dayOfWeek, active, timeSlots);
            redirectAttributes.addFlashAttribute("success", "Schedule updated for " + dayOfWeek + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update schedule: " + e.getMessage());
        }

        return "redirect:/therapist/dashboard#schedule-section";
    }

    // ─────────────────────────────────────────────────────────────
    // THERAPIST: EDIT PROFILE & CLINIC DETAILS
    // ─────────────────────────────────────────────────────────────

    /**
     * Update therapist clinic address, city, qualifications, languages, fee, and contact.
     */
    @PostMapping("/therapist/profile/update")
    public String updateProfile(@RequestParam(required = false) String clinicAddress,
                                @RequestParam(required = false) String city,
                                @RequestParam(required = false) String qualifications,
                                @RequestParam(required = false) String languages,
                                @RequestParam(required = false) String consultationFee,
                                @RequestParam(required = false) String availableDays,
                                @RequestParam(required = false) String availableHours,
                                @RequestParam(required = false) String bio,
                                @RequestParam(required = false) String phoneNumber,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {

        Optional<Therapist> therapistOpt = therapistService.findByEmail(userDetails.getUsername());
        if (therapistOpt.isEmpty()) {
            return "redirect:/auth?mode=login";
        }

        Therapist therapist = therapistOpt.get();
        if (clinicAddress != null) therapist.setClinicAddress(clinicAddress.trim());
        if (city != null) therapist.setCity(city.trim());
        if (qualifications != null) therapist.setQualifications(qualifications.trim());
        if (languages != null) therapist.setLanguages(languages.trim());
        if (consultationFee != null) therapist.setConsultationFee(consultationFee.trim());
        if (availableDays != null) therapist.setAvailableDays(availableDays.trim());
        if (availableHours != null) therapist.setAvailableHours(availableHours.trim());
        if (bio != null) therapist.setBio(bio.trim());
        if (phoneNumber != null) therapist.setPhoneNumber(phoneNumber.trim());

        therapistRepository.save(therapist);
        redirectAttributes.addFlashAttribute("success", "Profile and clinic information updated successfully!");

        return "redirect:/therapist/dashboard#profile-section";
    }

    // ─────────────────────────────────────────────────────────────
    // PATIENT: TAKE ASSIGNED CUSTOM TEST & VIEW RESULTS
    // ─────────────────────────────────────────────────────────────

    /**
     * Render the questionnaire for an assigned custom test.
     */
    @GetMapping("/assessments/custom/{assignmentId}")
    public String takeCustomTest(@PathVariable Long assignmentId,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

        Optional<Patient> patientOpt = patientService.findByEmail(userDetails.getUsername());
        if (patientOpt.isEmpty()) {
            return "redirect:/auth?mode=login";
        }

        Optional<CustomTestAssignment> assignOpt = customTestService.getAssignmentById(assignmentId);
        if (assignOpt.isEmpty() || !assignOpt.get().getPatient().getId().equals(patientOpt.get().getId())) {
            redirectAttributes.addFlashAttribute("error", "Assessment assignment not found.");
            return "redirect:/assessments";
        }

        CustomTestAssignment assignment = assignOpt.get();
        if ("COMPLETED".equals(assignment.getStatus())) {
            return "redirect:/assessments/custom/" + assignmentId + "/result";
        }

        CustomTest test = assignment.getCustomTest();
        List<Map<String, Object>> questions = Collections.emptyList();
        try {
            questions = objectMapper.readValue(test.getQuestionsJson(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ignored) {
        }

        model.addAttribute("userName", patientOpt.get().getName());
        model.addAttribute("assignment", assignment);
        model.addAttribute("test", test);
        model.addAttribute("questions", questions);

        return "custom-test-quiz";
    }

    /**
     * Submit responses for custom test, calculate score & severity, generate AI evaluation.
     */
    @PostMapping("/assessments/custom/{assignmentId}/submit")
    public String submitCustomTest(@PathVariable Long assignmentId,
                                   @RequestParam Map<String, String> answers,
                                   @AuthenticationPrincipal UserDetails userDetails) {

        Optional<Patient> patientOpt = patientService.findByEmail(userDetails.getUsername());
        if (patientOpt.isEmpty()) {
            return "redirect:/auth?mode=login";
        }

        customTestService.evaluateAndCompleteAssignment(assignmentId, answers);
        return "redirect:/assessments/custom/" + assignmentId + "/result";
    }

    /**
     * View completed results and AI clinical evaluation for custom test.
     */
    @GetMapping("/assessments/custom/{assignmentId}/result")
    public String viewCustomTestResult(@PathVariable Long assignmentId,
                                       @AuthenticationPrincipal UserDetails userDetails,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {

        Optional<CustomTestAssignment> assignOpt = customTestService.getAssignmentById(assignmentId);
        if (assignOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Result not found.");
            return "redirect:/dashboard";
        }

        CustomTestAssignment assignment = assignOpt.get();

        int maxScore = assignment.getCustomTest().getMaxScore();
        int score = assignment.getTotalScore() != null ? assignment.getTotalScore() : 0;
        int percentage = (maxScore > 0) ? (int) Math.round(((double) score / maxScore) * 100) : 0;

        model.addAttribute("assignment", assignment);
        model.addAttribute("test", assignment.getCustomTest());
        model.addAttribute("scorePercentage", percentage);

        return "custom-test-result";
    }
}
