package com.aicoach.backend.service;

import com.aicoach.backend.client.GarminBotClient;
import com.aicoach.backend.dto.GarminBotResponseDTO;
import com.aicoach.backend.models.Activity;
import com.aicoach.backend.models.Athlete;
import com.aicoach.backend.repository.AthleteRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.aicoach.backend.repository.ActivityRepo;

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
    public void rotinaDiariaDeSincronizacao() {
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
                .findFirst() // Pega o primeiro que encontrar com esse nome
                .ifPresent(dto -> {
                    Activity vdotTest = mapToEntity(dto, athlete);
                    vdotTest.setIsVdotTest(true); // Marca a flag de marco zero!
                    activityRepo.save(vdotTest);
                });
    }

    private void findActivities(Athlete athlete) {
        // 1. Busca a data do teste de 3km (marco zero) salvo no banco
        LocalDateTime dataMarcoZero = activityRepo.findByAthleteIdAndIsVdotTestTrue(athlete.getId())
                .map(Activity::getStartedAt)
                .orElse(null);

        // Se por algum motivo o marco zero sumiu, não prossegue
        if (dataMarcoZero == null) {
            return;
        }

        // Puxa só as recentes para economizar tempo (ex: últimas 10)
        List<GarminBotResponseDTO> activities = garminBotClient.fetchActivities(
                athlete.getGarminEmail(), athlete.getGarminPassword(), 10
        );

        // Filtra: DEPOIS do teste E que NÃO existem no banco e converte para entidade
        List<Activity> novasAtividades = activities.stream()
                .filter(dto -> dto.startedAt() != null && dto.startedAt().isAfter(dataMarcoZero))
                .filter(dto -> !activityRepo.existsByGarminActivityId(dto.activityId()))
                .map(dto -> {
                    Activity activity = mapToEntity(dto, athlete);
                    activity.setIsVdotTest(false);
                    return activity;
                })
                .toList();

        // Salva tudo de uma vez
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
        return activity;
    }
}
