package com.mental_health_app.mental_health.repository;

import com.mental_health_app.mental_health.entity.Appointment;
import com.mental_health_app.mental_health.entity.AppointmentStatus;
import com.mental_health_app.mental_health.entity.Patient;
import com.mental_health_app.mental_health.entity.Therapist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Appointment database operations.
 * Spring Data JPA provides built-in CRUD methods (save, findById, findAll, delete, etc.)
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Find all appointments for a specific patient, newest first
    List<Appointment> findByPatientOrderByAppointmentDateDesc(Patient patient);

    // Find all appointments for a specific therapist, newest first
    List<Appointment> findByTherapistOrderByAppointmentDateDesc(Therapist therapist);

    // Count appointments by therapist and status (e.g. how many PENDING or CONFIRMED)
    long countByTherapistAndStatus(Therapist therapist, AppointmentStatus status);

    // Count appointments by patient and status
    long countByPatientAndStatus(Patient patient, AppointmentStatus status);

    // Check if an appointment slot is already booked (status not CANCELLED)
    boolean existsByTherapistAndAppointmentDateAndAppointmentTimeAndStatusNot(Therapist therapist, 
                                                                               java.time.LocalDate appointmentDate, 
                                                                               String appointmentTime, 
                                                                               AppointmentStatus status);

    // Find all active/pending appointments for a therapist on a specific date
    List<Appointment> findByTherapistAndAppointmentDateAndStatusNot(Therapist therapist, 
                                                                    java.time.LocalDate appointmentDate, 
                                                                    AppointmentStatus status);
}
