package com.mental_health_app.mental_health.entity;

import jakarta.persistence.*;
import java.time.DayOfWeek;

/**
 * Represents a therapist's weekly recurring availability for a specific day of the week,
 * along with configured consultation time slots.
 */
@Entity
@Table(name = "therapist_availabilities", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"therapist_id", "day_of_week"})
})
public class TherapistAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "therapist_id", nullable = false)
    private Therapist therapist;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    // Comma-separated list of time slots, e.g. "09:00 AM, 10:30 AM, 01:00 PM, 02:30 PM, 04:00 PM"
    @Column(length = 1000, nullable = false)
    private String timeSlots;

    @Column(nullable = false)
    private boolean active = true;

    public TherapistAvailability() {
    }

    public TherapistAvailability(Therapist therapist, DayOfWeek dayOfWeek, String timeSlots, boolean active) {
        this.therapist = therapist;
        this.dayOfWeek = dayOfWeek;
        this.timeSlots = timeSlots;
        this.active = active;
    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Therapist getTherapist() {
        return therapist;
    }

    public void setTherapist(Therapist therapist) {
        this.therapist = therapist;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getTimeSlots() {
        return timeSlots;
    }

    public void setTimeSlots(String timeSlots) {
        this.timeSlots = timeSlots;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
