package com.mental_health_app.mental_health.repository;

import com.mental_health_app.mental_health.entity.ChatMessageEntity;
import com.mental_health_app.mental_health.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    // Get message history for a patient with a specific persona, in chronological order
    List<ChatMessageEntity> findByPatientAndPersonaOrderByCreatedAtAsc(Patient patient, String persona);

    // Get all messages for a patient across all personas, newest first
    List<ChatMessageEntity> findByPatientOrderByCreatedAtDesc(Patient patient);

    // Get messages since a specific date for behavioral analysis
    List<ChatMessageEntity> findByPatientAndCreatedAtAfterOrderByCreatedAtAsc(Patient patient, LocalDateTime since);

    // Count user messages to trigger periodic behavioral reports
    long countByPatientAndRole(Patient patient, String role);

    // Clear messages for a specific persona
    void deleteByPatientAndPersona(Patient patient, String persona);
}
