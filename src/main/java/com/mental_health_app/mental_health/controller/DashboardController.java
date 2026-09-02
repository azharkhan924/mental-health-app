package com.mental_health_app.mental_health.controller;

import com.mental_health_app.mental_health.entity.Appointment;
import com.mental_health_app.mental_health.entity.AppointmentStatus;
import com.mental_health_app.mental_health.entity.Assessment;
import com.mental_health_app.mental_health.entity.Patient;
import com.mental_health_app.mental_health.entity.Therapist;
import com.mental_health_app.mental_health.service.AppointmentService;
import com.mental_health_app.mental_health.service.AssessmentService;
import com.mental_health_app.mental_health.service.PatientService;
import com.mental_health_app.mental_health.service.TherapistService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

/**
 * DASHBOARD CONTROLLER
 * ────────────────────
 * Serves role-specific dashboards with real-time appointment records, assessments, and statistics.
 */
@Controller
public class DashboardController {

    private final PatientService patientService;
    private final TherapistService therapistService;
    private final AppointmentService appointmentService;
    private final AssessmentService assessmentService;
    private final com.mental_health_app.mental_health.service.ReportService reportService;

    public DashboardController(PatientService patientService,
                               TherapistService therapistService,
                               AppointmentService appointmentService,
                               AssessmentService assessmentService,
                               com.mental_health_app.mental_health.service.ReportService reportService) {
        this.patientService = patientService;
        this.therapistService = therapistService;
        this.appointmentService = appointmentService;
        this.assessmentService = assessmentService;
        this.reportService = reportService;
    }

    /**
     * Patient Dashboard
     */
    @GetMapping("/dashboard")
    public String patientDashboard(@RequestParam(required = false) String booked,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   Model model) {

        String email = userDetails.getUsername();
        Optional<Patient> patientOpt = patientService.findByEmail(email);

        if (patientOpt.isPresent()) {
            Patient patient = patientOpt.get();
            model.addAttribute("user", patient);
            model.addAttribute("userName", patient.getName());

            // Fetch patient's appointments
            List<Appointment> appointments = appointmentService.getAppointmentsForPatient(patient);
            model.addAttribute("appointments", appointments);

            // Fetch patient's past assessments
            List<Assessment> assessments = assessmentService.getPatientAssessments(patient);
            model.addAttribute("assessments", assessments);

            // Fetch patient's AI assessment reports
            model.addAttribute("reports", reportService.getReportsForPatient(patient));

            // Statistics
            model.addAttribute("pendingCount", appointmentService.countByPatientAndStatus(patient, AppointmentStatus.PENDING));
            model.addAttribute("confirmedCount", appointmentService.countByPatientAndStatus(patient, AppointmentStatus.CONFIRMED));
            model.addAttribute("assessmentsCount", assessmentService.countByPatient(patient));
        }

        if ("true".equals(booked)) {
            model.addAttribute("success", "Your appointment request has been submitted successfully!");
        }

        return "dashboard";
    }

    /**
     * Therapist Dashboard
     */
    @GetMapping("/therapist/dashboard")
    public String therapistDashboard(@AuthenticationPrincipal UserDetails userDetails,
                                     Model model) {

        String email = userDetails.getUsername();
        Optional<Therapist> therapistOpt = therapistService.findByEmail(email);

        if (therapistOpt.isPresent()) {
            Therapist therapist = therapistOpt.get();
            model.addAttribute("user", therapist);
            model.addAttribute("userName", therapist.getName());

            // Fetch therapist's appointments
            List<Appointment> appointments = appointmentService.getAppointmentsForTherapist(therapist);
            model.addAttribute("appointments", appointments);

            // Live statistics
            long pendingCount = appointmentService.countByTherapistAndStatus(therapist, AppointmentStatus.PENDING);
            long confirmedCount = appointmentService.countByTherapistAndStatus(therapist, AppointmentStatus.CONFIRMED);

            // Approximate unique active patients from appointments
            long uniquePatients = appointments.stream()
                    .map(a -> a.getPatient().getId())
                    .distinct()
                    .count();

            model.addAttribute("activePatientsCount", uniquePatients);
            model.addAttribute("upcomingSessionsCount", confirmedCount);
            model.addAttribute("pendingRequestsCount", pendingCount);
        }

        return "therapist-dashboard";
    }
}
