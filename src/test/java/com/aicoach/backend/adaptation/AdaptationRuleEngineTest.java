package com.aicoach.backend.adaptation;

import com.aicoach.backend.enums.AdaptationAlertLevel;
import com.aicoach.backend.enums.ComplianceClassification;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AdaptationRuleEngineTest {
    private final AdaptationRuleEngine engine = new AdaptationRuleEngine();

    @Test
    void severePainStopsTrainingAndProposesSeasonReview() {
        var decision = engine.evaluate(new AdaptationRuleEngine.Input(
                6, 5, 8, 8, ComplianceClassification.FULFILLED, new BigDecimal("100"), 0));

        assertEquals(AdaptationAlertLevel.STOP_AND_ASSESS, decision.alertLevel());
        assertEquals(100, decision.loadReductionPercent());
        assertFalse(decision.allowIntensity());
        assertTrue(decision.seasonPlanReviewProposed());
    }

    @Test
    void isolatedModeratePainOnlyRevisesUpcomingWeek() {
        var decision = engine.evaluate(new AdaptationRuleEngine.Input(
                7, 6, 7, 5, ComplianceClassification.PARTIAL, new BigDecimal("70"), 0));

        assertEquals(AdaptationAlertLevel.REDUCE_LOAD, decision.alertLevel());
        assertEquals(40, decision.loadReductionPercent());
        assertTrue(decision.weeklyReviewRequired());
        assertFalse(decision.seasonPlanReviewProposed());
    }

    @Test
    void recurrenceIsMaterialButNormalFeedbackDoesNotReplan() {
        var recurrent = engine.evaluate(new AdaptationRuleEngine.Input(
                6, 6, 7, 4, null, null, 2));
        var normal = engine.evaluate(new AdaptationRuleEngine.Input(
                6, 4, 8, 0, ComplianceClassification.FULFILLED, new BigDecimal("98"), 0));

        assertTrue(recurrent.seasonPlanReviewProposed());
        assertNotNull(recurrent.materialCause());
        assertEquals(AdaptationAlertLevel.NONE, normal.alertLevel());
        assertFalse(normal.weeklyReviewRequired());
    }

    @Test
    void executionDiscrepancyAndVeryHighEffortReduceLoad() {
        var decision = engine.evaluate(new AdaptationRuleEngine.Input(
                9, 6, 7, 0, ComplianceClassification.PARTIAL, new BigDecimal("68"), 0));

        assertEquals(25, decision.loadReductionPercent());
        assertFalse(decision.allowIntensity());
    }
}
