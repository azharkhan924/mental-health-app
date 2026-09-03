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
import java.util.Map;
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
    private final com.mental_health_app.mental_health.service.CustomTestService customTestService;
    private final com.mental_health_app.mental_health.service.PreAppointmentService preAppointmentService;
    private final com.mental_health_app.mental_health.repository.AppointmentNotesRepository appointmentNotesRepository;

    public DashboardController(PatientService patientService,
                               TherapistService therapistService,
                               AppointmentService appointmentService,
                               AssessmentService assessmentService,
                               com.mental_health_app.mental_health.service.ReportService reportService,
                               com.mental_health_app.mental_health.service.CustomTestService customTestService,
                               com.mental_health_app.mental_health.service.PreAppointmentService preAppointmentService,
                               com.mental_health_app.mental_health.repository.AppointmentNotesRepository appointmentNotesRepository) {
        this.patientService = patientService;
        this.therapistService = therapistService;
        this.appointmentService = appointmentService;
        this.assessmentService = assessmentService;
        this.reportService = reportService;
        this.customTestService = customTestService;
        this.preAppointmentService = preAppointmentService;
        this.appointmentNotesRepository = appointmentNotesRepository;
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

            // Fetch custom tests assigned by therapists
            List<com.mental_health_app.mental_health.entity.CustomTestAssignment> assignedTests = 
                    customTestService.getAssignmentsForPatient(patient);
            model.addAttribute("assignedTests", assignedTests);
            model.addAttribute("pendingAssignedTestsCount", 
                    assignedTests.stream().filter(a -> "PENDING".equals(a.getStatus())).count());

            // Fetch pre-appointment diagnostic tasks
            var preTasks = preAppointmentService.getPendingTasksForPatient(patient);
            model.addAttribute("pendingPreTasks", preTasks);
            model.addAttribute("pendingPreTasksCount", preTasks.size());

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

            // Schedule & Availabilities (all 7 days)
            model.addAttribute("availabilities", appointmentService.getOrCreateTherapistAvailabilities(therapist));

            // Custom Tests & Patient Assignments
            model.addAttribute("customTests", customTestService.getTestsByTherapist(therapist));
            model.addAttribute("testAssignments", customTestService.getAssignmentsByTherapist(therapist));

            // Available patients for test assignment
            model.addAttribute("allPatients", patientService.getAllPatients());

            // Pre-appointment diagnostic tasks
            model.addAttribute("preDiagnosisTasks", preAppointmentService.getTasksForTherapist(therapist));

            // Session Notes & Prescriptions mapped by appointment ID
            List<com.mental_health_app.mental_health.entity.AppointmentNotes> therapistNotes =
                    appointmentNotesRepository.findByTherapistOrderByCreatedAtDesc(therapist);
            Map<Long, com.mental_health_app.mental_health.entity.AppointmentNotes> notesMap = new java.util.HashMap<>();
            for (com.mental_health_app.mental_health.entity.AppointmentNotes note : therapistNotes) {
                if (note.getAppointment() != null) {
                    notesMap.put(note.getAppointment().getId(), note);
                }
            }
            model.addAttribute("notesMap", notesMap);
        }

        return "therapist-dashboard";
    }
}
