package com.aicoach.backend.review;

import com.aicoach.backend.enums.ComplianceClassification;
import com.aicoach.backend.models.Activity;
import com.aicoach.backend.models.ActivityComparison;
import com.aicoach.backend.models.ActivityStepComparison;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ActivityReviewPolicyTest {
    private final ActivityReviewPolicy policy = new ActivityReviewPolicy();

    @Test
    void skipsTrivialActivityWithoutCallingModel() {
        ActivityComparison comparison = comparison(500.0, 900.0, ComplianceClassification.DIFFERENT, "40.00");

        ActivityReviewPolicy.Decision decision = policy.decide(comparison);

        assertFalse(decision.callModel());
        assertTrue(decision.reason().contains("trivial"));
    }

    @Test
    void skipsFullyCompliantActivityWithoutAdditionalValue() {
        ActivityComparison comparison = comparison(2400.0, 7000.0, ComplianceClassification.FULFILLED, "98.00");
        ActivityStepComparison step = new ActivityStepComparison();
        step.setClassification(ComplianceClassification.FULFILLED);
        comparison.getSteps().add(step);

        assertFalse(policy.decide(comparison).callModel());
    }

    @Test
    void callsCoachForRelevantDiscrepancy() {
        ActivityComparison comparison = comparison(2400.0, 7000.0, ComplianceClassification.PARTIAL, "72.00");

        assertTrue(policy.decide(comparison).callModel());
    }

    private ActivityComparison comparison(double duration, double distance,
                                          ComplianceClassification classification, String percentage) {
        Activity activity = new Activity();
        activity.setDurationSeconds(duration);
        activity.setDistanceMeters(distance);
        ActivityComparison comparison = new ActivityComparison();
        comparison.setActivity(activity);
        comparison.setClassification(classification);
        comparison.setCompliancePercentage(new BigDecimal(percentage));
        return comparison;
    }
}
