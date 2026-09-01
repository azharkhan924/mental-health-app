package com.mental_health_app.mental_health.service;

import com.mental_health_app.mental_health.entity.Patient;
import com.mental_health_app.mental_health.repository.PatientRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Business-logic layer for Patient registration and authentication.
 */
@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    public PatientService(PatientRepository patientRepository,
                          PasswordEncoder passwordEncoder) {
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a new patient.
     * @throws IllegalArgumentException if the email is already in use.
     */
    public Patient register(Patient patient) {
        if (patientRepository.existsByEmail(patient.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        patient.setPassword(passwordEncoder.encode(patient.getPassword()));
        return patientRepository.save(patient);
    }

    /**
     * Authenticate a patient by email + raw password.
     * @return the Patient if credentials match, empty otherwise.
     */
    public Optional<Patient> login(String email, String rawPassword) {
        return patientRepository.findByEmail(email)
                .filter(p -> passwordEncoder.matches(rawPassword, p.getPassword()));
    }

    public Optional<Patient> findByEmail(String email) {
        return patientRepository.findByEmail(email);
    }
}
