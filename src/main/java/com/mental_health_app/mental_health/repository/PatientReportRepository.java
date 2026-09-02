package com.mental_health_app.mental_health.repository;

import com.mental_health_app.mental_health.entity.Patient;
import com.mental_health_app.mental_health.entity.PatientReport;
import com.mental_health_app.mental_health.entity.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PatientReportRepository extends JpaRepository<PatientReport, Long> {

    // Find all reports for a patient, newest first (used by authorized therapists)
    List<PatientReport> findByPatientOrderByCreatedAtDesc(Patient patient);

    // Find only patient-visible reports (e.g., Assessment reports)
    List<PatientReport> findByPatientAndVisibleToPatientTrueOrderByCreatedAtDesc(Patient patient);

    // Find reports within a specific date window
    List<PatientReport> findByPatientAndCreatedAtBetweenOrderByCreatedAtDesc(Patient patient, LocalDateTime start, LocalDateTime end);

    // Find reports by patient and type
    List<PatientReport> findByPatientAndReportTypeOrderByCreatedAtDesc(Patient patient, ReportType reportType);

    // Find report by associated assessment
    java.util.Optional<PatientReport> findByAssessment(com.mental_health_app.mental_health.entity.Assessment assessment);

    long countByPatient(Patient patient);

    long countByPatientAndVisibleToPatientTrue(Patient patient);
}
