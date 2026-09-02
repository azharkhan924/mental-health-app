package com.mental_health_app.mental_health.repository;

import com.mental_health_app.mental_health.entity.Assessment;
import com.mental_health_app.mental_health.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for Assessment entity.
 */
@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    // Find all assessments completed by a patient, newest first
    List<Assessment> findByPatientOrderByCreatedAtDesc(Patient patient);

    // Count how many assessments a patient has completed
    long countByPatient(Patient patient);
}
