package com.aicoach.backend.repository;

import com.aicoach.backend.models.ActivitySegmentDetailRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivitySegmentDetailRequestRepo extends JpaRepository<ActivitySegmentDetailRequest, Long> {
    List<ActivitySegmentDetailRequest> findByReviewIdOrderByRequestedAtAsc(Long reviewId);
}
