package com.aicoach.backend.repository;

import com.aicoach.backend.models.PlannedActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PlannedActivityRepo extends JpaRepository<PlannedActivity, Long> {
}