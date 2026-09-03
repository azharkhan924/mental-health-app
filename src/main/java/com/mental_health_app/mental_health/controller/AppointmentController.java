package com.mental_health_app.mental_health.controller;

import com.mental_health_app.mental_health.entity.AppointmentStatus;
import com.mental_health_app.mental_health.entity.Patient;
import com.mental_health_app.mental_health.service.AppointmentService;
import com.mental_health_app.mental_health.service.PatientService;
import com.mental_health_app.mental_health.service.TherapistService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

/**
 * APPOINTMENT CONTROLLER
 * ──────────────────────
 * Handles HTTP requests related to booking and updating appointments.
 */
@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final TherapistService therapistService;
    private final PatientService patientService;
    private final com.mental_health_app.mental_health.service.ReportService reportService;

    public AppointmentController(AppointmentService appointmentService, 
                                 TherapistService therapistService, 
                                 PatientService patientService,
                                 com.mental_health_app.mental_health.service.ReportService reportService) {
        this.appointmentService = appointmentService;
        this.therapistService = therapistService;
        this.patientService = patientService;
        this.reportService = reportService;
    }

    /**
     * Show the appointment booking form.
     */
    @GetMapping("/book")
    public String showBookingForm(@RequestParam(required = false) Long therapistId,
                                  Model model, 
                                  @AuthenticationPrincipal UserDetails userDetails) {
        Optional<Patient> patientOpt = patientService.findByEmail(userDetails.getUsername());
        patientOpt.ifPresent(p -> model.addAttribute("userName", p.getName()));

        model.addAttribute("therapists", therapistService.getAllTherapists());
        model.addAttribute("selectedTherapistId", therapistId);
        model.addAttribute("minDate", LocalDate.now());
        return "book-appointment";
    }

    /**
     * AJAX endpoint: Fetch real-time available time slots for a specific therapist on a selected date.
     */
    @GetMapping("/available-slots")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> getAvailableSlots(
            @RequestParam Long therapistId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        try {
            return org.springframework.http.ResponseEntity.ok(appointmentService.getAvailableSlots(therapistId, date));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /**
     * Submit an appointment booking request.
     */
    @PostMapping("/book")
    public String bookAppointment(@RequestParam Long therapistId,
                                   @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate appointmentDate,
                                   @RequestParam String appointmentTime,
                                   @RequestParam(required = false) String notes,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   Model model) {
        try {
            Optional<Patient> patientOpt = patientService.findByEmail(userDetails.getUsername());

            if (patientOpt.isEmpty()) {
                return "redirect:/auth?mode=login";
            }

            Patient patient = patientOpt.get();

            // Save the appointment
            appointmentService.bookAppointment(patient, therapistId, appointmentDate, appointmentTime, notes);

            // Generate an initial or refreshed behavioral report for the therapist
            try {
                reportService.generateChatBehavioralReport(patient);
            } catch (Exception ignored) {
            }

            // Redirect back to patient dashboard with a success message
            return "redirect:/dashboard?booked=true";

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("therapists", therapistService.getAllTherapists());
            return "book-appointment";
        }
    }

    /**
     * Update appointment status (Accept or Cancel).
     * Accessible by Therapist to Confirm/Cancel, or by Patient to Cancel.
     */
    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam AppointmentStatus status,
                               @RequestParam(defaultValue = "dashboard") String returnTo) {
        appointmentService.updateStatus(id, status);

        if ("therapist".equals(returnTo)) {
            return "redirect:/therapist/dashboard";
        }
        return "redirect:/dashboard";
    }
}
