package com.mental_health_app.mental_health.repository;

import com.mental_health_app.mental_health.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link Patient} entity.
 *
 * Provides standard CRUD operations (save, findById, findAll, delete, etc.)
 * via JpaRepository, plus custom query methods used by the auth and
 * patient-management layers.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    /**
     * Find a patient by their email address.
     * Used during login to retrieve the account.
     */
    Optional<Patient> findByEmail(String email);

    /**
     * Check whether a patient with the given email already exists.
     * Used during registration to prevent duplicate accounts.
     */
    boolean existsByEmail(String email);

    /**
     * Find all patients whose account is currently enabled/disabled.
     */
    List<Patient> findByEnabled(boolean enabled);
}
