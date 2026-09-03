package com.mental_health_app.mental_health.service;

import com.mental_health_app.mental_health.dto.SlotAvailabilityDto;
import com.mental_health_app.mental_health.entity.*;
import com.mental_health_app.mental_health.repository.AppointmentRepository;
import com.mental_health_app.mental_health.repository.TherapistAvailabilityRepository;
import com.mental_health_app.mental_health.repository.TherapistRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * APPOINTMENT SERVICE
 * ───────────────────
 * Contains the core business logic for booking, viewing, and managing appointments,
 * therapist availability configuration, and real-time conflict prevention.
 */
@Service
public class AppointmentService {

    private static final List<String> DEFAULT_TIME_SLOTS = List.of(
            "09:00 AM", "10:30 AM", "01:00 PM", "02:30 PM", "04:00 PM", "05:30 PM"
    );

    private final AppointmentRepository appointmentRepository;
    private final TherapistRepository therapistRepository;
    private final TherapistAvailabilityRepository availabilityRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              TherapistRepository therapistRepository,
                              TherapistAvailabilityRepository availabilityRepository) {
        this.appointmentRepository = appointmentRepository;
        this.therapistRepository = therapistRepository;
        this.availabilityRepository = availabilityRepository;
    }

    /**
     * Book a new appointment for a patient with a selected therapist.
     * Enforces strict slot collision prevention (cannot double-book same slot).
     */
    public synchronized Appointment bookAppointment(Patient patient, Long therapistId, LocalDate date, 
                                                    String time, String notes) {
        // Find therapist by ID
        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new IllegalArgumentException("Selected therapist does not exist"));

        // Conflict check: Ensure slot is not already booked by someone else
        boolean alreadyBooked = appointmentRepository.existsByTherapistAndAppointmentDateAndAppointmentTimeAndStatusNot(
                therapist, date, time.trim(), AppointmentStatus.CANCELLED);
        if (alreadyBooked) {
            throw new IllegalStateException("The time slot (" + time + ") on " + date + " is already booked. Please choose an available slot.");
        }

        // Create new Appointment entity
        Appointment appointment = new Appointment(patient, therapist, date, time.trim(), notes);

        // Save to database
        return appointmentRepository.save(appointment);
    }

    /**
     * Get real-time slot availability for a specific therapist on a given date.
     */
    public List<SlotAvailabilityDto> getAvailableSlots(Long therapistId, LocalDate date) {
        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new IllegalArgumentException("Therapist not found"));

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        Optional<TherapistAvailability> availOpt = availabilityRepository.findByTherapistAndDayOfWeek(therapist, dayOfWeek);

        // Check if day is disabled by therapist
        if (availOpt.isPresent() && !availOpt.get().isActive()) {
            return Collections.emptyList(); // Not working on this day
        }

        // Get slots configured for this day, or defaults
        List<String> configuredSlots = new ArrayList<>();
        if (availOpt.isPresent() && availOpt.get().getTimeSlots() != null && !availOpt.get().getTimeSlots().isBlank()) {
            String[] parts = availOpt.get().getTimeSlots().split(",");
            for (String p : parts) {
                if (!p.trim().isEmpty()) {
                    configuredSlots.add(p.trim());
                }
            }
        }
        if (configuredSlots.isEmpty()) {
            configuredSlots = DEFAULT_TIME_SLOTS;
        }

        // Find already booked slots on that date (excluding cancelled)
        List<Appointment> bookedAppointments = appointmentRepository.findByTherapistAndAppointmentDateAndStatusNot(
                therapist, date, AppointmentStatus.CANCELLED);
        Set<String> bookedSlots = bookedAppointments.stream()
                .map(a -> a.getAppointmentTime().trim())
                .collect(Collectors.toSet());

        List<SlotAvailabilityDto> result = new ArrayList<>();
        for (String slot : configuredSlots) {
            boolean isBooked = bookedSlots.contains(slot);
            result.add(new SlotAvailabilityDto(
                    slot,
                    !isBooked,
                    isBooked ? "Slot already booked by another patient" : "Available for consultation"
            ));
        }
        return result;
    }

    /**
     * Get or initialize all 7 day availabilities for a therapist.
     */
    public List<TherapistAvailability> getOrCreateTherapistAvailabilities(Therapist therapist) {
        List<TherapistAvailability> existing = availabilityRepository.findByTherapist(therapist);
        if (existing.size() == 7) {
            existing.sort(Comparator.comparing(TherapistAvailability::getDayOfWeek));
            return existing;
        }

        Map<DayOfWeek, TherapistAvailability> map = existing.stream()
                .collect(Collectors.toMap(TherapistAvailability::getDayOfWeek, a -> a));

        List<TherapistAvailability> all = new ArrayList<>();
        String defaultSlotsJoined = String.join(", ", DEFAULT_TIME_SLOTS);

        for (DayOfWeek dow : DayOfWeek.values()) {
            if (map.containsKey(dow)) {
                all.add(map.get(dow));
            } else {
                // Sunday off by default, Mon-Sat active
                boolean active = dow != DayOfWeek.SUNDAY;
                TherapistAvailability newAvail = new TherapistAvailability(therapist, dow, defaultSlotsJoined, active);
                all.add(availabilityRepository.save(newAvail));
            }
        }
        all.sort(Comparator.comparing(TherapistAvailability::getDayOfWeek));
        return all;
    }

    /**
     * Update a therapist's day availability and time slots.
     */
    public TherapistAvailability updateDayAvailability(Therapist therapist, DayOfWeek dayOfWeek, 
                                                       boolean active, String timeSlots) {
        TherapistAvailability avail = availabilityRepository.findByTherapistAndDayOfWeek(therapist, dayOfWeek)
                .orElse(new TherapistAvailability(therapist, dayOfWeek, timeSlots, active));

        avail.setActive(active);
        avail.setTimeSlots((timeSlots != null && !timeSlots.isBlank()) ? timeSlots.trim() : String.join(", ", DEFAULT_TIME_SLOTS));
        return availabilityRepository.save(avail);
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

    /**
     * Find an appointment by its ID.
     */
    public Optional<Appointment> getAppointmentById(Long id) {
        return appointmentRepository.findById(id);
    }
}
