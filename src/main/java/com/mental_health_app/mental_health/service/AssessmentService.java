package com.mental_health_app.mental_health.service;

import com.mental_health_app.mental_health.entity.Assessment;
import com.mental_health_app.mental_health.entity.AssessmentType;
import com.mental_health_app.mental_health.entity.Patient;
import com.mental_health_app.mental_health.repository.AssessmentRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * ASSESSMENT SERVICE
 * ──────────────────
 * Contains question sets, scoring algorithms, and clinical interpretations
 * for PHQ-9, GAD-7, PSS-10 (Stress), ISI (Insomnia), and Burnout assessments.
 */
@Service
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;

    public AssessmentService(AssessmentRepository assessmentRepository) {
        this.assessmentRepository = assessmentRepository;
    }

    /**
     * PHQ-9 Depression Screening Questions (9 questions)
     */
    public List<String> getPHQ9Questions() {
        return Arrays.asList(
            "1. Little interest or pleasure in doing things",
            "2. Feeling down, depressed, or hopeless",
            "3. Trouble falling or staying asleep, or sleeping too much",
            "4. Feeling tired or having little energy",
            "5. Poor appetite or overeating",
            "6. Feeling bad about yourself — or that you are a failure or have let yourself or your family down",
            "7. Trouble concentrating on things, such as reading or working",
            "8. Moving or speaking so slowly that others noticed, or being unusually restless",
            "9. Thoughts that you would be better off dead, or of hurting yourself in some way"
        );
    }

    /**
     * GAD-7 Anxiety Screening Questions (7 questions)
     */
    public List<String> getGAD7Questions() {
        return Arrays.asList(
            "1. Feeling nervous, anxious, or on edge",
            "2. Not being able to stop or control worrying",
            "3. Worrying too much about different things",
            "4. Trouble relaxing",
            "5. Being so restless that it's hard to sit still",
            "6. Becoming easily annoyed or irritable",
            "7. Feeling afraid as if something awful might happen"
        );
    }

    /**
     * PSS-10 Perceived Stress Scale (10 questions, rating 0 to 4)
     */
    public List<String> getPSS10Questions() {
        return Arrays.asList(
            "1. In the last month, how often have you been upset because of something that happened unexpectedly?",
            "2. In the last month, how often have you felt that you were unable to control the important things in your life?",
            "3. In the last month, how often have you felt nervous and stressed?",
            "4. In the last month, how often have you felt confident about your ability to handle your personal problems?",
            "5. In the last month, how often have you felt that things were going your way?",
            "6. In the last month, how often have you found that you could not cope with all the things that you had to do?",
            "7. In the last month, how often have you been able to control irritations in your life?",
            "8. In the last month, how often have you felt that you were on top of things?",
            "9. In the last month, how often have you been angered because of things that happened that were outside of your control?",
            "10. In the last month, how often have you felt difficulties were piling up so high that you could not overcome them?"
        );
    }

    /**
     * ISI Insomnia Severity Index (7 questions)
     */
    public List<String> getISIQuestions() {
        return Arrays.asList(
            "1. Difficulty falling asleep at bedtime",
            "2. Difficulty staying asleep (waking up during the night)",
            "3. Problems waking up too early in the morning",
            "4. How satisfied / dissatisfied are you with your current sleep pattern?",
            "5. How noticeable to others do you think your sleep problem is in terms of impairing the quality of your life?",
            "6. How worried / distressed are you about your current sleep problem?",
            "7. To what extent do you consider your sleep problem to interfere with your daily functioning (e.g. fatigue, mood, concentration)?"
        );
    }

    /**
     * Burnout & Academic/Work Fatigue Index (8 questions)
     */
    public List<String> getBurnoutQuestions() {
        return Arrays.asList(
            "1. I feel emotionally drained and exhausted from my work or studies",
            "2. I feel tired when I get up in the morning and have to face another day",
            "3. I feel like my motivation has significantly decreased over recent weeks",
            "4. I find myself becoming more cynical, detached, or frustrated easily",
            "5. I feel overwhelmed by deadlines, expectations, and daily workload",
            "6. I struggle to find joy or accomplishment in things I used to enjoy",
            "7. I feel like I am running on empty and lack mental bandwidth",
            "8. I have physical symptoms of fatigue (headaches, tension, lack of appetite)"
        );
    }

    /**
     * Get questions dynamically by assessment type
     */
    public List<String> getQuestionsForType(AssessmentType type) {
        return switch (type) {
            case PHQ9 -> getPHQ9Questions();
            case GAD7 -> getGAD7Questions();
            case PSS10 -> getPSS10Questions();
            case ISI -> getISIQuestions();
            case BURNOUT -> getBurnoutQuestions();
        };
    }

    /**
     * Calculate total score, determine severity, generate recommendations, and save to DB.
     */
    public Assessment evaluateAndSave(Patient patient, AssessmentType type, List<Integer> answers) {
        int totalScore = 0;
        for (Integer score : answers) {
            if (score != null) {
                totalScore += score;
            }
        }

        String severity;
        String recommendation;

        switch (type) {
            case PHQ9 -> {
                severity = getPHQ9Severity(totalScore);
                recommendation = getPHQ9Recommendation(totalScore);
            }
            case GAD7 -> {
                severity = getGAD7Severity(totalScore);
                recommendation = getGAD7Recommendation(totalScore);
            }
            case PSS10 -> {
                severity = getPSS10Severity(totalScore);
                recommendation = getPSS10Recommendation(totalScore);
            }
            case ISI -> {
                severity = getISISeverity(totalScore);
                recommendation = getISIRecommendation(totalScore);
            }
            case BURNOUT -> {
                severity = getBurnoutSeverity(totalScore);
                recommendation = getBurnoutRecommendation(totalScore);
            }
            default -> {
                severity = "Evaluation Complete";
                recommendation = "Assessment recorded successfully. Review with your healthcare specialist.";
            }
        }

        Assessment assessment = new Assessment(patient, type, totalScore, severity, recommendation);
        return assessmentRepository.save(assessment);
    }

    // --- PHQ-9 Scoring ---
    public String getPHQ9Severity(int score) {
        if (score <= 4) return "Minimal / None";
        if (score <= 9) return "Mild Depression";
        if (score <= 14) return "Moderate Depression";
        if (score <= 19) return "Moderately Severe Depression";
        return "Severe Depression";
    }

    public String getPHQ9Recommendation(int score) {
        if (score <= 4) return "Your score suggests minimal symptoms. Keep up with your balanced routine and daily mindfulness.";
        if (score <= 9) return "Mild depressive symptoms noted. Regular physical activity, proper sleep, and talking to trusted friends will help.";
        if (score <= 14) return "Moderate symptoms detected. We suggest scheduling a consultation with one of our licensed therapists for guidance.";
        if (score <= 19) return "Moderately severe symptoms identified. Professional psychological guidance is strongly advised.";
        return "Severe symptoms detected. Please reach out to a professional therapist promptly or contact the 988 Crisis Lifeline.";
    }

    // --- GAD-7 Scoring ---
    public String getGAD7Severity(int score) {
        if (score <= 4) return "Minimal Anxiety";
        if (score <= 9) return "Mild Anxiety";
        if (score <= 14) return "Moderate Anxiety";
        return "Severe Anxiety";
    }

    public String getGAD7Recommendation(int score) {
        if (score <= 4) return "Your score indicates minimal anxiety. Continue your positive self-care habits.";
        if (score <= 9) return "Mild anxiety symptoms. Practicing guided deep breathing and limiting caffeine intake can help reduce tension.";
        if (score <= 14) return "Moderate anxiety symptoms. Consulting with a mental health professional can help you learn cognitive coping strategies.";
        return "Severe anxiety symptoms identified. We strongly recommend booking a session with a licensed counselor.";
    }

    // --- PSS-10 (Stress) Scoring ---
    public String getPSS10Severity(int score) {
        if (score <= 13) return "Low Perceived Stress";
        if (score <= 26) return "Moderate Stress";
        return "High Perceived Stress";
    }

    public String getPSS10Recommendation(int score) {
        if (score <= 13) return "Your perceived stress level is low. You have good coping mechanisms in place. Maintain your healthy boundaries.";
        if (score <= 26) return "Moderate stress detected. Consider introducing daily relaxation breaks, time-blocking, and progressive muscle relaxation.";
        return "High stress levels detected. Chronic high stress impacts physical and mental health. We strongly advise speaking with our counselors and adopting immediate stress-reduction practices.";
    }

    // --- ISI (Insomnia / Sleep) Scoring ---
    public String getISISeverity(int score) {
        if (score <= 7) return "No Significant Insomnia";
        if (score <= 14) return "Subthreshold (Mild) Insomnia";
        if (score <= 21) return "Moderate Clinical Insomnia";
        return "Severe Clinical Insomnia";
    }

    public String getISIRecommendation(int score) {
        if (score <= 7) return "Your sleep pattern appears healthy. Maintain a consistent sleep schedule and comfortable sleep environment.";
        if (score <= 14) return "Mild sleep disturbance noted. Avoid screens 1 hour before bed, reduce late evening caffeine, and try relaxation audio in the AI Chat.";
        if (score <= 21) return "Moderate insomnia detected. Sleep restriction protocols and stimulus control therapy with a therapist can significantly restore deep sleep.";
        return "Severe insomnia identified. Chronic sleep deprivation affects all aspects of wellbeing. We recommend consulting a healthcare provider or sleep specialist.";
    }

    // --- Burnout Scoring ---
    public String getBurnoutSeverity(int score) {
        if (score <= 8) return "Healthy Energy Level";
        if (score <= 16) return "Early Burnout Warning";
        return "Severe Exhaustion & Burnout";
    }

    public String getBurnoutRecommendation(int score) {
        if (score <= 8) return "Your energy and engagement levels are balanced. Keep prioritizing regular rest and personal time.";
        if (score <= 16) return "Early warning signs of burnout detected. Take proactive steps: set clear work/study boundaries, take micro-breaks, and delegate tasks.";
        return "High burnout levels identified. You are experiencing significant mental fatigue. A complete digital detox weekend and consultation with a counselor is strongly advised.";
    }

    public List<Assessment> getPatientAssessments(Patient patient) {
        return assessmentRepository.findByPatientOrderByCreatedAtDesc(patient);
    }

    public Optional<Assessment> getById(Long id) {
        return assessmentRepository.findById(id);
    }

    public long countByPatient(Patient patient) {
        return assessmentRepository.countByPatient(patient);
    }
}
