package com.aicoach.backend.service;

import com.aicoach.backend.dto.ActivityComparisonResponse;
import com.aicoach.backend.enums.*;
import com.aicoach.backend.models.*;
import com.aicoach.backend.repository.ActivityComparisonRepo;
import com.aicoach.backend.repository.ActivityRepo;
import com.aicoach.backend.repository.PlannedActivityRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ActivityComparisonServiceTest {
    private ActivityRepo activityRepo;
    private PlannedActivityRepo plannedRepo;
    private ActivityComparisonRepo comparisonRepo;
    private ActivityComparisonService service;
    private Activity activity;

    @BeforeEach
    void setUp() {
        activityRepo = mock(ActivityRepo.class);
        plannedRepo = mock(PlannedActivityRepo.class);
        comparisonRepo = mock(ActivityComparisonRepo.class);
        service = new ActivityComparisonService(activityRepo, plannedRepo, comparisonRepo,
                Clock.fixed(Instant.parse("2026-09-19T18:00:00Z"), ZoneOffset.UTC));
        activity = actualActivity();
        when(activityRepo.findById(44L)).thenReturn(Optional.of(activity));
        when(comparisonRepo.findByActivityId(44L)).thenReturn(Optional.empty());
        when(comparisonRepo.save(any())).thenAnswer(invocation -> {
            ActivityComparison saved = invocation.getArgument(0);
            saved.setId(90L);
            return saved;
        });
    }

    @Test
    void pairsSameDayPlanPersistsExpandedStepsAndObservedSensors() {
        PlannedActivity planned = plannedActivity();
        when(plannedRepo.findComparisonCandidates(7L, LocalDate.of(2026, 9, 18),
                LocalDate.of(2026, 9, 20))).thenReturn(List.of(planned));
        when(comparisonRepo.findByPlannedActivityId(55L)).thenReturn(Optional.empty());

        ActivityComparisonResponse response = service.compareActivity(44L);

        assertEquals(ActivityMatchType.PLANNED, response.matchType());
        assertEquals(ComplianceClassification.FULFILLED, response.classification());
        assertEquals("100.00", response.compliancePercentage().toPlainString());
        assertEquals(2, response.steps().size());
        assertEquals(DurationType.TIME, response.steps().get(0).durationType());
        assertEquals(600, response.steps().get(0).plannedValue());
        assertEquals("300.00", response.metrics().plannedPaceSecondsPerKm().toPlainString());
        assertEquals("155.00", response.steps().get(0).actualAverageHeartRate().toPlainString());
        assertEquals("86.00", response.steps().get(0).actualAverageCadence().toPlainString());
        assertTrue(response.explanation().contains("não pontuadas sem alvos prescritos"));
        verify(comparisonRepo).save(argThat(item -> item.getSteps().size() == 2
                && item.getToleranceVersion().equals("activity-comparison-v1")));
    }

    @Test
    void marksRunAsUnplannedWhenNoCandidateExists() {
        when(plannedRepo.findComparisonCandidates(any(), any(), any())).thenReturn(List.of());

        ActivityComparisonResponse response = service.compareActivity(44L);

        assertEquals(ActivityMatchType.UNPLANNED, response.matchType());
        assertEquals(ComplianceClassification.DIFFERENT, response.classification());
        assertEquals("0.00", response.compliancePercentage().toPlainString());
        assertNull(response.plannedActivityId());
    }

    private Activity actualActivity() {
        Athlete athlete = new Athlete();
        athlete.setId(7L);
        Activity item = new Activity();
        item.setId(44L);
        item.setAthlete(athlete);
        item.setStartedAt(LocalDateTime.of(2026, 9, 19, 7, 0));
        item.setSport("running");
        item.setDurationSeconds(1200.0);
        item.setDistanceMeters(4000.0);
        item.setAverageHeartRate(160);
        item.setAvgCadence((short) 87);
        item.getRecords().add(record(item, 0, 0, 150, 84));
        item.getRecords().add(record(item, 600, 2000, 160, 88));
        item.getRecords().add(record(item, 1200, 4000, 170, 90));
        return item;
    }

    private ActivityRecord record(Activity activity, int seconds, double meters, int hr, int cadence) {
        ActivityRecord record = new ActivityRecord();
        record.setActivity(activity);
        record.setElapsedS((short) seconds);
        record.setDistanceKm(java.math.BigDecimal.valueOf(meters / 1000));
        record.setHeartRate((short) hr);
        record.setCadence((short) cadence);
        return record;
    }

    private PlannedActivity plannedActivity() {
        GlobalPlan global = new GlobalPlan();
        global.setAthlete(activity.getAthlete());
        TrainingCycle cycle = new TrainingCycle();
        cycle.setGlobalPlan(global);
        PlannedActivity planned = new PlannedActivity();
        planned.setId(55L);
        planned.setTrainingCycle(cycle);
        planned.setScheduledDate(LocalDate.of(2026, 9, 19));
        planned.setWorkoutType(WorkoutType.EASY_RUN);
        planned.setPlannedDurationSeconds(1200);
        planned.setPlannedDistanceMeters(4000);
        WorkoutBlock block = new WorkoutBlock();
        block.setPlannedActivity(planned);
        block.setBlockOrder(1);
        block.setIterations(2);
        WorkoutStep step = new WorkoutStep();
        step.setId(66L);
        step.setPlannedActivity(planned);
        step.setWorkoutBlock(block);
        step.setStepOrder(1);
        step.setStepType(StepType.WORK);
        step.setDurationType(DurationType.TIME);
        step.setDurationValue(600);
        step.setTargetZone(IntensityZone.E_PACE);
        step.setTargetPaceFastestSecondsPerKm(290);
        step.setTargetPaceSlowestSecondsPerKm(310);
        block.getSteps().add(step);
        planned.getWorkoutBlocks().add(block);
        return planned;
    }
}
