package com.mental_health_app.mental_health.repository;

import com.mental_health_app.mental_health.entity.Appointment;
import com.mental_health_app.mental_health.entity.AppointmentNotes;
import com.mental_health_app.mental_health.entity.Patient;
import com.mental_health_app.mental_health.entity.Therapist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentNotesRepository extends JpaRepository<AppointmentNotes, Long> {

    Optional<AppointmentNotes> findByAppointment(Appointment appointment);

    List<AppointmentNotes> findByPatientOrderByCreatedAtDesc(Patient patient);

    List<AppointmentNotes> findByTherapistOrderByCreatedAtDesc(Therapist therapist);

    boolean existsByAppointment(Appointment appointment);
}
