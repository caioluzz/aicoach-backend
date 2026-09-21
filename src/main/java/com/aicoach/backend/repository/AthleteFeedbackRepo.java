package com.aicoach.backend.repository;

import com.aicoach.backend.models.AthleteFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AthleteFeedbackRepo extends JpaRepository<AthleteFeedback, Long> {
}
