package com.aicoach.backend.service;

import com.aicoach.backend.dto.WeeklyPlanCreateRequest;
import com.aicoach.backend.dto.WeeklyPlanResponse;
import com.aicoach.backend.enums.*;
import com.aicoach.backend.models.*;
import com.aicoach.backend.repository.*;
import com.aicoach.backend.training.daniels.DanielsIntensity;
import com.aicoach.backend.weeklyplan.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WeeklyPlanService {
    private final AthleteRepo athleteRepo;
    private final GlobalPlanRepo seasonPlanRepo;
    private final WeeklyPlanRepo weeklyPlanRepo;
    private final WeeklyPlanGenerator generator;
    private final WeeklyPlanValidator validator;
    private final Clock clock;

    @Transactional
    public WeeklyPlanResponse create(Long athleteId, WeeklyPlanCreateRequest request) {
        Athlete athlete = athleteRepo.findById(athleteId)
                .orElseThrow(() -> new AthleteNotFoundException(athleteId));
        GlobalPlan seasonPlan = seasonPlanRepo.findByIdAndAthleteId(request.seasonPlanId(), athleteId)
                .orElseThrow(() -> new WeeklyPlanPrerequisiteException("O plano geral não pertence ao atleta"));
        if (seasonPlan.getStatus() != SeasonPlanStatus.APPROVED) {
            throw new WeeklyPlanPrerequisiteException("O plano geral precisa estar aprovado");
        }
        SeasonPlanWeek week = seasonPlan.getWeeks().stream()
                .filter(item -> item.getWeekNumber().equals(request.weekNumber())).findFirst()
                .orElseThrow(() -> new WeeklyPlanPrerequisiteException("Semana inexistente no plano geral"));
        TrainingCycle cycle = seasonPlan.getTrainingCycles().stream()
                .filter(item -> request.weekNumber() >= item.getStartWeek()
                        && request.weekNumber() <= item.getEndWeek()).findFirst()
                .orElseThrow(() -> new WeeklyPlanPrerequisiteException("A semana não possui fase de treinamento"));

        AthleteAssessment assessment = seasonPlan.getAssessment();
        validateHealthClearance(assessment);
        AthleteMetrics metrics = seasonPlan.getAthleteMetrics();
        WeeklyPlanGenerationContext context = toContext(seasonPlan, week, cycle, assessment, metrics);
        WeeklyPlanGenerationResult generated = generator.generate(context);
        WeeklyPlanCalculation calculation = validator.validate(context, generated.proposal());

        int version = weeklyPlanRepo
                .findTopByAthleteIdAndGlobalPlanIdAndSeasonPlanWeekWeekNumberOrderByVersionDesc(
                        athleteId, seasonPlan.getId(), week.getWeekNumber())
                .map(plan -> plan.getVersion() + 1).orElse(1);
        WeeklyPlan weeklyPlan = mapPlan(athlete, seasonPlan, week, cycle, assessment, metrics,
                generated, calculation, version, context);
        return toResponse(weeklyPlanRepo.save(weeklyPlan));
    }

    @Transactional(readOnly = true)
    public WeeklyPlanResponse get(Long athleteId, Long weeklyPlanId) {
        ensureAthleteExists(athleteId);
        return weeklyPlanRepo.findByIdAndAthleteId(weeklyPlanId, athleteId).map(this::toResponse)
                .orElseThrow(() -> new WeeklyPlanNotFoundException(weeklyPlanId));
    }

    @Transactional(readOnly = true)
    public WeeklyPlanResponse getLatest(Long athleteId, Long seasonPlanId, Integer weekNumber) {
        ensureAthleteExists(athleteId);
        return weeklyPlanRepo.findTopByAthleteIdAndGlobalPlanIdAndSeasonPlanWeekWeekNumberOrderByVersionDesc(
                        athleteId, seasonPlanId, weekNumber).map(this::toResponse)
                .orElseThrow(() -> new WeeklyPlanNotFoundException(seasonPlanId));
    }

    @Transactional(readOnly = true)
    public List<WeeklyPlanResponse> getHistory(Long athleteId, Long seasonPlanId, Integer weekNumber) {
        ensureAthleteExists(athleteId);
        return weeklyPlanRepo.findByAthleteIdAndGlobalPlanIdAndSeasonPlanWeekWeekNumberOrderByVersionDesc(
                athleteId, seasonPlanId, weekNumber).stream().map(this::toResponse).toList();
    }

    private WeeklyPlanGenerationContext toContext(GlobalPlan plan, SeasonPlanWeek week, TrainingCycle cycle,
                                                   AthleteAssessment assessment, AthleteMetrics metrics) {
        Double priorVolume = plan.getWeeks().stream()
                .filter(item -> item.getWeekNumber() == week.getWeekNumber() - 1)
                .map(SeasonPlanWeek::getTargetVolumeKm).findFirst()
                .orElse(assessment.getRecentAverageWeeklyVolumeKm());
        return new WeeklyPlanGenerationContext(plan.getAthlete().getId(), plan.getId(), week.getId(),
                week.getWeekNumber(), week.getStartDate(), week.getEndDate(), week.getTargetVolumeKm(),
                week.getFocus(), week.getRecoveryWeek(), week.getTaperWeek(), cycle.getPhase(),
                cycle.getObjective(), priorVolume, plan.getObjective().getTargetDate(),
                plan.getObjective().getTargetDistance_m(), assessment.getVersion(),
                assessment.getAvailability().stream().map(item -> new WeeklyPlanGenerationContext.Availability(
                        item.getDayOfWeek(), item.getAvailableMinutes())).toList(),
                assessment.getPreferredLongRunDay(), assessment.getCurrentRunsPerWeek(),
                assessment.getRecoveryDaysPerWeek(), assessment.getHasMedicalRestrictions(),
                assessment.getMedicalRestrictions(), assessment.getHealthIssues().stream()
                .map(item -> new WeeklyPlanGenerationContext.HealthIssue(item.getBodyArea(), item.getStatus(),
                        item.getPainSeverity(), item.getRestrictionNotes())).toList(),
                assessment.getAverageSleepHours(), assessment.getSleepQuality(), assessment.getRoutineType(),
                assessment.getRoutineNotes(), metrics.getVdot(), new WeeklyPlanGenerationContext.PaceProfile(
                metrics.getEasyPaceSec(), metrics.getMarathonPaceSec(), metrics.getThresholdPaceSec(),
                metrics.getIntervalPaceSec(), metrics.getRepetitionPaceSec()));
    }

    private void validateHealthClearance(AthleteAssessment assessment) {
        if (Boolean.TRUE.equals(assessment.getHasMedicalRestrictions())) {
            throw new WeeklyPlanPrerequisiteException(
                    "Restrição médica ativa exige liberação antes do planejamento semanal");
        }
        boolean severeActiveIssue = assessment.getHealthIssues().stream().anyMatch(issue ->
                issue.getStatus() == HealthIssueStatus.ACTIVE && issue.getPainSeverity() != null
                        && issue.getPainSeverity() >= 7);
        if (severeActiveIssue) {
            throw new WeeklyPlanPrerequisiteException(
                    "Dor ativa grave exige avaliação antes do planejamento semanal");
        }
    }

    private WeeklyPlan mapPlan(Athlete athlete, GlobalPlan seasonPlan, SeasonPlanWeek week,
                               TrainingCycle cycle, AthleteAssessment assessment, AthleteMetrics metrics,
                               WeeklyPlanGenerationResult generated, WeeklyPlanCalculation calculation,
                               int version, WeeklyPlanGenerationContext context) {
        WeeklyPlan plan = new WeeklyPlan();
        plan.setAthlete(athlete);
        plan.setGlobalPlan(seasonPlan);
        plan.setSeasonPlanWeek(week);
        plan.setTrainingCycle(cycle);
        plan.setAssessment(assessment);
        plan.setAthleteMetrics(metrics);
        plan.setVersion(version);
        plan.setWeekStart(week.getStartDate());
        plan.setWeekEnd(week.getEndDate());
        plan.setTargetVolumeKm(week.getTargetVolumeKm());
        plan.setPlannedDistanceMeters(calculation.totalDistanceMeters());
        plan.setPlannedDurationSeconds(calculation.totalDurationSeconds());
        plan.setSummary(generated.proposal().summary().trim());
        plan.setCreatedAt(Instant.now(clock));
        plan.setGenerationSource("OPENAI");
        plan.setOpenaiResponseId(generated.responseId());
        plan.setModel(generated.model());
        plan.setInputTokens(generated.inputTokens());
        plan.setOutputTokens(generated.outputTokens());
        plan.setGenerationLatencyMs(generated.latencyMs());

        List<PlannedActivity> sessions = new ArrayList<>();
        for (int sessionIndex = 0; sessionIndex < generated.proposal().sessions().size(); sessionIndex++) {
            WeeklyPlanProposal.Session source = generated.proposal().sessions().get(sessionIndex);
            WeeklyPlanCalculation.Session totals = calculation.sessions().get(sessionIndex);
            PlannedActivity session = new PlannedActivity();
            session.setTrainingCycle(cycle);
            session.setWeeklyPlan(plan);
            session.setSessionOrder(source.order());
            session.setName(source.name().trim());
            session.setScheduledDate(source.scheduledDate());
            session.setWorkoutType(source.workoutType());
            session.setPlannedDistanceMeters(totals.distanceMeters());
            session.setPlannedDurationSeconds(totals.durationSeconds());
            session.setCalculatedStressPoints(calculateStress(totals));

            List<WorkoutBlock> blocks = new ArrayList<>();
            for (int blockIndex = 0; blockIndex < source.blocks().size(); blockIndex++) {
                WeeklyPlanProposal.Block sourceBlock = source.blocks().get(blockIndex);
                WorkoutBlock block = new WorkoutBlock();
                block.setPlannedActivity(session);
                block.setBlockOrder(blockIndex + 1);
                block.setIterations(sourceBlock.repetitions());
                List<WorkoutStep> steps = new ArrayList<>();
                for (int stepIndex = 0; stepIndex < sourceBlock.steps().size(); stepIndex++) {
                    WeeklyPlanProposal.Step sourceStep = sourceBlock.steps().get(stepIndex);
                    WorkoutStep step = new WorkoutStep();
                    step.setPlannedActivity(session);
                    step.setWorkoutBlock(block);
                    step.setStepOrder(stepIndex + 1);
                    step.setStepType(StepType.valueOf(sourceStep.kind().name()));
                    step.setDurationType(sourceStep.durationType());
                    step.setDurationValue(sourceStep.durationValue());
                    step.setTargetZone(toZone(sourceStep.intensity()));
                    int pace = context.paces().forIntensity(sourceStep.intensity());
                    step.setTargetPaceFastestSecondsPerKm(pace);
                    step.setTargetPaceSlowestSecondsPerKm(pace);
                    step.setInstruction(sourceStep.instruction().trim());
                    steps.add(step);
                }
                block.setSteps(steps);
                blocks.add(block);
            }
            session.setWorkoutBlocks(blocks);
            sessions.add(session);
        }
        plan.setSessions(sessions);
        return plan;
    }

    private int calculateStress(WeeklyPlanCalculation.Session session) {
        double points = session.intensityLoads().stream().mapToDouble(load ->
                load.durationSeconds() / 60.0 * switch (load.intensity()) {
                    case E -> 1.0;
                    case M -> 1.5;
                    case T -> 2.0;
                    case I -> 2.5;
                    case R -> 3.0;
                }).sum();
        return (int) Math.round(points);
    }

    private IntensityZone toZone(DanielsIntensity intensity) {
        return IntensityZone.valueOf(intensity.name() + "_PACE");
    }

    private WeeklyPlanResponse toResponse(WeeklyPlan plan) {
        return new WeeklyPlanResponse(plan.getId(), plan.getAthlete().getId(), plan.getGlobalPlan().getId(),
                plan.getSeasonPlanWeek().getWeekNumber(), plan.getVersion(), plan.getWeekStart(), plan.getWeekEnd(),
                plan.getTargetVolumeKm(), plan.getPlannedDistanceMeters(), plan.getPlannedDurationSeconds(),
                plan.getSummary(), plan.getCreatedAt(), new WeeklyPlanResponse.Generation(plan.getGenerationSource(),
                plan.getOpenaiResponseId(), plan.getModel(), plan.getPromptVersion(), plan.getSchemaVersion(),
                plan.getInputTokens(), plan.getOutputTokens(), plan.getGenerationLatencyMs()),
                plan.getSessions().stream().sorted(Comparator.comparing(PlannedActivity::getSessionOrder))
                        .map(this::toSessionResponse).toList());
    }

    private WeeklyPlanResponse.Session toSessionResponse(PlannedActivity session) {
        return new WeeklyPlanResponse.Session(session.getSessionOrder(), session.getName(), session.getScheduledDate(),
                session.getWorkoutType(), session.getPlannedDistanceMeters(), session.getPlannedDurationSeconds(),
                session.getWorkoutBlocks().stream().sorted(Comparator.comparing(WorkoutBlock::getBlockOrder))
                        .map(block -> new WeeklyPlanResponse.Block(block.getBlockOrder(), block.getIterations(),
                                block.getSteps().stream().sorted(Comparator.comparing(WorkoutStep::getStepOrder))
                                        .map(step -> new WeeklyPlanResponse.Step(step.getStepOrder(), step.getStepType(),
                                                step.getDurationType(), step.getDurationValue(), step.getTargetZone(),
                                                step.getTargetPaceFastestSecondsPerKm(),
                                                step.getTargetPaceSlowestSecondsPerKm(), step.getInstruction())).toList()))
                        .toList());
    }

    private void ensureAthleteExists(Long athleteId) {
        if (!athleteRepo.existsById(athleteId)) throw new AthleteNotFoundException(athleteId);
    }
}
