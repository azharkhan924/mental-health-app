package com.mental_health_app.mental_health.repository;

import com.mental_health_app.mental_health.entity.CustomTest;
import com.mental_health_app.mental_health.entity.CustomTestAssignment;
import com.mental_health_app.mental_health.entity.Patient;
import com.mental_health_app.mental_health.entity.Therapist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomTestAssignmentRepository extends JpaRepository<CustomTestAssignment, Long> {

    List<CustomTestAssignment> findByPatientOrderByAssignedAtDesc(Patient patient);

    List<CustomTestAssignment> findByPatientAndStatusOrderByAssignedAtDesc(Patient patient, String status);

    List<CustomTestAssignment> findByAssignedByOrderByAssignedAtDesc(Therapist therapist);

    List<CustomTestAssignment> findByCustomTestOrderByAssignedAtDesc(CustomTest customTest);

    long countByPatientAndStatus(Patient patient, String status);

    boolean existsByCustomTestAndPatientAndStatus(CustomTest customTest, Patient patient, String status);
}
