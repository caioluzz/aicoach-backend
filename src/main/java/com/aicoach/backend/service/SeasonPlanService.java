package com.aicoach.backend.service;

import com.aicoach.backend.dto.*;
import com.aicoach.backend.enums.*;
import com.aicoach.backend.models.*;
import com.aicoach.backend.repository.*;
import com.aicoach.backend.seasonplan.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SeasonPlanService {
    private final AthleteRepo athleteRepo;
    private final ObjectiveRepo objectiveRepo;
    private final AthleteAssessmentRepo assessmentRepo;
    private final AthleteMetricsRepo metricsRepo;
    private final GlobalPlanRepo planRepo;
    private final SeasonPlanGenerator generator;
    private final SeasonPlanValidator validator;
    private final Clock clock;

    @Transactional
    public SeasonPlanResponse create(Long athleteId, SeasonPlanCreateRequest request) {
        Athlete athlete = athleteRepo.findById(athleteId)
                .orElseThrow(() -> new AthleteNotFoundException(athleteId));
        Objective objective = objectiveRepo.findByIdAndAthleteId(request.objectiveId(), athleteId)
                .orElseThrow(() -> new SeasonPlanPrerequisiteException("A prova-alvo não pertence ao atleta"));
        if (objective.getStatus() != ObjectiveStatus.ACTIVE)
            throw new SeasonPlanPrerequisiteException("A prova-alvo deve estar ativa");

        AthleteAssessment assessment = assessmentRepo.findTopByAthleteIdOrderByVersionDesc(athleteId)
                .filter(item -> item.getOnboardingStatus() == OnboardingStatus.COMPLETED)
                .orElseThrow(() -> new SeasonPlanPrerequisiteException("Conclua a anamnese antes de gerar o plano"));
        AthleteMetrics metrics = metricsRepo.findTopByAthleteIdOrderByRecordedAtDesc(athleteId)
                .orElseThrow(() -> new SeasonPlanPrerequisiteException("Calcule o perfil Daniels antes de gerar o plano"));

        LocalDate startDate = LocalDate.now(clock);
        if (objective.getTargetDate().isBefore(startDate))
            throw new SeasonPlanPrerequisiteException("A prova-alvo deve estar no futuro");
        if (!objective.getTargetDate().equals(assessment.getTargetRaceDate())
                || !objective.getTargetDistance_m().equals(assessment.getTargetRaceDistanceMeters()))
            throw new SeasonPlanPrerequisiteException("A prova-alvo deve corresponder à versão atual da anamnese");

        int totalWeeks = (int) Math.ceil((ChronoUnit.DAYS.between(startDate, objective.getTargetDate()) + 1) / 7.0);
        SeasonPlanGenerationContext context = toContext(athleteId, assessment, metrics, objective, startDate, totalWeeks);
        SeasonPlanGenerationResult generated = generator.generate(context);
        validator.validate(context, generated.proposal());

        int version = planRepo.findTopByAthleteIdAndObjectiveIdOrderByVersionDesc(athleteId, objective.getId())
                .map(plan -> plan.getVersion() + 1).orElse(1);
        GlobalPlan plan = mapPlan(athlete, objective, assessment, metrics, context, generated, version);
        return toResponse(planRepo.save(plan));
    }

    @Transactional(readOnly = true)
    public SeasonPlanResponse get(Long athleteId, Long planId) {
        ensureAthleteExists(athleteId);
        return planRepo.findByIdAndAthleteId(planId, athleteId).map(this::toResponse)
                .orElseThrow(() -> new SeasonPlanNotFoundException(planId));
    }

    @Transactional(readOnly = true)
    public SeasonPlanResponse getLatest(Long athleteId) {
        ensureAthleteExists(athleteId);
        return planRepo.findTopByAthleteIdOrderByCreatedAtDesc(athleteId).map(this::toResponse)
                .orElseThrow(() -> new SeasonPlanNotFoundException(athleteId));
    }

    @Transactional(readOnly = true)
    public List<SeasonPlanResponse> getHistory(Long athleteId) {
        ensureAthleteExists(athleteId);
        return planRepo.findByAthleteIdOrderByCreatedAtDesc(athleteId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public SeasonPlanResponse review(Long athleteId, Long planId, SeasonPlanReviewRequest request) {
        GlobalPlan plan = planRepo.findByIdAndAthleteId(planId, athleteId)
                .orElseThrow(() -> new SeasonPlanNotFoundException(planId));
        if (plan.getStatus() != SeasonPlanStatus.DRAFT)
            throw new SeasonPlanStateException("Somente um plano em rascunho pode ser revisado");
        plan.setReviewedAt(Instant.now(clock));
        plan.setReviewComment(blankToNull(request.comment()));
        if (request.decision() == SeasonPlanReviewDecision.REJECT) {
            plan.setStatus(SeasonPlanStatus.REJECTED);
        } else {
            planRepo.findFirstByAthleteIdAndObjectiveIdAndStatus(
                    athleteId, plan.getObjective().getId(), SeasonPlanStatus.APPROVED)
                    .filter(previous -> !previous.getId().equals(plan.getId()))
                    .ifPresent(previous -> previous.setStatus(SeasonPlanStatus.SUPERSEDED));
            plan.setStatus(SeasonPlanStatus.APPROVED);
        }
        return toResponse(plan);
    }

    private SeasonPlanGenerationContext toContext(Long athleteId, AthleteAssessment assessment,
                                                   AthleteMetrics metrics, Objective objective,
                                                   LocalDate startDate, int totalWeeks) {
        return new SeasonPlanGenerationContext(athleteId, assessment.getVersion(), metrics.getId(),
                startDate, objective.getTargetDate(), totalWeeks, objective.getTitle(),
                objective.getTargetDistance_m(), assessment.getTargetRaceTimeSeconds(), objective.getPriority(),
                assessment.getExperienceLevel(), assessment.getCurrentWeeklyVolumeKm(),
                assessment.getRecentAverageWeeklyVolumeKm(), assessment.getRecentLongestRunKm(),
                assessment.getCurrentRunsPerWeek(), assessment.getAvailability().stream()
                .map(item -> new SeasonPlanGenerationContext.Availability(
                        item.getDayOfWeek(), item.getAvailableMinutes())).toList(),
                assessment.getPreferredLongRunDay(), assessment.getRecoveryDaysPerWeek(),
                assessment.getHasMedicalRestrictions(), assessment.getHealthIssues().stream()
                .map(item -> new SeasonPlanGenerationContext.HealthIssue(item.getBodyArea(), item.getStatus(),
                        item.getPainSeverity(), item.getRestrictionNotes())).toList(), metrics.getVdot(),
                new SeasonPlanGenerationContext.PaceProfile(metrics.getEasyPaceSec(), metrics.getMarathonPaceSec(),
                        metrics.getThresholdPaceSec(), metrics.getIntervalPaceSec(), metrics.getRepetitionPaceSec()));
    }

    private GlobalPlan mapPlan(Athlete athlete, Objective objective, AthleteAssessment assessment,
                               AthleteMetrics metrics, SeasonPlanGenerationContext context,
                               SeasonPlanGenerationResult generated, int version) {
        GlobalPlan plan = new GlobalPlan();
        plan.setAthlete(athlete);
        plan.setObjective(objective);
        plan.setAssessment(assessment);
        plan.setAthleteMetrics(metrics);
        plan.setVersion(version);
        plan.setStatus(SeasonPlanStatus.DRAFT);
        plan.setStartDate(context.planStartDate());
        plan.setEndDate(context.raceDate());
        plan.setTotalWeeks(context.totalWeeks());
        plan.setSummary(generated.proposal().summary());
        plan.setCreatedAt(Instant.now(clock));
        plan.setGenerationSource("OPENAI");
        plan.setOpenaiResponseId(generated.responseId());
        plan.setModel(generated.model());
        plan.setInputTokens(generated.inputTokens());
        plan.setOutputTokens(generated.outputTokens());
        plan.setGenerationLatencyMs(generated.latencyMs());

        List<SeasonPlanWeek> weeks = generated.proposal().weeks().stream().map(source -> {
            SeasonPlanWeek week = new SeasonPlanWeek();
            week.setGlobalPlan(plan);
            week.setWeekNumber(source.weekNumber());
            week.setStartDate(source.startDate());
            week.setEndDate(source.endDate());
            week.setTargetVolumeKm(source.targetVolumeKm());
            week.setFocus(source.focus());
            week.setRecoveryWeek(source.recoveryWeek());
            week.setTaperWeek(source.taperWeek());
            return week;
        }).toList();
        plan.setWeeks(new ArrayList<>(weeks));

        plan.setTrainingCycles(new ArrayList<>(generated.proposal().phases().stream().map(source -> {
            TrainingCycle cycle = new TrainingCycle();
            cycle.setGlobalPlan(plan);
            cycle.setCycleOrder(source.order());
            cycle.setPhase(source.phase());
            cycle.setStartWeek(source.startWeek());
            cycle.setEndWeek(source.endWeek());
            cycle.setStartDate(weeks.get(source.startWeek() - 1).getStartDate());
            cycle.setEndDate(weeks.get(source.endWeek() - 1).getEndDate());
            cycle.setObjective(source.objective());
            cycle.setExpectedProgression(source.expectedProgression());
            cycle.setMaxWeeklyVolumeKm(weeks.subList(source.startWeek() - 1, source.endWeek()).stream()
                    .mapToDouble(SeasonPlanWeek::getTargetVolumeKm).max().orElseThrow());
            return cycle;
        }).toList()));

        plan.setRevisionCriteria(new ArrayList<>(generated.proposal().revisionCriteria().stream().map(source -> {
            PlanRevisionCriterion criterion = new PlanRevisionCriterion();
            criterion.setGlobalPlan(plan);
            criterion.setCode(source.code());
            criterion.setDescription(source.description());
            return criterion;
        }).toList()));
        return plan;
    }

    private SeasonPlanResponse toResponse(GlobalPlan plan) {
        return new SeasonPlanResponse(plan.getId(), plan.getAthlete().getId(), plan.getObjective().getId(),
                plan.getAssessment().getId(), plan.getAssessment().getVersion(), plan.getAthleteMetrics().getId(),
                plan.getVersion(), plan.getStatus(), plan.getStartDate(), plan.getEndDate(), plan.getTotalWeeks(),
                plan.getSummary(), plan.getCreatedAt(), plan.getReviewedAt(), plan.getReviewComment(),
                new SeasonPlanResponse.Generation(plan.getGenerationSource(), plan.getOpenaiResponseId(),
                        plan.getModel(), plan.getPromptVersion(), plan.getSchemaVersion(), plan.getInputTokens(),
                        plan.getOutputTokens(), plan.getGenerationLatencyMs()),
                plan.getTrainingCycles().stream().sorted(Comparator.comparing(TrainingCycle::getCycleOrder))
                        .map(item -> new SeasonPlanResponse.Phase(item.getCycleOrder(), item.getPhase(),
                                item.getStartWeek(), item.getEndWeek(), item.getStartDate(), item.getEndDate(),
                                item.getObjective(), item.getExpectedProgression(), item.getMaxWeeklyVolumeKm())).toList(),
                plan.getWeeks().stream().map(item -> new SeasonPlanResponse.Week(item.getWeekNumber(),
                        item.getStartDate(), item.getEndDate(), item.getTargetVolumeKm(), item.getFocus(),
                        item.getRecoveryWeek(), item.getTaperWeek())).toList(),
                plan.getRevisionCriteria().stream().map(item -> new SeasonPlanResponse.RevisionCriterion(
                        item.getCode(), item.getDescription())).toList());
    }

    private void ensureAthleteExists(Long athleteId) {
        if (!athleteRepo.existsById(athleteId)) throw new AthleteNotFoundException(athleteId);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
