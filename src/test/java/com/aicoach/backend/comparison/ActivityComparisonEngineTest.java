package com.aicoach.backend.comparison;

import com.aicoach.backend.enums.ComplianceClassification;
import com.aicoach.backend.enums.DurationType;
import com.aicoach.backend.enums.StepAlignmentSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ActivityComparisonEngineTest {
    private final ActivityComparisonEngine engine =
            new ActivityComparisonEngine(ComparisonTolerancePolicy.V1);

    @Test
    void alignsTimeStepsToRecordsAndProducesReproduciblePercentage() {
        var plan = new ActivityComparisonEngine.Plan(1200, 4000, List.of(
                new ActivityComparisonEngine.PlannedStep(11L, 1, DurationType.TIME, 600, 290, 310),
                new ActivityComparisonEngine.PlannedStep(12L, 1, DurationType.TIME, 600, 290, 310)));
        var actual = new ActivityComparisonEngine.Actual(1200.0, 4000.0, 160.0, 88.0, List.of(
                point(0, 0, 140, 84), point(600, 2000, 160, 88), point(1200, 4000, 170, 90)), List.of());

        var first = engine.compare(plan, actual);
        var second = engine.compare(plan, actual);

        assertEquals(100.0, first.percentage());
        assertEquals(ComplianceClassification.FULFILLED, first.classification());
        assertEquals(first, second);
        assertEquals(StepAlignmentSource.RECORDS, first.steps().get(0).source());
        assertEquals(300.0, first.steps().get(0).paceSecondsPerKm());
        assertTrue(first.explanation().contains(ComparisonTolerancePolicy.V1.version()));
    }

    @Test
    void fallsBackToLapsAndClassifiesShortExecutionAsPartial() {
        var plan = new ActivityComparisonEngine.Plan(null, 1000, List.of(
                new ActivityComparisonEngine.PlannedStep(11L, 1, DurationType.DISTANCE, 1000, null, null)));
        var actual = new ActivityComparisonEngine.Actual(210.0, 600.0, 150.0, 86.0, List.of(), List.of(
                new ActivityComparisonEngine.LapSegment(210.0, 600.0, 150.0, 86.0)));

        var result = engine.compare(plan, actual);

        assertEquals(ComplianceClassification.PARTIAL, result.classification());
        assertEquals(StepAlignmentSource.LAPS, result.steps().get(0).source());
        assertEquals(70.0, result.steps().get(0).percentage());
    }

    @Test
    void reportsExceededWhenAnIntervalPassesVersionedOverageTolerance() {
        var plan = new ActivityComparisonEngine.Plan(600, null, List.of(
                new ActivityComparisonEngine.PlannedStep(11L, 1, DurationType.TIME, 600, null, null)));
        var actual = new ActivityComparisonEngine.Actual(900.0, 3000.0, null, null, List.of(
                point(0, 0, null, null), point(900, 3000, null, null)), List.of());

        var result = engine.compare(plan, actual);

        assertEquals(ComplianceClassification.EXCEEDED, result.classification());
        assertEquals(ComplianceClassification.EXCEEDED, result.steps().get(0).classification());
    }

    private ActivityComparisonEngine.RecordPoint point(double seconds, double meters, Integer hr, Integer cadence) {
        return new ActivityComparisonEngine.RecordPoint(seconds, meters,
                hr == null ? null : hr.doubleValue(), cadence == null ? null : cadence.doubleValue());
    }
}
