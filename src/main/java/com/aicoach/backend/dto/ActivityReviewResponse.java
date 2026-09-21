package com.aicoach.backend.dto;

import com.aicoach.backend.enums.ActivityReviewStatus;
import com.aicoach.backend.enums.SegmentQueryType;
import com.aicoach.backend.enums.SegmentResolution;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ActivityReviewResponse(
        Long id,
        Long activityId,
        Long comparisonId,
        ActivityReviewStatus status,
        String policyVersion,
        String policyReason,
        boolean modelCalled,
        String assessment,
        String model,
        String responseId,
        Integer inputTokens,
        Integer outputTokens,
        Long latencyMs,
        Instant createdAt,
        Instant completedAt,
        List<DetailRequest> detailRequests) {

    public record DetailRequest(Long id, SegmentQueryType queryType, SegmentResolution resolution,
                                BigDecimal start, BigDecimal end, BigDecimal contextBefore,
                                BigDecimal contextAfter, List<String> fields, String reason,
                                Instant requestedAt, Integer pointsReturned) {
    }
}
