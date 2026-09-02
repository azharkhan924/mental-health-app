package com.mental_health_app.mental_health.service;

import com.mental_health_app.mental_health.entity.Patient;
import com.mental_health_app.mental_health.entity.Therapist;
import com.mental_health_app.mental_health.repository.PatientRepository;
import com.mental_health_app.mental_health.repository.TherapistRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

/**
 * HOW SPRING SECURITY LOGIN WORKS:
 * ─────────────────────────────────
 * When a user submits the login form, Spring Security automatically
 * calls the loadUserByUsername() method in this class.
 *
 * Our job is simple:
 *   1. Take the email from the login form
 *   2. Search the database for a Patient or Therapist with that email
 *   3. If found → return their details (Spring Security will check the password for us)
 *   4. If not found → throw an exception (login fails)
 *
 * We do NOT check the password here — Spring Security does that automatically
 * using the BCrypt password encoder from SecurityConfig.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final PatientRepository patientRepository;
    private final TherapistRepository therapistRepository;

    // Constructor injection — Spring automatically provides these
    public CustomUserDetailsService(PatientRepository patientRepository,
                                    TherapistRepository therapistRepository) {
        this.patientRepository = patientRepository;
        this.therapistRepository = therapistRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // Step 1: Search in the patients table
        Optional<Patient> patient = patientRepository.findByEmail(email);

        if (patient.isPresent()) {
            Patient p = patient.get();

            // Step 2: Return patient's details to Spring Security
            // "ROLE_PATIENT" tells Spring this user is a patient
            return new User(
                p.getEmail(),
                p.getPassword(),   // Spring Security will compare this with the form password
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))
            );
        }

        // Step 3: If not a patient, search in the therapists table
        Optional<Therapist> therapist = therapistRepository.findByEmail(email);

        if (therapist.isPresent()) {
            Therapist t = therapist.get();

            // Return therapist's details with "ROLE_THERAPIST"
            return new User(
                t.getEmail(),
                t.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_THERAPIST"))
            );
        }

        // Step 4: No user found in either table — login fails
        throw new UsernameNotFoundException("No account found with email: " + email);
    }
}
