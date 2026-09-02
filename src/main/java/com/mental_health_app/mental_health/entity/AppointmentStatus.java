package com.mental_health_app.mental_health.entity;

/**
 * Represents the status of an appointment.
 * 
 * PENDING   - Patient requested the appointment, waiting for therapist response.
 * CONFIRMED - Therapist accepted the appointment.
 * CANCELLED - Either patient or therapist cancelled the appointment.
 */
public enum AppointmentStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
