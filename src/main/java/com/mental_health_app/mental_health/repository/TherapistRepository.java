package com.mental_health_app.mental_health.repository;

import com.mental_health_app.mental_health.entity.Therapist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link Therapist} entity.
 *
 * Provides standard CRUD operations (save, findById, findAll, delete, etc.)
 * via JpaRepository, plus custom query methods used by the auth and
 * therapist-management layers.
 */
@Repository
public interface TherapistRepository extends JpaRepository<Therapist, Long> {

    /**
     * Find a therapist by their email address.
     * Used during login to retrieve the account.
     */
    Optional<Therapist> findByEmail(String email);

    /**
     * Check whether a therapist with the given email already exists.
     * Used during registration to prevent duplicate accounts.
     */
    boolean existsByEmail(String email);

    /**
     * Find therapists by their specialization.
     * Used when patients search for a therapist by specialty.
     */
    List<Therapist> findBySpecialization(String specialization);

    /**
     * Find all therapists whose account is currently enabled/disabled.
     */
    List<Therapist> findByEnabled(boolean enabled);
}
