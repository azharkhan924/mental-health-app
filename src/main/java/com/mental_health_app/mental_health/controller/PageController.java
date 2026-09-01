package com.mental_health_app.mental_health.controller;

import com.mental_health_app.mental_health.dto.LoginRequest;
import com.mental_health_app.mental_health.dto.PatientRegisterRequest;
import com.mental_health_app.mental_health.dto.TherapistRegisterRequest;
import com.mental_health_app.mental_health.entity.Patient;
import com.mental_health_app.mental_health.entity.Therapist;
import com.mental_health_app.mental_health.service.PatientService;
import com.mental_health_app.mental_health.service.TherapistService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Thymeleaf-based controller for all page routes and form submissions.
 */
@Controller
public class PageController {

    private final PatientService patientService;
    private final TherapistService therapistService;

    public PageController(PatientService patientService,
                          TherapistService therapistService) {
        this.patientService = patientService;
        this.therapistService = therapistService;
    }

    // ── Landing page ──

    @GetMapping("/")
    public String landing() {
        return "index";
    }

    // ── Auth page (shows register form by default) ──

    @GetMapping("/auth")
    public String authPage(@RequestParam(defaultValue = "register") String mode,
                           @RequestParam(defaultValue = "patient") String role,
                           Model model) {
        model.addAttribute("mode", mode);
        model.addAttribute("role", role);
        model.addAttribute("patientRegister", new PatientRegisterRequest());
        model.addAttribute("therapistRegister", new TherapistRegisterRequest());
        model.addAttribute("loginRequest", new LoginRequest());
        return "auth";
    }

    // ── Patient Registration ──

    @PostMapping("/auth/patient/register")
    public String registerPatient(@Valid PatientRegisterRequest req,
                                  BindingResult result,
                                  RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("error", result.getFieldError().getDefaultMessage());
            return "redirect:/auth?mode=register&role=patient";
        }
        try {
            Patient patient = new Patient();
            patient.setName(req.getName());
            patient.setEmail(req.getEmail());
            patient.setPassword(req.getPassword());
            patient.setPhoneNumber(req.getPhoneNumber());
            patientService.register(patient);

            flash.addFlashAttribute("success", "Account created! Please log in.");
            return "redirect:/auth?mode=login&role=patient";
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/auth?mode=register&role=patient";
        }
    }

    // ── Therapist Registration ──

    @PostMapping("/auth/therapist/register")
    public String registerTherapist(@Valid TherapistRegisterRequest req,
                                    BindingResult result,
                                    RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("error", result.getFieldError().getDefaultMessage());
            return "redirect:/auth?mode=register&role=therapist";
        }
        try {
            Therapist therapist = new Therapist();
            therapist.setName(req.getName());
            therapist.setEmail(req.getEmail());
            therapist.setPassword(req.getPassword());
            therapist.setPhoneNumber(req.getPhoneNumber());
            therapist.setSpecialization(req.getSpecialization());
            therapist.setLicenseNumber(req.getLicenseNumber());
            therapistService.register(therapist);

            flash.addFlashAttribute("success", "Account created! Please log in.");
            return "redirect:/auth?mode=login&role=therapist";
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/auth?mode=register&role=therapist";
        }
    }

    // ── Login (shared for both roles) ──

    @PostMapping("/auth/login")
    public String login(@Valid LoginRequest req,
                        @RequestParam(defaultValue = "patient") String role,
                        BindingResult result,
                        RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("error", result.getFieldError().getDefaultMessage());
            return "redirect:/auth?mode=login&role=" + role;
        }

        if ("therapist".equals(role)) {
            Optional<Therapist> therapist = therapistService.login(req.getEmail(), req.getPassword());
            if (therapist.isPresent()) {
                flash.addFlashAttribute("success", "Welcome back, " + therapist.get().getName() + "!");
                return "redirect:/";
            }
        } else {
            Optional<Patient> patient = patientService.login(req.getEmail(), req.getPassword());
            if (patient.isPresent()) {
                flash.addFlashAttribute("success", "Welcome back, " + patient.get().getName() + "!");
                return "redirect:/";
            }
        }

        flash.addFlashAttribute("error", "Invalid email or password");
        return "redirect:/auth?mode=login&role=" + role;
    }
}
