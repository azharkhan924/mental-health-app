package com.mental_health_app.mental_health.controller;

import com.mental_health_app.mental_health.entity.*;
import com.mental_health_app.mental_health.repository.AppointmentNotesRepository;
import com.mental_health_app.mental_health.service.*;
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
 * Serves the Comprehensive Report Hub to authorized therapists within the 3-month
 * appointment access window. Aggregates all report types: AI Chat Behavioral,
 * Clinical Assessments, Custom Tests, Pre-Diagnosis AI, and Appointment Notes.
 */
@Controller
public class ReportController {

    private final ReportService reportService;
    private final TherapistService therapistService;
    private final PatientService patientService;
    private final CustomTestService customTestService;
    private final PreAppointmentService preAppointmentService;
    private final AppointmentNotesRepository appointmentNotesRepository;

    public ReportController(ReportService reportService,
                            TherapistService therapistService,
                            PatientService patientService,
                            CustomTestService customTestService,
                            PreAppointmentService preAppointmentService,
                            AppointmentNotesRepository appointmentNotesRepository) {
        this.reportService = reportService;
        this.therapistService = therapistService;
        this.patientService = patientService;
        this.customTestService = customTestService;
        this.preAppointmentService = preAppointmentService;
        this.appointmentNotesRepository = appointmentNotesRepository;
    }

    /**
     * Comprehensive Report Hub: All patient reports aggregated in 5 sections.
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

        // SECTION 1: AI Chat Behavioral Reports
        List<PatientReport> allReports = reportService.getReportsForTherapist(therapist, patient);

        List<PatientReport> behavioralReports = allReports.stream()
                .filter(r -> r.getReportType() == ReportType.CHAT_BEHAVIORAL)
                .toList();

        // SECTION 2: Clinical Assessment Reports (PHQ-9, GAD-7 etc.)
        List<PatientReport> assessmentReports = allReports.stream()
                .filter(r -> r.getReportType() == ReportType.ASSESSMENT)
                .toList();

        // SECTION 3: Custom Test Results
        List<CustomTestAssignment> customTestResults = customTestService.getAssignmentsForPatient(patient)
                .stream().filter(a -> "COMPLETED".equals(a.getStatus())).toList();

        // SECTION 4: Pre-Diagnosis AI Conversations
        List<PreAppointmentTask> preDiagnosisTasks = preAppointmentService.getTasksForPatient(patient)
                .stream().filter(t -> "COMPLETED".equals(t.getStatus())).toList();

        // SECTION 5: Appointment Notes & Prescriptions
        List<AppointmentNotes> appointmentNotes = appointmentNotesRepository.findByPatientOrderByCreatedAtDesc(patient);

        model.addAttribute("therapist", therapist);
        model.addAttribute("userName", therapist.getName());
        model.addAttribute("patient", patient);
        model.addAttribute("allReports", allReports);
        model.addAttribute("assessmentReports", assessmentReports);
        model.addAttribute("behavioralReports", behavioralReports);
        model.addAttribute("customTestResults", customTestResults);
        model.addAttribute("preDiagnosisTasks", preDiagnosisTasks);
        model.addAttribute("appointmentNotes", appointmentNotes);
        model.addAttribute("accessExpiryDate", formattedExpiry);

        // Section counts for tab badges
        model.addAttribute("totalSections",
                (behavioralReports.isEmpty() ? 0 : 1) +
                (assessmentReports.isEmpty() ? 0 : 1) +
                (customTestResults.isEmpty() ? 0 : 1) +
                (preDiagnosisTasks.isEmpty() ? 0 : 1) +
                (appointmentNotes.isEmpty() ? 0 : 1));

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

    /**
     * Patient View: Comprehensive Personal Health Report Hub.
     * Includes all clinical assessments, custom tests, pre-diagnosis summaries,
     * and session prescriptions & follow-up recommendations from therapists.
     */
    @GetMapping("/reports")
    public String viewPatientOwnReports(@AuthenticationPrincipal UserDetails userDetails,
                                        Model model) {

        Optional<Patient> patientOpt = patientService.findByEmail(userDetails.getUsername());
        if (patientOpt.isEmpty()) {
            return "redirect:/auth?mode=login";
        }

        Patient patient = patientOpt.get();

        // 1. Validated Clinical Assessments
        List<PatientReport> assessmentReports = reportService.getReportsForPatient(patient);

        // 2. Custom Tests Completed
        List<CustomTestAssignment> customTestResults = customTestService.getAssignmentsForPatient(patient)
                .stream().filter(a -> "COMPLETED".equals(a.getStatus())).toList();

        // 3. Pre-Diagnosis Screenings Completed
        List<PreAppointmentTask> preDiagnosisTasks = preAppointmentService.getTasksForPatient(patient)
                .stream().filter(t -> "COMPLETED".equals(t.getStatus())).toList();

        // 4. Appointment Prescriptions & Session Plans
        List<AppointmentNotes> appointmentNotes = appointmentNotesRepository.findByPatientOrderByCreatedAtDesc(patient);

        model.addAttribute("patient", patient);
        model.addAttribute("userName", patient.getName());
        model.addAttribute("assessmentReports", assessmentReports);
        model.addAttribute("customTestResults", customTestResults);
        model.addAttribute("preDiagnosisTasks", preDiagnosisTasks);
        model.addAttribute("appointmentNotes", appointmentNotes);

        return "patient-view-reports";
    }
}
