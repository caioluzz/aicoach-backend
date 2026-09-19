package com.aicoach.backend.service;

import com.aicoach.backend.client.GarminWorkoutGateway;
import com.aicoach.backend.dto.GarminDeliveryResponse;
import com.aicoach.backend.enums.*;
import com.aicoach.backend.models.*;
import com.aicoach.backend.repository.GarminWorkoutDeliveryRepo;
import com.aicoach.backend.repository.WeeklyPlanRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GarminWorkoutDeliveryServiceTest {
    private WeeklyPlanRepo weeklyPlanRepo;
    private GarminWorkoutDeliveryRepo deliveryRepo;
    private GarminWorkoutGateway gateway;
    private GarminWorkoutDeliveryService service;
    private WeeklyPlan plan;
    private List<GarminWorkoutDelivery> stored;

    @BeforeEach
    void setUp() {
        weeklyPlanRepo = mock(WeeklyPlanRepo.class);
        deliveryRepo = mock(GarminWorkoutDeliveryRepo.class);
        gateway = mock(GarminWorkoutGateway.class);
        service = new GarminWorkoutDeliveryService(weeklyPlanRepo, deliveryRepo, gateway,
                Clock.fixed(Instant.parse("2026-09-19T18:00:00Z"), ZoneOffset.UTC));
        plan = approvedPlan();
        stored = new ArrayList<>();
        when(weeklyPlanRepo.findByIdAndAthleteId(31L, 7L)).thenReturn(Optional.of(plan));
        when(deliveryRepo.findByAthleteIdAndPlannedActivityIdAndPlanVersionAndContentHash(
                anyLong(), anyLong(), anyInt(), anyString())).thenAnswer(invocation -> stored.stream().findFirst());
        when(deliveryRepo.saveAndFlush(any())).thenAnswer(invocation -> {
            GarminWorkoutDelivery item = invocation.getArgument(0);
            if (item.getId() == null) item.setId(91L);
            if (!stored.contains(item)) stored.add(item);
            return item;
        });
        when(deliveryRepo.findByAthleteIdAndWeeklyPlanIdOrderByPlannedActivitySessionOrder(7L, 31L))
                .thenAnswer(invocation -> List.copyOf(stored));
        when(deliveryRepo.findByIdAndAthleteIdAndWeeklyPlanId(91L, 7L, 31L))
                .thenAnswer(invocation -> stored.stream().findFirst());
        when(gateway.preview(any())).thenReturn(new GarminWorkoutGateway.Preview(
                "a".repeat(64), "a".repeat(64), LocalDate.of(2026, 10, 6), Map.of("workoutName", "Easy")));
        when(gateway.deliver(any(), anyString(), any())).thenReturn(new GarminWorkoutGateway.Delivery(
                101L, 201L, LocalDate.of(2026, 10, 6), "b".repeat(64), false));
    }

    @Test
    void rejectsAnyPlanThatIsNotApprovedBeforeCallingGarmin() {
        plan.setStatus(WeeklyPlanStatus.VALIDATED);
        assertThrows(WeeklyPlanStateException.class, () -> service.deliver(7L, 31L));
        verifyNoInteractions(gateway);
    }

    @Test
    void previewsNeutralContractWithoutReturningCredentials() {
        var preview = service.preview(7L, 31L);
        assertEquals("a".repeat(64), preview.sessions().get(0).contentHash());
        verify(gateway).preview(argThat(contract -> contract.schemaVersion().equals("workout.v1")
                && contract.blocks().get(0).steps().get(0).kind() == StepType.WARMUP));
        assertFalse(preview.toString().contains("secret"));
    }

    @Test
    void deliversIdempotentlyAndConfirmsWholePlan() {
        GarminDeliveryResponse first = service.deliver(7L, 31L);
        GarminDeliveryResponse repeated = service.deliver(7L, 31L);

        assertEquals(GarminDeliveryStatus.SCHEDULED, first.deliveries().get(0).status());
        assertEquals(101L, repeated.deliveries().get(0).externalWorkoutId());
        verify(gateway, times(1)).deliver(any(), anyString(), any());

        when(gateway.confirm(any(), eq(101L), eq(201L), any(), anyString()))
                .thenReturn(new GarminWorkoutGateway.Confirmation(true, 101L, 201L,
                        LocalDate.of(2026, 10, 6)));
        GarminDeliveryResponse confirmed = service.confirm(7L, 31L);

        assertEquals(GarminDeliveryStatus.CONFIRMED, confirmed.deliveries().get(0).status());
        assertEquals(WeeklyPlanStatus.DELIVERED, plan.getStatus());
    }

    @Test
    void updatesAndCancelsSupportedExternalWorkout() {
        service.deliver(7L, 31L);
        when(gateway.update(any(), eq(101L), eq(201L), anyString(), any()))
                .thenReturn(new GarminWorkoutGateway.Delivery(101L, 202L,
                        LocalDate.of(2026, 10, 6), "b".repeat(64), null));

        GarminDeliveryResponse updated = service.update(7L, 31L, 91L);
        GarminDeliveryResponse cancelled = service.cancel(7L, 31L, 91L);

        assertEquals(202L, updated.deliveries().get(0).externalScheduleId());
        assertEquals(GarminDeliveryStatus.CANCELLED, cancelled.deliveries().get(0).status());
        verify(gateway).cancel(any(), eq(101L), eq(202L));
        assertEquals(WeeklyPlanStatus.APPROVED, plan.getStatus());
    }

    @Test
    void persistsSanitizedFailureForRetry() {
        when(gateway.deliver(any(), anyString(), any())).thenThrow(new RuntimeException("token=secret"));

        assertThrows(GarminDeliveryException.class, () -> service.deliver(7L, 31L));
        assertEquals(GarminDeliveryStatus.FAILED, stored.get(0).getStatus());
        assertEquals("Falha temporária na integração Garmin", stored.get(0).getLastError());
        assertFalse(stored.get(0).getLastError().contains("secret"));
    }

    private WeeklyPlan approvedPlan() {
        Athlete athlete = new Athlete();
        athlete.setId(7L);
        athlete.setGarminEmail("runner@example.test");
        athlete.setGarminPassword("secret");
        WeeklyPlan result = new WeeklyPlan();
        result.setId(31L);
        result.setAthlete(athlete);
        result.setVersion(3);
        result.setStatus(WeeklyPlanStatus.APPROVED);
        PlannedActivity activity = new PlannedActivity();
        activity.setId(51L);
        activity.setWeeklyPlan(result);
        activity.setSessionOrder(1);
        activity.setName("Intervalado");
        activity.setScheduledDate(LocalDate.of(2026, 10, 6));
        activity.setWorkoutType(WorkoutType.QUALITY_1);
        WorkoutBlock block = new WorkoutBlock();
        block.setId(61L);
        block.setPlannedActivity(activity);
        block.setBlockOrder(1);
        block.setIterations(2);
        WorkoutStep step = new WorkoutStep();
        step.setId(71L);
        step.setPlannedActivity(activity);
        step.setWorkoutBlock(block);
        step.setStepOrder(1);
        step.setStepType(StepType.WARMUP);
        step.setDurationType(DurationType.TIME);
        step.setDurationValue(600);
        step.setTargetZone(IntensityZone.E_PACE);
        step.setTargetPaceFastestSecondsPerKm(330);
        step.setTargetPaceSlowestSecondsPerKm(360);
        step.setInstruction("Leve");
        block.setSteps(new ArrayList<>(List.of(step)));
        activity.setWorkoutBlocks(new ArrayList<>(List.of(block)));
        result.setSessions(new ArrayList<>(List.of(activity)));
        return result;
    }
}
