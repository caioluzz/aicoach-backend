package com.aicoach.backend.service;

import com.aicoach.backend.client.GarminBotClient;
import com.aicoach.backend.dto.ActivitySyncResponse;
import com.aicoach.backend.dto.ActivitySummaryResponse;
import com.aicoach.backend.dto.GarminActivityMetadata;
import com.aicoach.backend.dto.GarminBotResponseDTO;
import com.aicoach.backend.dto.VdotTestConfirmationResponse;
import com.aicoach.backend.enums.ActivitySyncStatus;
import com.aicoach.backend.models.*;
import com.aicoach.backend.repository.ActivityRepo;
import com.aicoach.backend.repository.ActivitySyncStateRepo;
import com.aicoach.backend.repository.AthleteMetricsRepo;
import com.aicoach.backend.repository.AthleteRepo;
import com.aicoach.backend.training.daniels.DanielsIntensity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class ActivityService {
    private static final Logger log = LoggerFactory.getLogger(ActivityService.class);

    private final ActivityRepo activityRepo;
    private final AthleteRepo athleteRepo;
    private final ActivitySyncStateRepo syncStateRepo;
    private final AthleteMetricsRepo athleteMetricsRepo;
    private final GarminBotClient garminBotClient;
    private final ActivityComparisonService activityComparisonService;
    private final Clock clock;
    private final com.aicoach.backend.training.daniels.DanielsPerformanceEngine performanceEngine;
    private final AtomicBoolean allAthletesSyncRunning = new AtomicBoolean(false);
    private final Set<Long> activeAthleteSyncs = ConcurrentHashMap.newKeySet();

    @Value("${garmin.sync.discovery-limit:100}")
    private int discoveryLimit = 100;
    @Value("${garmin.sync.recovery-window-hours:24}")
    private long recoveryWindowHours = 24;
    @Value("${garmin.sync.max-attempts:3}")
    private int maxAttempts = 3;
    @Value("${garmin.sync.retry-delay-ms:500}")
    private long retryDelayMs = 500;

    @Transactional
    public Activity saveActivity(Activity activity) {
        if (activity.getGarminActivityId() != null
                && activityRepo.existsByGarminActivityId(activity.getGarminActivityId())) {
            throw new IllegalArgumentException("Atividade do Garmin já registrada no sistema.");
        }
        Activity saved = activityRepo.saveAndFlush(activity);
        compareIfRunning(saved);
        return saved;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        log.info("activity_sync trigger=startup status=started");
        syncAllAthletes();
    }

    @Scheduled(
            fixedDelayString = "${garmin.sync.fixed-delay-ms:7200000}",
            initialDelayString = "${garmin.sync.initial-delay-ms:7200000}"
    )
    public void scheduledSync() {
        log.info("activity_sync trigger=schedule status=started");
        syncAllAthletes();
    }

    public List<ActivitySyncResponse> syncAllAthletes() {
        if (!allAthletesSyncRunning.compareAndSet(false, true)) {
            log.info("activity_sync scope=all status=skipped reason=already_running");
            return getSyncStatuses();
        }
        try {
            return athleteRepo.findAll().stream()
                    .map(athlete -> syncGarminActivities(athlete.getId()))
                    .toList();
        } finally {
            allAthletesSyncRunning.set(false);
        }
    }

    public ActivitySyncResponse syncGarminActivities(Long athleteId) {
        Athlete athlete = athleteRepo.findById(athleteId)
                .orElseThrow(() -> new AthleteNotFoundException(athleteId));
        if (!activeAthleteSyncs.add(athleteId)) {
            log.info("activity_sync athlete_id={} status=skipped reason=already_running", athleteId);
            return getSyncStatus(athleteId);
        }

        ActivitySyncState state = syncStateRepo.findByAthleteId(athleteId).orElseGet(() -> {
            ActivitySyncState created = new ActivitySyncState();
            created.setAthlete(athlete);
            return created;
        });
        state.setStatus(ActivitySyncStatus.RUNNING);
        state.setLastAttemptAt(LocalDateTime.now());
        state.setLastError(null);
        state.setDiscoveredCount(0);
        state.setImportedCount(0);
        state.setSkippedCount(0);
        state = syncStateRepo.save(state);

        try {
            LocalDateTime since = state.getCheckpointStartedAt() == null
                    ? null
                    : state.getCheckpointStartedAt().minusHours(recoveryWindowHours);
            List<GarminActivityMetadata> discovered = retry(
                    () -> garminBotClient.discoverActivities(
                            athlete.getGarminEmail(), athlete.getGarminPassword(), discoveryLimit, since),
                    "discover",
                    athleteId);
            if (discovered == null) {
                discovered = List.of();
            }
            discovered = discovered.stream()
                    .filter(item -> item.activityId() != null)
                    .sorted(Comparator.comparing(
                            GarminActivityMetadata::startedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();

            int imported = 0;
            int skipped = 0;
            List<Long> failedActivityIds = new ArrayList<>();
            for (GarminActivityMetadata metadata : discovered) {
                if (activityRepo.existsByGarminActivityId(metadata.activityId())) {
                    activityRepo.findByGarminActivityId(metadata.activityId())
                            .filter(this::isRunning)
                            .filter(activity -> !activityComparisonService.hasComparison(activity.getId()))
                            .ifPresent(this::compareIfRunning);
                    skipped++;
                    continue;
                }
                try {
                    GarminBotResponseDTO detail = retry(
                            () -> garminBotClient.downloadActivity(
                                    athlete.getGarminEmail(), athlete.getGarminPassword(), metadata.activityId()),
                            "download",
                            athleteId);
                    if (detail == null || !metadata.activityId().equals(detail.activityId())) {
                        throw new IllegalStateException("Resposta Garmin não corresponde à atividade solicitada");
                    }
                    if (!activityRepo.existsByGarminActivityId(metadata.activityId())) {
                        Activity importedActivity = activityRepo.saveAndFlush(mapToEntity(detail, athlete));
                        compareIfRunning(importedActivity);
                        imported++;
                    } else {
                        skipped++;
                    }
                } catch (RuntimeException exception) {
                    failedActivityIds.add(metadata.activityId());
                    log.warn("activity_sync athlete_id={} activity_id={} status=failed error_type={}",
                            athleteId, metadata.activityId(), exception.getClass().getSimpleName());
                }
            }

            state.setDiscoveredCount(discovered.size());
            state.setImportedCount(imported);
            state.setSkippedCount(skipped);
            if (!failedActivityIds.isEmpty()) {
                state.setStatus(ActivitySyncStatus.FAILED);
                state.setLastError("Falha ao importar atividades: " + failedActivityIds);
            } else {
                LocalDateTime newest = discovered.stream()
                        .map(GarminActivityMetadata::startedAt)
                        .filter(value -> value != null)
                        .max(LocalDateTime::compareTo)
                        .orElse(state.getCheckpointStartedAt());
                if (newest != null && (state.getCheckpointStartedAt() == null
                        || newest.isAfter(state.getCheckpointStartedAt()))) {
                    state.setCheckpointStartedAt(newest);
                }
                state.setStatus(ActivitySyncStatus.SUCCESS);
                state.setLastSuccessAt(LocalDateTime.now());
                state.setLastError(null);
            }
            state = syncStateRepo.save(state);
            log.info("activity_sync athlete_id={} status={} discovered={} imported={} skipped={} failed={}",
                    athleteId, state.getStatus(), discovered.size(), imported, skipped, failedActivityIds.size());
            return toResponse(state);
        } catch (RuntimeException exception) {
            state.setStatus(ActivitySyncStatus.FAILED);
            state.setLastError("Falha na descoberta: " + exception.getClass().getSimpleName());
            state = syncStateRepo.save(state);
            log.warn("activity_sync athlete_id={} status=failed phase=discover error_type={}",
                    athleteId, exception.getClass().getSimpleName());
            return toResponse(state);
        } finally {
            activeAthleteSyncs.remove(athleteId);
        }
    }

    public List<ActivitySyncResponse> getSyncStatuses() {
        return athleteRepo.findAll().stream()
                .map(athlete -> getSyncStatus(athlete.getId()))
                .toList();
    }

    public ActivitySyncResponse getSyncStatus(Long athleteId) {
        if (!athleteRepo.existsById(athleteId)) {
            throw new AthleteNotFoundException(athleteId);
        }
        return syncStateRepo.findByAthleteId(athleteId)
                .map(this::toResponse)
                .orElse(new ActivitySyncResponse(
                        athleteId, ActivitySyncStatus.NEVER_RUN, null, null, null, 0, 0, 0, null));
    }

    @Transactional
    public List<ActivitySummaryResponse> listActivities(Long athleteId) {
        if (!athleteRepo.existsById(athleteId)) throw new AthleteNotFoundException(athleteId);
        return activityRepo.findByAthleteIdOrderByStartedAtDesc(athleteId).stream()
                .map(activity -> new ActivitySummaryResponse(activity.getId(), athleteId,
                        activity.getGarminActivityId(), activity.getName(), activity.getStartedAt(),
                        activity.getEndedAt(), activity.getDistanceMeters(), activity.getDurationSeconds(),
                        activity.getSport(), activity.getIsVdotTest(), activity.getAvgPaceSPerKm(),
                        activity.getAverageHeartRate(), activity.getAvgCadence()))
                .toList();
    }

    @Transactional
    public VdotTestConfirmationResponse confirmVdotTest(Long athleteId, Long activityId) {
        Activity activity = activityRepo.findByIdAndAthleteId(activityId, athleteId)
                .orElseThrow(() -> new IllegalArgumentException("Atividade não pertence ao atleta"));
        if (activity.getSport() == null || !activity.getSport().toLowerCase().contains("run")) {
            throw new IllegalArgumentException("Somente uma atividade de corrida pode ser confirmada como teste");
        }
        if (activity.getDistanceMeters() == null || activity.getDurationSeconds() == null) {
            throw new IllegalArgumentException("A atividade não possui distância e duração suficientes");
        }
        int distance = (int) Math.round(activity.getDistanceMeters());
        int duration = (int) Math.round(activity.getDurationSeconds());
        var profile = performanceEngine.fromThreeKmTest(distance, duration);
        activityRepo.findByAthleteIdAndIsVdotTestTrue(athleteId).ifPresent(previous -> {
            previous.setIsVdotTest(false);
            activityRepo.save(previous);
        });
        activity.setIsVdotTest(true);
        activityRepo.save(activity);

        Athlete athlete = athleteRepo.findById(athleteId)
                .orElseThrow(() -> new AthleteNotFoundException(athleteId));
        AthleteMetrics metrics = new AthleteMetrics();
        metrics.setAthlete(athlete);
        metrics.setRecordedAt(LocalDateTime.ofInstant(profile.calculatedAt(), clock.getZone()));
        metrics.setVdot(profile.vdot());
        metrics.setEasyPaceSec(profile.paces().get(DanielsIntensity.E).slowestSecondsPerKm());
        metrics.setMarathonPaceSec(profile.paces().get(DanielsIntensity.M).slowestSecondsPerKm());
        metrics.setThresholdPaceSec(profile.paces().get(DanielsIntensity.T).slowestSecondsPerKm());
        metrics.setIntervalPaceSec(profile.paces().get(DanielsIntensity.I).slowestSecondsPerKm());
        metrics.setRepetitionPaceSec(profile.paces().get(DanielsIntensity.R).slowestSecondsPerKm());
        athleteMetricsRepo.save(metrics);

        Map<String, VdotTestConfirmationResponse.PaceRange> paces = new LinkedHashMap<>();
        profile.paces().forEach((intensity, pace) -> paces.put(intensity.name(),
                new VdotTestConfirmationResponse.PaceRange(pace.fastestSecondsPerKm(), pace.slowestSecondsPerKm())));
        return new VdotTestConfirmationResponse(activityId, athleteId, distance, duration,
                new VdotTestConfirmationResponse.PerformanceProfile(profile.vdot(), profile.engineVersion(),
                        profile.coefficientSet(), profile.calculatedAt(), paces));
    }

    private <T> T retry(Supplier<T> operation, String phase, Long athleteId) {
        RuntimeException lastFailure = null;
        int attempts = Math.max(1, maxAttempts);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return operation.get();
            } catch (RuntimeException exception) {
                lastFailure = exception;
                log.warn("activity_sync athlete_id={} phase={} attempt={} max_attempts={} error_type={}",
                        athleteId, phase, attempt, attempts, exception.getClass().getSimpleName());
                if (attempt < attempts && retryDelayMs > 0) {
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Retry de sincronização interrompido", interrupted);
                    }
                }
            }
        }
        throw lastFailure == null ? new IllegalStateException("Falha desconhecida") : lastFailure;
    }

    private ActivitySyncResponse toResponse(ActivitySyncState state) {
        return new ActivitySyncResponse(
                state.getAthlete().getId(), state.getStatus(), state.getCheckpointStartedAt(),
                state.getLastAttemptAt(), state.getLastSuccessAt(), state.getDiscoveredCount(),
                state.getImportedCount(), state.getSkippedCount(), state.getLastError());
    }

    private Activity mapToEntity(GarminBotResponseDTO dto, Athlete athlete) {
        Activity activity = new Activity();
        activity.setAthlete(athlete);
        activity.setGarminActivityId(dto.activityId());
        activity.setName(dto.activityName());
        activity.setDistanceMeters(dto.distanceMeters());
        activity.setDurationSeconds(dto.durationSeconds());
        activity.setStartedAt(dto.startedAt());
        activity.setAverageHeartRate(dto.averageHeartRate());
        activity.setAverageSpeed(dto.averageSpeed());
        activity.setSport(dto.sport());
        activity.setSubSport(dto.subSport());
        activity.setIsVdotTest(Boolean.TRUE.equals(dto.isVdotTest()));
        activity.setEndedAt(dto.endedAt());
        activity.setMaxSpeedKmh(dto.maxSpeedKmh());
        activity.setAvgPaceSPerKm(dto.avgPaceSPerKm());
        activity.setBestPaceSPerKm(dto.bestPaceSPerKm());
        activity.setMaxHr(dto.maxHr());
        activity.setAvgCadence(dto.avgCadence());
        activity.setMaxCadence(dto.maxCadence());
        activity.setElevationGainM(dto.elevationGainM());
        activity.setElevationLossM(dto.elevationLossM());
        activity.setMinAltitudeM(dto.minAltitudeM());
        activity.setMaxAltitudeM(dto.maxAltitudeM());
        activity.setLapCount(dto.lapCount());
        activity.setRecordCount(dto.recordCount());
        activity.setRawFilePath(dto.rawFilePath());

        if (dto.laps() != null) {
            dto.laps().forEach(lapDTO -> {
                Lap lap = new Lap();
                lap.setActivity(activity);
                lap.setLapNumber(lapDTO.lapNumber());
                lap.setLapType(lapDTO.lapType());
                lap.setStartTime(lapDTO.startTime());
                lap.setDurationS(lapDTO.durationS());
                lap.setDistanceKm(lapDTO.distanceKm());
                lap.setAvgPaceSPerKm(lapDTO.avgPaceSPerKm());
                lap.setAvgSpeedKmh(lapDTO.avgSpeedKmh());
                lap.setAvgHr(lapDTO.avgHr());
                lap.setMaxHr(lapDTO.maxHr());
                lap.setAvgCadence(lapDTO.avgCadence());
                lap.setMaxCadence(lapDTO.maxCadence());
                lap.setAscentM(lapDTO.ascentM());
                lap.setDescentM(lapDTO.descentM());
                activity.getLaps().add(lap);
            });
        }
        if (dto.records() != null) {
            dto.records().forEach(recordDTO -> {
                ActivityRecord record = new ActivityRecord();
                record.setActivity(activity);
                ActivityRecordId recordId = new ActivityRecordId();
                recordId.setTs(recordDTO.ts());
                record.setId(recordId);
                if (recordDTO.elapsedS() != null) {
                    record.setElapsedS(recordDTO.elapsedS().shortValue());
                }
                record.setDistanceKm(recordDTO.distanceKm());
                record.setSpeedKmh(recordDTO.speedKmh());
                record.setPaceSPerKm(recordDTO.paceSPerKm());
                record.setHeartRate(recordDTO.heartRate());
                record.setCadence(recordDTO.cadence());
                record.setAltitudeM(recordDTO.altitudeM());
                activity.getRecords().add(record);
            });
        }
        return activity;
    }

    private void compareIfRunning(Activity activity) {
        if (!isRunning(activity)) return;
        try {
            activityComparisonService.compareActivity(activity.getId());
        } catch (RuntimeException exception) {
            log.warn("activity_comparison activity_id={} status=failed error_type={}",
                    activity.getId(), exception.getClass().getSimpleName());
        }
    }

    private boolean isRunning(Activity activity) {
        return activity.getSport() != null && activity.getSport().toLowerCase().contains("run");
    }
}
