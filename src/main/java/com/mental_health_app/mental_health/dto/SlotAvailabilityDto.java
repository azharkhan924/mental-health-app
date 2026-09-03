package com.mental_health_app.mental_health.dto;

/**
 * Represents the availability status of a specific consultation time slot.
 */
public class SlotAvailabilityDto {

    private String slotTime;
    private boolean available;
    private String reason; // e.g. "Available", "Booked by another patient", "Therapist not available on this day"

    public SlotAvailabilityDto() {
    }

    public SlotAvailabilityDto(String slotTime, boolean available, String reason) {
        this.slotTime = slotTime;
        this.available = available;
        this.reason = reason;
    }

    public String getSlotTime() {
        return slotTime;
    }

    public void setSlotTime(String slotTime) {
        this.slotTime = slotTime;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
