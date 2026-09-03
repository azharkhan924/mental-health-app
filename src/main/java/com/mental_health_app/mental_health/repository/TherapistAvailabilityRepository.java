package com.mental_health_app.mental_health.repository;

import com.mental_health_app.mental_health.entity.Therapist;
import com.mental_health_app.mental_health.entity.TherapistAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface TherapistAvailabilityRepository extends JpaRepository<TherapistAvailability, Long> {

    List<TherapistAvailability> findByTherapist(Therapist therapist);

    List<TherapistAvailability> findByTherapistAndActiveTrue(Therapist therapist);

    Optional<TherapistAvailability> findByTherapistAndDayOfWeek(Therapist therapist, DayOfWeek dayOfWeek);
}
