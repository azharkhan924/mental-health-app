package com.mental_health_app.mental_health.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a reusable custom psychometric or psychological assessment
 * created by a therapist via document upload or AI question parsing.
 */
@Entity
@Table(name = "custom_tests")
public class CustomTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "therapist_id", nullable = false)
    private Therapist therapist;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;

    // JSON formatted list of questions: [{"id":1, "question":"...", "options":[{"label":"...", "score":1}]}]
    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionsJson;

    // JSON or text scoring interpretation rules: [{"min":0, "max":5, "severity":"Low", "recommendation":"..."}]
    @Column(columnDefinition = "TEXT")
    private String scoringRulesJson;

    private int maxScore;

    private int questionCount;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    public CustomTest() {
    }

    public CustomTest(Therapist therapist, String title, String description, String category,
                      String questionsJson, String scoringRulesJson, int maxScore, int questionCount) {
        this.therapist = therapist;
        this.title = title;
        this.description = description;
        this.category = category;
        this.questionsJson = questionsJson;
        this.scoringRulesJson = scoringRulesJson;
        this.maxScore = maxScore;
        this.questionCount = questionCount;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getQuestionsJson() {
        return questionsJson;
    }

    public void setQuestionsJson(String questionsJson) {
        this.questionsJson = questionsJson;
    }

    public String getScoringRulesJson() {
        return scoringRulesJson;
    }

    public void setScoringRulesJson(String scoringRulesJson) {
        this.scoringRulesJson = scoringRulesJson;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
