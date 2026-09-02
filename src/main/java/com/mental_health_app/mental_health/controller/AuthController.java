package com.mental_health_app.mental_health.controller;

import com.mental_health_app.mental_health.entity.Patient;
import com.mental_health_app.mental_health.entity.Therapist;
import com.mental_health_app.mental_health.service.PatientService;
import com.mental_health_app.mental_health.service.TherapistService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * AUTH CONTROLLER
 * ───────────────
 * Handles:
 *   1. Showing the login/register page (GET /auth)
 *   2. Registering new patients (POST /auth/patient/register)
 *   3. Registering new therapists (POST /auth/therapist/register)
 *
 * NOTE: Login POST is handled by Spring Security automatically.
 *       We only handle REGISTRATION here.
 *       See SecurityConfig.java for login configuration.
 */
@Controller
@RequestMapping("/auth")
public class AuthController {

    private final PatientService patientService;
    private final TherapistService therapistService;

    public AuthController(PatientService patientService, TherapistService therapistService) {
        this.patientService = patientService;
        this.therapistService = therapistService;
    }

    /**
     * Show the auth page.
     *
     * URL examples:
     *   /auth                         → shows register form for patient
     *   /auth?mode=login              → shows login form
     *   /auth?mode=register&role=therapist → shows register form for therapist
     *   /auth?mode=login&error=true   → shows login form with error message
     */
    @GetMapping
    public String showAuthPage(@RequestParam(defaultValue = "register") String mode,
                               @RequestParam(defaultValue = "patient") String role,
                               @RequestParam(required = false) String error,
                               Model model) {

        // Spring Security sends ?error=true when login fails
        if (error != null) {
            model.addAttribute("error", "Invalid email or password");
        }

        return renderAuth(model, mode, role);
    }

    /**
     * Register a new patient.
     * If email already exists, shows error. Otherwise saves and shows login form.
     */
    @PostMapping("/patient/register")
    public String registerPatient(@ModelAttribute Patient patient, Model model) {
        try {
            patientService.register(patient);
            model.addAttribute("success", "Account created! Please log in.");
            return renderAuth(model, "login", "patient");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("patient", patient);
            return renderAuth(model, "register", "patient");
        }
    }

    /**
     * Register a new therapist.
     */
    @PostMapping("/therapist/register")
    public String registerTherapist(@ModelAttribute Therapist therapist, Model model) {
        try {
            therapistService.register(therapist);
            model.addAttribute("success", "Account created! Please log in.");
            return renderAuth(model, "login", "therapist");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("therapist", therapist);
            return renderAuth(model, "register", "therapist");
        }
    }

    /**
     * Helper method — sets up the model attributes needed by auth.html
     * This avoids repeating the same code in every method above.
     */
    private String renderAuth(Model model, String mode, String role) {
        model.addAttribute("mode", mode);
        model.addAttribute("role", role);
        if (!model.containsAttribute("patient")) model.addAttribute("patient", new Patient());
        if (!model.containsAttribute("therapist")) model.addAttribute("therapist", new Therapist());
        return "auth";  // renders auth.html
    }
}
