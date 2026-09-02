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

    public AppointmentController(AppointmentService appointmentService,
                                 TherapistService therapistService,
                                 PatientService patientService) {
        this.appointmentService = appointmentService;
        this.therapistService = therapistService;
        this.patientService = patientService;
    }

    /**
     * Show appointment booking page with the list of available therapists.
     */
    @GetMapping("/book")
    public String showBookingPage(@RequestParam(required = false) Long therapistId,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model) {
        // Add all therapists to the model so patient can select one
        model.addAttribute("therapists", therapistService.getAllTherapists());
        model.addAttribute("selectedTherapistId", therapistId);

        // Fetch patient name for greeting in header
        Optional<Patient> patient = patientService.findByEmail(userDetails.getUsername());
        patient.ifPresent(p -> model.addAttribute("userName", p.getName()));

        // Set minimum date for booking as today
        model.addAttribute("minDate", LocalDate.now());

        return "book-appointment";
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

            // Save the appointment
            appointmentService.bookAppointment(patientOpt.get(), therapistId, appointmentDate, appointmentTime, notes);

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
