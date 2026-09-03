package com.mental_health_app.mental_health.repository;

import com.mental_health_app.mental_health.entity.CustomTest;
import com.mental_health_app.mental_health.entity.Therapist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomTestRepository extends JpaRepository<CustomTest, Long> {

    List<CustomTest> findByTherapistOrderByCreatedAtDesc(Therapist therapist);
}
