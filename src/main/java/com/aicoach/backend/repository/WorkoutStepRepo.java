package com.aicoach.backend.repository;

import com.aicoach.backend.models.WorkoutStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkoutStepRepo extends JpaRepository<WorkoutStep, Long> {
}