package com.aicoach.backend.service;

import com.aicoach.backend.client.GarminWorkoutGateway;
import com.aicoach.backend.dto.GarminWorkoutContract;
import com.aicoach.backend.dto.VdotTestWorkoutResponse;
import com.aicoach.backend.enums.DurationType;
import com.aicoach.backend.enums.IntensityZone;
import com.aicoach.backend.enums.StepType;
import com.aicoach.backend.models.Athlete;
import com.aicoach.backend.repository.AthleteRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VdotTestWorkoutService {
    private static final String WORKOUT_NAME = "Teste de 3 km — AI RunCoach";

    private final AthleteRepo athleteRepo;
    private final GarminWorkoutGateway gateway;
    private final Clock clock;

    @Transactional(readOnly = true)
    public VdotTestWorkoutResponse preview(Long athleteId) {
        Athlete athlete = athlete(athleteId);
        LocalDate date = scheduledDate();
        GarminWorkoutContract workout = workout(date);
        GarminWorkoutGateway.Preview preview = gateway.preview(workout);
        return new VdotTestWorkoutResponse(athleteId, workout.name(), date,
                idempotencyKey(athleteId, date), preview.payload(), null);
    }

    @Transactional
    public VdotTestWorkoutResponse deliver(Long athleteId) {
        Athlete athlete = athlete(athleteId);
        if (athlete.getGarminEmail() == null || athlete.getGarminEmail().isBlank()
                || athlete.getGarminPassword() == null || athlete.getGarminPassword().isBlank()) {
            throw new WeeklyPlanPrerequisiteException("O atleta precisa configurar credenciais Garmin antes do teste");
        }
        LocalDate date = scheduledDate();
        String key = idempotencyKey(athleteId, date);
        GarminWorkoutGateway.Delivery delivery = gateway.deliver(
                new GarminWorkoutGateway.Credentials(athlete.getGarminEmail(), athlete.getGarminPassword()),
                key, workout(date));
        return new VdotTestWorkoutResponse(athleteId, WORKOUT_NAME, date, key, null,
                new VdotTestWorkoutResponse.Delivery(delivery.workoutId(), delivery.scheduledWorkoutId(),
                        delivery.reusedWorkout()));
    }

    private Athlete athlete(Long athleteId) {
        return athleteRepo.findById(athleteId)
                .orElseThrow(() -> new AthleteNotFoundException(athleteId));
    }

    private LocalDate scheduledDate() {
        return LocalDate.now(clock).plusDays(1);
    }

    private GarminWorkoutContract workout(LocalDate date) {
        return new GarminWorkoutContract(GarminWorkoutContract.SCHEMA_VERSION, WORKOUT_NAME, date, List.of(
                new GarminWorkoutContract.Block(1, 1, List.of(
                        step(1, StepType.WARMUP, DurationType.TIME, 600, IntensityZone.E_PACE,
                                "Aquecimento leve; termine pronto para correr forte."))),
                new GarminWorkoutContract.Block(2, 1, List.of(
                        step(1, StepType.WORK, DurationType.DISTANCE, 3_000, IntensityZone.REST,
                                "Teste máximo controlado de 3 km. Registre o tempo final."))),
                new GarminWorkoutContract.Block(3, 1, List.of(
                        step(1, StepType.COOLDOWN, DurationType.TIME, 600, IntensityZone.E_PACE,
                                "Desaquecimento confortável.")))
        ));
    }

    private GarminWorkoutContract.Step step(int order, StepType kind, DurationType durationType,
                                            int durationValue, IntensityZone zone, String instruction) {
        return new GarminWorkoutContract.Step(order, kind, durationType, durationValue, zone,
                null, null, instruction);
    }

    private String idempotencyKey(Long athleteId, LocalDate date) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String source = "vdot-test:" + athleteId + ":" + date;
            return HexFormat.of().formatHex(digest.digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
