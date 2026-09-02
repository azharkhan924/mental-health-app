package com.mental_health_app.mental_health.service;

import com.mental_health_app.mental_health.entity.Therapist;
import com.mental_health_app.mental_health.repository.TherapistRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Business-logic layer for Therapist operations (registration, retrieval, etc.).
 */
@Service
public class TherapistService {

    private final TherapistRepository therapistRepository;
    private final PasswordEncoder passwordEncoder;

    public TherapistService(TherapistRepository therapistRepository,
                            PasswordEncoder passwordEncoder) {
        this.therapistRepository = therapistRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a new therapist.
     * @throws IllegalArgumentException if the email is already in use.
     */
    public Therapist register(Therapist therapist) {
        if (therapistRepository.existsByEmail(therapist.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        therapist.setPassword(passwordEncoder.encode(therapist.getPassword()));
        return therapistRepository.save(therapist);
    }

    /**
     * Authenticate a therapist by email + raw password.
     */
    public Optional<Therapist> login(String email, String rawPassword) {
        return therapistRepository.findByEmail(email)
                .filter(t -> passwordEncoder.matches(rawPassword, t.getPassword()));
    }

    public Optional<Therapist> findByEmail(String email) {
        return therapistRepository.findByEmail(email);
    }

    public Optional<Therapist> findById(Long id) {
        return therapistRepository.findById(id);
    }

    /**
     * Get all therapists registered in the system (for patients to browse).
     */
    public List<Therapist> getAllTherapists() {
        return therapistRepository.findAll();
    }
}
