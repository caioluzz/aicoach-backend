package com.aicoach.backend.service;

import com.aicoach.backend.client.GarminBotClient;
import com.aicoach.backend.dto.GarminBotResponseDTO;
import com.aicoach.backend.models.*;
import com.aicoach.backend.repository.AthleteRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.aicoach.backend.repository.ActivityRepo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepo activityRepo;
    private final AthleteRepo athleteRepo;
    private final GarminBotClient garminBotClient;

    @Transactional
    public Activity saveActivity(Activity activity) {
        if (activity.getGarminActivityId() != null &&
                activityRepo.existsByGarminActivityId(activity.getGarminActivityId())) {
            throw new IllegalArgumentException("[ActivityService] Atividade do Garmin já registrada no sistema.");
        }

        return activityRepo.save(activity);
    }

    @Scheduled(fixedDelay = 60000)
    public void dailySyncRoutine() {
        System.out.println("[ActivityService] Iniciando rotina de sincronização automática com a Garmin...");

        List<Athlete> athletes = athleteRepo.findAll();

        for (Athlete athlete : athletes) {
            try {
                syncGarminActivities(athlete.getId());
                System.out.println("[ActivityService] Sincronização do atleta " + athlete.getName() + " concluída.");
            } catch (Exception e) {
                System.err.println("[ActivityService] Erro ao sincronizar atleta ID " + athlete.getId() + ": " + e.getMessage());
            }
        }
    }

    public void syncGarminActivities(Long athleteId) {
        Athlete athlete = athleteRepo.findById(athleteId)
                .orElseThrow(() -> new RuntimeException("[ActivityService] Atleta não encontrado"));

        boolean isFirstSync = activityRepo.countByAthleteId(athleteId) == 0;

        if (isFirstSync) {
            findVdotTest(athlete);
        } else {
            findActivities(athlete);
        }


    }

    private void findVdotTest(Athlete athlete) {
        List<GarminBotResponseDTO> activities = garminBotClient.fetchActivities(
                athlete.getGarminEmail(), athlete.getGarminPassword(), 50
        );

        activities.stream()
                .filter(dto -> dto.activityName().toLowerCase().contains("teste 3km") || dto.activityName().toLowerCase().contains("vdot"))
                .findFirst()
                .ifPresent(dto -> {
                    Activity vdotTest = mapToEntity(dto, athlete);
                    vdotTest.setIsVdotTest(true);
                    activityRepo.save(vdotTest);
                });
    }

    private void findActivities(Athlete athlete) {
        LocalDateTime dataMarcoZero = activityRepo.findByAthleteIdAndIsVdotTestTrue(athlete.getId())
                .map(Activity::getStartedAt)
                .orElse(null);

        if (dataMarcoZero == null) {
            return;
        }

        List<GarminBotResponseDTO> activities = garminBotClient.fetchActivities(
                athlete.getGarminEmail(), athlete.getGarminPassword(), 10
        );

        List<Activity> novasAtividades = activities.stream()
                .filter(dto -> dto.startedAt() != null && dto.startedAt().isAfter(dataMarcoZero))
                .filter(dto -> !activityRepo.existsByGarminActivityId(dto.activityId()))
                .map(dto -> {
                    Activity activity = mapToEntity(dto, athlete);
                    activity.setIsVdotTest(false);
                    return activity;
                })
                .toList();

        if (!novasAtividades.isEmpty()) {
            activityRepo.saveAll(novasAtividades);
        }
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

        if (dto.isVdotTest() != null) {
            activity.setIsVdotTest(dto.isVdotTest());
        }

        activity.setEndedAt(dto.endedAt());
        // Removidas as linhas duplicadas de duration, avgHr, etc.
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

        if (dto.laps() != null && !dto.laps().isEmpty()) {
            List<Lap> laps = dto.laps().stream().map(lapDTO -> {
                Lap lap = new Lap();
                lap.setActivity(activity);
                lap.setLapNumber(lapDTO.lapNumber());
                lap.setStartTime(lapDTO.startTime());

                if (lapDTO.durationS() != null) {
                    lap.setDurationS(lapDTO.durationS());
                }

                lap.setDistanceKm(lapDTO.distanceKm());
                lap.setAvgPaceSPerKm(lapDTO.avgPaceSPerKm());
                lap.setAvgSpeedKmh(lapDTO.avgSpeedKmh());
                lap.setAvgHr(lapDTO.avgHr());
                lap.setMaxHr(lapDTO.maxHr());
                lap.setAvgCadence(lapDTO.avgCadence());
                lap.setMaxCadence(lapDTO.maxCadence());
                lap.setAscentM(lapDTO.ascentM());
                lap.setDescentM(lapDTO.descentM());

                return lap;
            }).toList();

            activity.getLaps().addAll(laps);
        }

        if (dto.records() != null && !dto.records().isEmpty()) {
            List<ActivityRecord> records = dto.records().stream().map(recordDTO -> {
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

                return record;
            }).toList();

            activity.getRecords().addAll(records);
        }

        return activity;
    }
}
