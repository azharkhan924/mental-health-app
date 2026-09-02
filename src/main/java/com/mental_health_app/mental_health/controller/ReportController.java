package com.mental_health_app.mental_health.controller;

import com.mental_health_app.mental_health.entity.Patient;
import com.mental_health_app.mental_health.entity.PatientReport;
import com.mental_health_app.mental_health.entity.ReportType;
import com.mental_health_app.mental_health.entity.Therapist;
import com.mental_health_app.mental_health.service.PatientService;
import com.mental_health_app.mental_health.service.ReportService;
import com.mental_health_app.mental_health.service.TherapistService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * REPORT CONTROLLER
 * ─────────────────
 * Serves clinical reports to authorized therapists within the 3-month appointment access window,
 * and allows patients to view their own AI-generated test assessment reports.
 */
@Controller
public class ReportController {

    private final ReportService reportService;
    private final TherapistService therapistService;
    private final PatientService patientService;

    public ReportController(ReportService reportService,
                            TherapistService therapistService,
                            PatientService patientService) {
        this.reportService = reportService;
        this.therapistService = therapistService;
        this.patientService = patientService;
    }

    /**
     * Therapist Clinical View: Complete patient reports & hidden chat behavioral analysis.
     * Access Window: from appointment booking date to 3 months post-appointment.
     */
    @GetMapping("/therapist/patient/{patientId}/reports")
    public String viewPatientReportsForTherapist(@PathVariable Long patientId,
                                                @AuthenticationPrincipal UserDetails userDetails,
                                                Model model,
                                                RedirectAttributes redirectAttributes) {

        Optional<Therapist> therapistOpt = therapistService.findByEmail(userDetails.getUsername());
        if (therapistOpt.isEmpty()) {
            return "redirect:/auth?mode=login";
        }

        Therapist therapist = therapistOpt.get();
        Optional<Patient> patientOpt = patientService.findById(patientId);

        if (patientOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Patient not found.");
            return "redirect:/therapist/dashboard";
        }

        Patient patient = patientOpt.get();

        // Verify 3-month access window authorization
        boolean authorized = reportService.isTherapistAuthorized(therapist, patient);
        if (!authorized) {
            redirectAttributes.addFlashAttribute("error",
                    "Access window closed or no valid appointment exists for patient " + patient.getName() + ".");
            return "redirect:/therapist/dashboard";
        }

        LocalDate expiryDate = reportService.getAccessExpiryDate(therapist, patient);
        String formattedExpiry = (expiryDate != null)
                ? expiryDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                : "Active Consultation Window";

        List<PatientReport> allReports = reportService.getReportsForTherapist(therapist, patient);

        List<PatientReport> assessmentReports = allReports.stream()
                .filter(r -> r.getReportType() == ReportType.ASSESSMENT)
                .toList();

        List<PatientReport> behavioralReports = allReports.stream()
                .filter(r -> r.getReportType() == ReportType.CHAT_BEHAVIORAL)
                .toList();

        model.addAttribute("therapist", therapist);
        model.addAttribute("userName", therapist.getName());
        model.addAttribute("patient", patient);
        model.addAttribute("allReports", allReports);
        model.addAttribute("assessmentReports", assessmentReports);
        model.addAttribute("behavioralReports", behavioralReports);
        model.addAttribute("accessExpiryDate", formattedExpiry);

        return "patient-reports";
    }

    /**
     * Therapist action: Trigger an instant fresh AI Chat Behavioral Report analysis for a patient.
     */
    @PostMapping("/therapist/patient/{patientId}/generate-chat-report")
    public String generateFreshChatReport(@PathVariable Long patientId,
                                          @AuthenticationPrincipal UserDetails userDetails,
                                          RedirectAttributes redirectAttributes) {

        Optional<Therapist> therapistOpt = therapistService.findByEmail(userDetails.getUsername());
        if (therapistOpt.isEmpty()) {
            return "redirect:/auth?mode=login";
        }

        Therapist therapist = therapistOpt.get();
        Optional<Patient> patientOpt = patientService.findById(patientId);

        if (patientOpt.isPresent() && reportService.isTherapistAuthorized(therapist, patientOpt.get())) {
            PatientReport generated = reportService.generateChatBehavioralReport(patientOpt.get());
            if (generated != null) {
                redirectAttributes.addFlashAttribute("success", "Fresh AI Behavioral Analysis generated successfully.");
            } else {
                redirectAttributes.addFlashAttribute("info", "No new chat messages found to analyze.");
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Unauthorized or patient not found.");
        }

        return "redirect:/therapist/patient/" + patientId + "/reports";
    }
}
