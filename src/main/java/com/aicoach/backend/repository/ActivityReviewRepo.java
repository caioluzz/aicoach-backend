package com.aicoach.backend.repository;

import com.aicoach.backend.models.ActivityReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActivityReviewRepo extends JpaRepository<ActivityReview, Long> {
    Optional<ActivityReview> findByActivityId(Long activityId);
}
