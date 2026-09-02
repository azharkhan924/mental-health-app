package com.mental_health_app.mental_health.entity;

/**
 * Type of generated mental health report.
 */
public enum ReportType {
    ASSESSMENT("Clinical Assessment Report", true),
    CHAT_BEHAVIORAL("AI Chat Behavioral Analysis (Therapist Only)", false);

    private final String displayName;
    private final boolean defaultVisibleToPatient;

    ReportType(String displayName, boolean defaultVisibleToPatient) {
        this.displayName = displayName;
        this.defaultVisibleToPatient = defaultVisibleToPatient;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isDefaultVisibleToPatient() {
        return defaultVisibleToPatient;
    }
}
