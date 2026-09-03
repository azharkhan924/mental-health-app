package com.mental_health_app.mental_health.repository;

import com.mental_health_app.mental_health.entity.Patient;
import com.mental_health_app.mental_health.entity.PreAppointmentTask;
import com.mental_health_app.mental_health.entity.Therapist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PreAppointmentTaskRepository extends JpaRepository<PreAppointmentTask, Long> {

    List<PreAppointmentTask> findByPatientOrderByCreatedAtDesc(Patient patient);

    List<PreAppointmentTask> findByPatientAndStatusOrderByCreatedAtDesc(Patient patient, String status);

    List<PreAppointmentTask> findByTherapistOrderByCreatedAtDesc(Therapist therapist);

    long countByPatientAndStatus(Patient patient, String status);
}
