package com.aicoach.backend.review;

import com.aicoach.backend.dto.ActivitySegmentDetailRequest;
import com.aicoach.backend.dto.ActivitySegmentDetails;

public interface ActivityCoach {
    Result review(ActivityReviewContext context, SegmentDetailsTool tool);

    @FunctionalInterface
    interface SegmentDetailsTool {
        ActivitySegmentDetails get(ActivitySegmentDetailRequest request);
    }

    record Result(String assessment, String responseId, String model,
                  Integer inputTokens, Integer outputTokens, Long latencyMs) {
    }
}
