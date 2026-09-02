package com.mental_health_app.mental_health.service;

import com.mental_health_app.mental_health.entity.Appointment;
import com.mental_health_app.mental_health.entity.AppointmentStatus;
import com.mental_health_app.mental_health.entity.Patient;
import com.mental_health_app.mental_health.entity.Therapist;
import com.mental_health_app.mental_health.repository.AppointmentRepository;
import com.mental_health_app.mental_health.repository.TherapistRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * APPOINTMENT SERVICE
 * ───────────────────
 * Contains the core business logic for booking, viewing, and managing appointments.
 */
@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final TherapistRepository therapistRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              TherapistRepository therapistRepository) {
        this.appointmentRepository = appointmentRepository;
        this.therapistRepository = therapistRepository;
    }

    /**
     * Book a new appointment for a patient with a selected therapist.
     */
    public Appointment bookAppointment(Patient patient, Long therapistId, LocalDate date, 
                                       String time, String notes) {
        // Find therapist by ID
        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new IllegalArgumentException("Selected therapist does not exist"));

        // Create new Appointment entity
        Appointment appointment = new Appointment(patient, therapist, date, time, notes);

        // Save to database
        return appointmentRepository.save(appointment);
    }

    /**
     * Get all appointments booked by a patient (ordered newest first).
     */
    public List<Appointment> getAppointmentsForPatient(Patient patient) {
        return appointmentRepository.findByPatientOrderByAppointmentDateDesc(patient);
    }

    /**
     * Get all appointments assigned to a therapist (ordered newest first).
     */
    public List<Appointment> getAppointmentsForTherapist(Therapist therapist) {
        return appointmentRepository.findByTherapistOrderByAppointmentDateDesc(therapist);
    }

    /**
     * Update appointment status (e.g. CONFIRMED or CANCELLED).
     */
    public Appointment updateStatus(Long appointmentId, AppointmentStatus newStatus) {
        Optional<Appointment> optionalAppointment = appointmentRepository.findById(appointmentId);

        if (optionalAppointment.isPresent()) {
            Appointment appointment = optionalAppointment.get();
            appointment.setStatus(newStatus);
            return appointmentRepository.save(appointment);
        } else {
            throw new IllegalArgumentException("Appointment not found with ID: " + appointmentId);
        }
    }

    /**
     * Count appointments for a therapist by status (used for dashboard badges/counters).
     */
    public long countByTherapistAndStatus(Therapist therapist, AppointmentStatus status) {
        return appointmentRepository.countByTherapistAndStatus(therapist, status);
    }

    /**
     * Count appointments for a patient by status.
     */
    public long countByPatientAndStatus(Patient patient, AppointmentStatus status) {
        return appointmentRepository.countByPatientAndStatus(patient, status);
    }
}
