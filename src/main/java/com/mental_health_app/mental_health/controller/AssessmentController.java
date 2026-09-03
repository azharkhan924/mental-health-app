package com.mental_health_app.mental_health.controller;

import com.mental_health_app.mental_health.entity.Assessment;
import com.mental_health_app.mental_health.entity.AssessmentType;
import com.mental_health_app.mental_health.entity.Patient;
import com.mental_health_app.mental_health.entity.PatientReport;
import com.mental_health_app.mental_health.service.AssessmentService;
import com.mental_health_app.mental_health.service.CustomTestService;
import com.mental_health_app.mental_health.service.PatientService;
import com.mental_health_app.mental_health.service.ReportService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ASSESSMENT CONTROLLER
 * ─────────────────────
 * Handles assessment selection, dynamic questionnaire rendering for 5 test types,
 * scoring calculations, and AI clinical report generation.
 */
@Controller
@RequestMapping("/assessments")
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final PatientService patientService;
    private final ReportService reportService;
    private final CustomTestService customTestService;

    public AssessmentController(AssessmentService assessmentService,
                                PatientService patientService,
                                ReportService reportService,
                                CustomTestService customTestService) {
        this.assessmentService = assessmentService;
        this.patientService = patientService;
        this.reportService = reportService;
        this.customTestService = customTestService;
    }

    /**
     * Assessments Landing Page — Displays all 5 assessment cards, custom therapist tests, and past history.
     */
    @GetMapping
    public String showAssessmentsOverview(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Optional<Patient> patientOpt = patientService.findByEmail(userDetails.getUsername());

        if (patientOpt.isPresent()) {
            Patient patient = patientOpt.get();
            model.addAttribute("userName", patient.getName());
            model.addAttribute("assessments", assessmentService.getPatientAssessments(patient));
            model.addAttribute("reports", reportService.getReportsForPatient(patient));
            model.addAttribute("assignedTests", customTestService.getAssignmentsForPatient(patient));
        }

        return "assessments";
    }

    /**
     * Render Questionnaire Form dynamically for any AssessmentType.
     */
    @GetMapping("/take/{type}")
    public String takeAssessment(@PathVariable AssessmentType type,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 Model model) {
        Optional<Patient> patientOpt = patientService.findByEmail(userDetails.getUsername());
        patientOpt.ifPresent(p -> model.addAttribute("userName", p.getName()));

        model.addAttribute("assessmentType", type);
        model.addAttribute("questions", assessmentService.getQuestionsForType(type));
        model.addAttribute("title", type.getDisplayName());

        String description = switch (type) {
            case PHQ9 -> "Over the last 2 weeks, how often have you been bothered by any of the following problems?";
            case GAD7 -> "Over the last 2 weeks, how often have you experienced the following anxiety symptoms?";
            case PSS10 -> "In the last month, how often have you felt or experienced each of the following situations?";
            case ISI -> "Please rate the severity and impact of your sleep difficulties over the past month.";
            case BURNOUT -> "Reflect on your recent work and academic routine and rate how frequently you feel these symptoms.";
        };
        model.addAttribute("description", description);

        return "assessment-quiz";
    }

    /**
     * Submit assessment answers, compute score, generate AI clinical report, and redirect to the result page.
     */
    @PostMapping("/submit")
    public String submitAssessment(@RequestParam AssessmentType type,
                                   @RequestParam Map<String, String> allParams,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        Optional<Patient> patientOpt = patientService.findByEmail(userDetails.getUsername());

        if (patientOpt.isEmpty()) {
            return "redirect:/auth?mode=login";
        }

        int questionCount = type.getQuestionCount();
        List<Integer> answers = new ArrayList<>();

        for (int i = 0; i < questionCount; i++) {
            String val = allParams.get("q" + i);
            int score = (val != null) ? Integer.parseInt(val) : 0;
            answers.add(score);
        }

        Assessment saved = assessmentService.evaluateAndSave(patientOpt.get(), type, answers);

        // Generate and persist AI Clinical Report for this assessment
        try {
            reportService.generateAssessmentReport(saved);
        } catch (Exception e) {
            // Log and continue gracefully
        }

        return "redirect:/assessments/result/" + saved.getId();
    }

    /**
     * View detailed assessment score evaluation, AI clinical analysis, and suggestions.
     */
    @GetMapping("/result/{id}")
    public String viewResult(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model) {
        Optional<Patient> patientOpt = patientService.findByEmail(userDetails.getUsername());
        patientOpt.ifPresent(p -> model.addAttribute("userName", p.getName()));

        Optional<Assessment> assessmentOpt = assessmentService.getById(id);

        if (assessmentOpt.isPresent()) {
            Assessment assessment = assessmentOpt.get();
            model.addAttribute("assessment", assessment);

            int maxScore = assessment.getType().getMaxScore();
            int percentage = (int) Math.round(((double) assessment.getTotalScore() / maxScore) * 100);
            model.addAttribute("scorePercentage", percentage);

            // Fetch AI Clinical Report
            Optional<PatientReport> reportOpt = reportService.getReportForAssessment(assessment);
            reportOpt.ifPresent(r -> model.addAttribute("aiReport", r));

            return "assessment-result";
        }

        return "redirect:/assessments";
    }
}

