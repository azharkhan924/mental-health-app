package com.mental_health_app.mental_health.entity;

/**
 * Standard clinical and psychological self-assessments supported by the platform.
 */
public enum AssessmentType {
    PHQ9("PHQ-9 Depression Screening", 27, 9),
    GAD7("GAD-7 Anxiety Screening", 21, 7),
    PSS10("PSS-10 Perceived Stress Scale", 40, 10),
    ISI("ISI Insomnia Severity Index", 28, 7),
    BURNOUT("Burnout & Fatigue Index", 24, 8);

    private final String displayName;
    private final int maxScore;
    private final int questionCount;

    AssessmentType(String displayName, int maxScore, int questionCount) {
        this.displayName = displayName;
        this.maxScore = maxScore;
        this.questionCount = questionCount;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public int getQuestionCount() {
        return questionCount;
    }
}
