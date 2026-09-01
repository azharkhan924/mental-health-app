package com.mental_health_app.mental_health.controller;

import com.mental_health_app.mental_health.dto.LoginRequest;
import com.mental_health_app.mental_health.dto.TherapistRegisterRequest;
import com.mental_health_app.mental_health.entity.Therapist;
import com.mental_health_app.mental_health.service.TherapistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for Therapist registration and login.
 *
 * Endpoints:
 *   POST /api/therapists/register  — create a new therapist account
 *   POST /api/therapists/login     — authenticate with email + password
 */
@RestController
@RequestMapping("/api/therapists")
public class TherapistController {

    private final TherapistService therapistService;

    public TherapistController(TherapistService therapistService) {
        this.therapistService = therapistService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody TherapistRegisterRequest req) {
        try {
            Therapist therapist = new Therapist();
            therapist.setName(req.getName());
            therapist.setEmail(req.getEmail());
            therapist.setPassword(req.getPassword());
            therapist.setPhoneNumber(req.getPhoneNumber());
            therapist.setSpecialization(req.getSpecialization());
            therapist.setLicenseNumber(req.getLicenseNumber());

            Therapist saved = therapistService.register(therapist);

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
        return therapistService.login(req.getEmail(), req.getPassword())
                .map(therapist -> ResponseEntity.ok(Map.of(
                        "message", "Login successful",
                        "id", therapist.getId(),
                        "name", therapist.getName(),
                        "email", therapist.getEmail(),
                        "role", therapist.getRole().name()
                )))
                .orElse(ResponseEntity.status(401)
                        .body(Map.of("error", "Invalid email or password")));
    }
}
