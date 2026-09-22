package com.aicoach.backend.service;

import com.aicoach.backend.dto.AthleteAssessmentRequest;
import com.aicoach.backend.dto.AthleteAssessmentResponse;
import com.aicoach.backend.enums.HealthIssueStatus;
import com.aicoach.backend.models.*;
import com.aicoach.backend.repository.AthleteAssessmentRepo;
import com.aicoach.backend.repository.AthleteRepo;
import com.aicoach.backend.repository.ActivityRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AthleteAssessmentService {

    private final AthleteRepo athleteRepo;
    private final AthleteAssessmentRepo assessmentRepo;
    private final ActivityRepo activityRepo;

    @Transactional
    public AthleteAssessmentResponse createVersion(Long athleteId, AthleteAssessmentRequest request) {
        Athlete athlete = athleteRepo.findByIdForUpdate(athleteId)
                .orElseThrow(() -> new AthleteNotFoundException(athleteId));
        validateConsistency(request);

        int nextVersion = assessmentRepo.findTopByAthleteIdOrderByVersionDesc(athleteId)
                .map(latest -> latest.getVersion() + 1)
                .orElse(1);

        AthleteAssessment assessment = mapRequest(athlete, nextVersion, request);
        synchronizeAthleteProfile(athlete, request);
        return toResponse(assessmentRepo.save(assessment));
    }

    @Transactional
    public AthleteAssessmentResponse completeOnboarding(Long athleteId) {
        Athlete athlete = athleteRepo.findByIdForUpdate(athleteId)
                .orElseThrow(() -> new AthleteNotFoundException(athleteId));
        AthleteAssessment assessment = assessmentRepo.findTopByAthleteIdOrderByVersionDesc(athleteId)
                .orElseThrow(() -> new AssessmentNotFoundException(athleteId));
        if (athlete.getGarminEmail() == null || athlete.getGarminEmail().isBlank()
                || athlete.getGarminPassword() == null || athlete.getGarminPassword().isBlank()) {
            throw new AssessmentValidationException("Configure o Garmin antes de concluir o onboarding");
        }
        if (activityRepo.findByAthleteIdAndIsVdotTestTrue(athleteId).isEmpty()) {
            throw new AssessmentValidationException("Confirme a atividade do teste de 3 km antes de concluir o onboarding");
        }
        assessment.setOnboardingStatus(com.aicoach.backend.enums.OnboardingStatus.COMPLETED);
        if (assessment.getCompletedAt() == null) assessment.setCompletedAt(Instant.now());
        return toResponse(assessmentRepo.save(assessment));
    }

    @Transactional(readOnly = true)
    public AthleteAssessmentResponse getLatest(Long athleteId) {
        ensureAthleteExists(athleteId);
        return assessmentRepo.findTopByAthleteIdOrderByVersionDesc(athleteId)
                .map(this::toResponse)
                .orElseThrow(() -> new AssessmentNotFoundException(athleteId));
    }

    @Transactional(readOnly = true)
    public List<AthleteAssessmentResponse> getHistory(Long athleteId) {
        ensureAthleteExists(athleteId);
        return assessmentRepo.findByAthleteIdOrderByVersionDesc(athleteId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void ensureAthleteExists(Long athleteId) {
        if (!athleteRepo.existsById(athleteId)) {
            throw new AthleteNotFoundException(athleteId);
        }
    }

    private void validateConsistency(AthleteAssessmentRequest request) {
        List<DayOfWeek> days = request.availability().stream()
                .map(AthleteAssessmentRequest.Availability::dayOfWeek)
                .toList();
        if (new HashSet<>(days).size() != days.size()) {
            throw new AssessmentValidationException("Cada dia de disponibilidade deve aparecer apenas uma vez");
        }
        if (!days.contains(request.preferredLongRunDay())) {
            throw new AssessmentValidationException("O dia preferido para o longo deve estar entre os dias disponíveis");
        }
        if (request.health().hasMedicalRestrictions()
                && (request.health().medicalRestrictions() == null
                || request.health().medicalRestrictions().isBlank())) {
            throw new AssessmentValidationException("Descreva as restrições médicas informadas");
        }
        boolean activeIssueWithoutPain = request.health().issues().stream()
                .anyMatch(issue -> issue.status() == HealthIssueStatus.ACTIVE && issue.painSeverity() == 0);
        if (activeIssueWithoutPain) {
            throw new AssessmentValidationException("Uma dor ativa deve informar intensidade entre 1 e 10");
        }
    }

    private AthleteAssessment mapRequest(Athlete athlete, int version, AthleteAssessmentRequest request) {
        AthleteAssessment assessment = new AthleteAssessment();
        assessment.setAthlete(athlete);
        assessment.setVersion(version);
        assessment.setOnboardingStatus(request.onboardingStatus());

        var physical = request.physicalProfile();
        assessment.setDateOfBirth(physical.dateOfBirth());
        assessment.setWeightKg(physical.weightKg());
        assessment.setHeightCm(physical.heightCm());
        assessment.setGender(physical.gender());

        var running = request.runningProfile();
        assessment.setExperienceLevel(running.experienceLevel());
        assessment.setRunningYears(running.runningYears());
        assessment.setCurrentWeeklyVolumeKm(running.currentWeeklyVolumeKm());
        assessment.setRecentAverageWeeklyVolumeKm(running.recentAverageWeeklyVolumeKm());
        assessment.setRecentLongestRunKm(running.recentLongestRunKm());
        assessment.setCurrentRunsPerWeek(running.currentRunsPerWeek());

        assessment.setPreferredLongRunDay(request.preferredLongRunDay());
        assessment.setSurfaces(new HashSet<>(request.surfaces()));
        assessment.setEquipment(new HashSet<>(request.equipment()));
        assessment.setStrengthSessionsPerWeek(request.strengthTraining().sessionsPerWeek());
        assessment.setStrengthTrainingNotes(request.strengthTraining().notes());

        assessment.setHasMedicalRestrictions(request.health().hasMedicalRestrictions());
        assessment.setMedicalRestrictions(request.health().medicalRestrictions());

        var recovery = request.recovery();
        assessment.setAverageSleepHours(recovery.averageSleepHours());
        assessment.setSleepQuality(recovery.sleepQuality());
        assessment.setRecoveryDaysPerWeek(recovery.recoveryDaysPerWeek());
        assessment.setRoutineType(recovery.routineType());
        assessment.setRoutineNotes(recovery.routineNotes());

        var race = request.targetRace();
        assessment.setTargetRaceTitle(race.title());
        assessment.setTargetRaceDate(race.date());
        assessment.setTargetRaceDistanceMeters(race.distanceMeters());
        assessment.setTargetRaceTimeSeconds(race.desiredTimeSeconds());
        assessment.setTargetRacePriority(race.priority());

        assessment.setAvailability(new ArrayList<>(request.availability().stream().map(item -> {
            AssessmentAvailability availability = new AssessmentAvailability();
            availability.setAssessment(assessment);
            availability.setDayOfWeek(item.dayOfWeek());
            availability.setAvailableMinutes(item.availableMinutes());
            return availability;
        }).toList()));

        assessment.setHealthIssues(new ArrayList<>(request.health().issues().stream().map(item -> {
            AssessmentHealthIssue issue = new AssessmentHealthIssue();
            issue.setAssessment(assessment);
            issue.setBodyArea(item.bodyArea());
            issue.setDescription(item.description());
            issue.setStatus(item.status());
            issue.setPainSeverity(item.painSeverity());
            issue.setStartedOn(item.startedOn());
            issue.setRestrictionNotes(item.restrictionNotes());
            return issue;
        }).toList()));
        return assessment;
    }

    private void synchronizeAthleteProfile(Athlete athlete, AthleteAssessmentRequest request) {
        athlete.setDateOfBirth(request.physicalProfile().dateOfBirth());
        athlete.setWeightKg(request.physicalProfile().weightKg());
        athlete.setHeightCm(request.physicalProfile().heightCm());
        athlete.setGender(request.physicalProfile().gender());
        athlete.setAvailableTrainingDays(new HashSet<>(request.availability().stream()
                .map(AthleteAssessmentRequest.Availability::dayOfWeek).toList()));

        Objective objective = athlete.getObjectives() == null ? null : athlete.getObjectives().stream()
                .filter(item -> item.getStatus() == com.aicoach.backend.enums.ObjectiveStatus.ACTIVE)
                .findFirst().orElse(null);
        if (objective == null) {
            objective = new Objective();
            objective.setAthlete(athlete);
            objective.setStatus(com.aicoach.backend.enums.ObjectiveStatus.ACTIVE);
            if (athlete.getObjectives() == null) athlete.setObjectives(new ArrayList<>());
            athlete.getObjectives().add(objective);
        }
        objective.setTitle(request.targetRace().title());
        objective.setTargetDate(request.targetRace().date());
        objective.setTargetDistance_m(request.targetRace().distanceMeters());
        objective.setPriority(request.targetRace().priority());
        athleteRepo.save(athlete);
    }

    private AthleteAssessmentResponse toResponse(AthleteAssessment assessment) {
        return new AthleteAssessmentResponse(
                assessment.getId(), assessment.getAthlete().getId(), assessment.getVersion(),
                assessment.getSchemaVersion(), assessment.getOnboardingStatus(), assessment.getCreatedAt(),
                assessment.getCompletedAt(),
                new AthleteAssessmentResponse.PhysicalProfile(assessment.getDateOfBirth(),
                        assessment.getWeightKg(), assessment.getHeightCm(), assessment.getGender()),
                new AthleteAssessmentResponse.RunningProfile(assessment.getExperienceLevel(),
                        assessment.getRunningYears(), assessment.getCurrentWeeklyVolumeKm(),
                        assessment.getRecentAverageWeeklyVolumeKm(), assessment.getRecentLongestRunKm(),
                        assessment.getCurrentRunsPerWeek()),
                assessment.getAvailability().stream()
                        .map(item -> new AthleteAssessmentResponse.Availability(
                                item.getDayOfWeek(), item.getAvailableMinutes()))
                        .toList(),
                assessment.getPreferredLongRunDay(), Set.copyOf(assessment.getSurfaces()),
                Set.copyOf(assessment.getEquipment()),
                new AthleteAssessmentResponse.StrengthTraining(assessment.getStrengthSessionsPerWeek(),
                        assessment.getStrengthTrainingNotes()),
                new AthleteAssessmentResponse.HealthProfile(assessment.getHasMedicalRestrictions(),
                        assessment.getMedicalRestrictions(), assessment.getHealthIssues().stream()
                        .map(item -> new AthleteAssessmentResponse.HealthIssue(item.getBodyArea(),
                                item.getDescription(), item.getStatus(), item.getPainSeverity(),
                                item.getStartedOn(), item.getRestrictionNotes()))
                        .toList()),
                new AthleteAssessmentResponse.RecoveryProfile(assessment.getAverageSleepHours(),
                        assessment.getSleepQuality(), assessment.getRecoveryDaysPerWeek(),
                        assessment.getRoutineType(), assessment.getRoutineNotes()),
                new AthleteAssessmentResponse.TargetRace(assessment.getTargetRaceTitle(),
                        assessment.getTargetRaceDate(), assessment.getTargetRaceDistanceMeters(),
                        assessment.getTargetRaceTimeSeconds(), assessment.getTargetRacePriority()));
    }
}
