package com.mental_health_app.mental_health.controller;

import com.mental_health_app.mental_health.entity.Patient;
import com.mental_health_app.mental_health.entity.Therapist;
import com.mental_health_app.mental_health.service.PatientService;
import com.mental_health_app.mental_health.service.TherapistService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Clean & minimal Auth Controller for Patient & Therapist authentication.
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

    @GetMapping
    public String showAuthPage(@RequestParam(defaultValue = "register") String mode,
                               @RequestParam(defaultValue = "patient") String role,
                               Model model) {
        return renderAuth(model, mode, role);
    }

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

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        @RequestParam(defaultValue = "patient") String role,
                        Model model) {
        boolean success = "therapist".equals(role)
                ? therapistService.login(email, password).isPresent()
                : patientService.login(email, password).isPresent();

        if (success) {
            return "redirect:/";
        }

        model.addAttribute("error", "Invalid email or password");
        return renderAuth(model, "login", role);
    }

    // Helper to keep code DRY (Don't Repeat Yourself)
    private String renderAuth(Model model, String mode, String role) {
        model.addAttribute("mode", mode);
        model.addAttribute("role", role);
        if (!model.containsAttribute("patient")) model.addAttribute("patient", new Patient());
        if (!model.containsAttribute("therapist")) model.addAttribute("therapist", new Therapist());
        return "auth";
    }
}
