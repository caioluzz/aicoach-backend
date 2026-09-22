package com.aicoach.backend.service;

import com.aicoach.backend.dto.ActivityComparisonResponse;
import com.aicoach.backend.dto.ActivityReviewResponse;
import com.aicoach.backend.enums.ActivityReviewStatus;
import com.aicoach.backend.models.ActivityComparison;
import com.aicoach.backend.models.ActivityReview;
import com.aicoach.backend.repository.ActivityComparisonRepo;
import com.aicoach.backend.repository.ActivityReviewRepo;
import com.aicoach.backend.review.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityReviewService {
    private final ActivityComparisonRepo comparisonRepo;
    private final ActivityReviewRepo reviewRepo;
    private final ActivityComparisonService comparisonService;
    private final ActivityReviewPolicy policy;
    private final ActivityCoach coach;
    private final ActivitySegmentDetailsService detailsService;
    private final Clock clock;

    @Transactional(noRollbackFor = ActivityReviewException.class)
    public ActivityReviewResponse review(Long activityId) {
        ActivityReview existing = reviewRepo.findByActivityId(activityId).orElse(null);
        if (existing != null && (existing.getStatus() == ActivityReviewStatus.COMPLETED
                || existing.getStatus() == ActivityReviewStatus.SKIPPED)) {
            return toResponse(existing);
        }
        ActivityComparison comparison = comparisonRepo.findByActivityId(activityId)
                .orElseThrow(() -> new ActivityComparisonNotFoundException(activityId));
        ActivityReviewPolicy.Decision decision = policy.decide(comparison);
        ActivityReview review = existing == null ? new ActivityReview() : existing;
        review.setActivity(comparison.getActivity());
        review.setComparison(comparison);
        review.setPolicyVersion(ActivityReviewPolicy.VERSION);
        review.setPolicyReason(decision.reason());
        review.setCreatedAt(review.getCreatedAt() == null ? clock.instant() : review.getCreatedAt());
        review.setCompletedAt(null);
        review.setResponseId(null);
        review.setModel(null);
        review.setInputTokens(null);
        review.setOutputTokens(null);
        review.setLatencyMs(null);

        if (!decision.callModel()) {
            review.setStatus(ActivityReviewStatus.SKIPPED);
            review.setModelCalled(false);
            review.setAssessment(decision.deterministicAssessment());
            review.setCompletedAt(clock.instant());
            return toResponse(reviewRepo.save(review));
        }

        review.setStatus(ActivityReviewStatus.IN_PROGRESS);
        review.setModelCalled(true);
        review.setAssessment(null);
        reviewRepo.saveAndFlush(review);
        ActivityComparisonResponse summary = comparisonService.getForActivity(activityId);
        ActivityReviewContext context = new ActivityReviewContext(comparison.getActivity().getAthlete().getId(),
                review.getId(), activityId,
                comparison.getActivity().getStartedAt(), comparison.getActivity().getName(),
                comparison.getActivity().getSport(), comparison.getActivity().getDurationSeconds(),
                comparison.getActivity().getDistanceMeters(), comparison.getActivity().getAverageHeartRate(),
                comparison.getActivity().getAvgCadence(), summary);
        try {
            ActivityCoach.Result result = coach.review(context,
                    request -> detailsService.getActivitySegmentDetails(review.getId(), request));
            review.setAssessment(result.assessment());
            review.setResponseId(result.responseId());
            review.setModel(result.model());
            review.setInputTokens(result.inputTokens());
            review.setOutputTokens(result.outputTokens());
            review.setLatencyMs(result.latencyMs());
            review.setStatus(ActivityReviewStatus.COMPLETED);
            review.setCompletedAt(clock.instant());
            return toResponse(reviewRepo.save(review));
        } catch (ActivityReviewException exception) {
            review.setStatus(ActivityReviewStatus.FAILED);
            review.setAssessment(null);
            review.setCompletedAt(clock.instant());
            reviewRepo.save(review);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public ActivityReviewResponse getForActivity(Long activityId) {
        return reviewRepo.findByActivityId(activityId).map(this::toResponse)
                .orElseThrow(() -> new ActivityReviewNotFoundException(activityId));
    }

    private ActivityReviewResponse toResponse(ActivityReview review) {
        List<ActivityReviewResponse.DetailRequest> requests = review.getDetailRequests().stream()
                .map(item -> new ActivityReviewResponse.DetailRequest(item.getId(), item.getQueryType(),
                        item.getResolution(), item.getRangeStart(), item.getRangeEnd(), item.getContextBefore(),
                        item.getContextAfter(), item.getFields() == null || item.getFields().isBlank()
                        ? List.of() : Arrays.asList(item.getFields().split(",")), item.getReason(),
                        item.getRequestedAt(), item.getPointsReturned())).toList();
        return new ActivityReviewResponse(review.getId(), review.getActivity().getId(),
                review.getComparison().getId(), review.getStatus(), review.getPolicyVersion(),
                review.getPolicyReason(), review.isModelCalled(), review.getAssessment(), review.getModel(),
                review.getResponseId(), review.getInputTokens(), review.getOutputTokens(), review.getLatencyMs(),
                review.getCreatedAt(), review.getCompletedAt(), requests);
    }
}
