package com.aicoach.backend.repository;

import com.aicoach.backend.models.WorkoutBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkoutBlockRepo extends JpaRepository<WorkoutBlock, Long> {
}