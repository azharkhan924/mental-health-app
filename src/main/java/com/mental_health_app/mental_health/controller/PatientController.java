package com.mental_health_app.mental_health.controller;

import com.mental_health_app.mental_health.dto.LoginRequest;
import com.mental_health_app.mental_health.dto.PatientRegisterRequest;
import com.mental_health_app.mental_health.entity.Patient;
import com.mental_health_app.mental_health.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for Patient registration and login.
 *
 * Endpoints:
 *   POST /api/patients/register  — create a new patient account
 *   POST /api/patients/login     — authenticate with email + password
 */
@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody PatientRegisterRequest req) {
        try {
            Patient patient = new Patient();
            patient.setName(req.getName());
            patient.setEmail(req.getEmail());
            patient.setPassword(req.getPassword());
            patient.setPhoneNumber(req.getPhoneNumber());

            Patient saved = patientService.register(patient);

            return ResponseEntity.ok(Map.of(
                    "message", "Registration successful",
                    "id", saved.getId(),
                    "name", saved.getName(),
                    "email", saved.getEmail(),
                    "role", saved.getRole().name()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        return patientService.login(req.getEmail(), req.getPassword())
                .map(patient -> ResponseEntity.ok(Map.of(
                        "message", "Login successful",
                        "id", patient.getId(),
                        "name", patient.getName(),
                        "email", patient.getEmail(),
                        "role", patient.getRole().name()
                )))
                .orElse(ResponseEntity.status(401)
                        .body(Map.of("error", "Invalid email or password")));
    }
}
