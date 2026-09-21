package com.aicoach.backend.weeklyplan;

import com.aicoach.backend.enums.DurationType;
import com.aicoach.backend.enums.HealthIssueStatus;
import com.aicoach.backend.enums.WorkoutType;
import com.aicoach.backend.training.daniels.DanielsIntensity;
import com.aicoach.backend.training.validation.*;
import com.aicoach.backend.training.workout.WorkoutStepDefinition;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class WeeklyPlanValidator {
    private final DanielsTrainingValidator weekValidator = new DanielsTrainingValidator();
    private final DanielsStimulusRules stimulusRules = new DanielsStimulusRules();

    public WeeklyPlanCalculation validate(WeeklyPlanGenerationContext context, WeeklyPlanProposal proposal) {
        List<String> violations = new ArrayList<>();
        if (proposal == null) throw new WeeklyPlanValidationException(List.of("Proposta ausente"));
        if (isBlank(proposal.summary())) violations.add("Resumo semanal obrigatório");
        if (proposal.sessions() == null || proposal.sessions().isEmpty()) {
            violations.add("O plano semanal deve conter sessões");
            throw new WeeklyPlanValidationException(violations);
        }

        Map<DayOfWeek, Integer> availability = new EnumMap<>(DayOfWeek.class);
        if (context.availability() != null) {
            context.availability().forEach(item -> availability.put(item.dayOfWeek(), item.availableMinutes()));
        }
        List<WeeklyPlanCalculation.Session> calculations = new ArrayList<>();
        Set<LocalDate> dates = new HashSet<>();
        LocalDate previousDate = null;
        int longRuns = 0;
        int qualitySessions = 0;
        int raceSessions = 0;

        for (int index = 0; index < proposal.sessions().size(); index++) {
            WeeklyPlanProposal.Session session = proposal.sessions().get(index);
            if (session == null) {
                violations.add("Sessão ausente na posição " + (index + 1));
                continue;
            }
            if (!Objects.equals(session.order(), index + 1)) violations.add("Ordem inválida das sessões");
            if (isBlank(session.name()) || session.workoutType() == null || session.workoutType() == WorkoutType.REST) {
                violations.add("Sessão incompleta na posição " + (index + 1));
            }
            LocalDate date = session.scheduledDate();
            if (date == null || date.isBefore(context.weekStart()) || date.isAfter(context.weekEnd())) {
                violations.add("Sessão fora da semana na posição " + (index + 1));
            } else {
                if (!dates.add(date)) violations.add("Mais de uma sessão em " + date);
                if (previousDate != null && !date.isAfter(previousDate)) {
                    violations.add("Sessões devem estar em ordem cronológica");
                }
                previousDate = date;
                if (!availability.containsKey(date.getDayOfWeek())) {
                    violations.add("Sessão em dia indisponível: " + date.getDayOfWeek());
                }
            }

            WeeklyPlanCalculation.Session calculation = calculateSession(context, session, violations);
            calculations.add(calculation);
            List<WeeklyPlanProposal.Step> sessionSteps = session.blocks() == null ? List.of()
                    : session.blocks().stream().filter(Objects::nonNull)
                    .filter(block -> block.steps() != null).flatMap(block -> block.steps().stream())
                    .filter(Objects::nonNull).toList();
            if (sessionSteps.stream().noneMatch(step -> step.kind() == WorkoutStepDefinition.Kind.WORK)) {
                violations.add("Sessão " + session.order() + " deve conter ao menos um passo WORK");
            }
            if (date != null && availability.containsKey(date.getDayOfWeek())
                    && calculation.durationSeconds() > availability.get(date.getDayOfWeek()) * 60) {
                violations.add("Sessão excede a disponibilidade de " + date.getDayOfWeek());
            }

            boolean quality = calculation.intensityLoads().stream()
                    .anyMatch(load -> isQuality(load.intensity()) && load.durationSeconds() > 0);
            if (quality) qualitySessions++;
            if (quality && session.workoutType() != WorkoutType.QUALITY_1
                    && session.workoutType() != WorkoutType.QUALITY_2 && session.workoutType() != WorkoutType.RACE) {
                violations.add("Sessão com T/I/R deve ser classificada como qualidade");
            }
            if (!quality && (session.workoutType() == WorkoutType.QUALITY_1
                    || session.workoutType() == WorkoutType.QUALITY_2)) {
                violations.add("Sessão de qualidade deve conter estímulo T, I ou R");
            }
            if (quality && session.workoutType() != WorkoutType.RACE && !hasQualityBookends(sessionSteps)) {
                violations.add("Sessão de qualidade exige aquecimento e desaquecimento em E");
            }
            if (session.workoutType() == WorkoutType.LONG_RUN) {
                longRuns++;
                validateLongRun(context, session, calculation, violations);
            }
            if (session.workoutType() == WorkoutType.RACE) {
                raceSessions++;
                if (!Objects.equals(session.scheduledDate(), context.targetRaceDate())) {
                    violations.add("A prova deve ocorrer na data do objetivo");
                }
                int raceTolerance = Math.max(100, context.targetRaceDistanceMeters() / 100);
                if (Math.abs(calculation.distanceMeters() - context.targetRaceDistanceMeters()) > raceTolerance) {
                    violations.add("A sessão RACE deve corresponder à distância da prova");
                }
            }
            if (session.workoutType() != WorkoutType.RACE) validateStimuli(context, session, violations);
        }

        validateSchedule(context, proposal, longRuns, qualitySessions, raceSessions, violations);
        int totalDistance = calculations.stream().mapToInt(WeeklyPlanCalculation.Session::distanceMeters).sum();
        int totalDuration = calculations.stream().mapToInt(WeeklyPlanCalculation.Session::durationSeconds).sum();
        int targetMeters = (int) Math.round(context.effectiveTargetVolumeKm() * 1_000);
        int tolerance = Math.max(500, (int) Math.round(targetMeters * 0.05));
        if (Math.abs(totalDistance - targetMeters) > tolerance) {
            violations.add("Volume detalhado deve ficar a até 5% do alvo semanal");
        }
        validateDanielsWeek(proposal, calculations, violations);
        validateHealth(context, calculations, violations);
        validateAdaptation(context, calculations, violations);

        if (!violations.isEmpty()) throw new WeeklyPlanValidationException(violations);
        return new WeeklyPlanCalculation(totalDistance, totalDuration, List.copyOf(calculations));
    }

    private WeeklyPlanCalculation.Session calculateSession(WeeklyPlanGenerationContext context,
                                                            WeeklyPlanProposal.Session session,
                                                            List<String> violations) {
        if (session.blocks() == null || session.blocks().isEmpty()) {
            violations.add("Sessão " + session.order() + " não possui blocos");
            return new WeeklyPlanCalculation.Session(orZero(session.order()), 0, 0, List.of());
        }
        Map<DanielsIntensity, int[]> loads = new EnumMap<>(DanielsIntensity.class);
        for (int blockIndex = 0; blockIndex < session.blocks().size(); blockIndex++) {
            WeeklyPlanProposal.Block block = session.blocks().get(blockIndex);
            if (block == null || block.repetitions() == null || block.repetitions() <= 0
                    || block.repetitions() > 50 || block.steps() == null || block.steps().isEmpty()) {
                violations.add("Bloco inválido na sessão " + session.order());
                continue;
            }
            for (WeeklyPlanProposal.Step step : block.steps()) {
                if (step == null || step.kind() == null || step.durationType() == null
                        || step.durationValue() == null || step.durationValue() <= 0
                        || step.intensity() == null || isBlank(step.instruction())) {
                    violations.add("Passo inválido na sessão " + session.order());
                    continue;
                }
                int pace;
                try {
                    pace = context.paces().forIntensity(step.intensity());
                } catch (IllegalArgumentException exception) {
                    violations.add(exception.getMessage());
                    continue;
                }
                int distance = step.durationType() == DurationType.DISTANCE ? step.durationValue()
                        : (int) Math.round(step.durationValue() * 1_000.0 / pace);
                int duration = step.durationType() == DurationType.TIME ? step.durationValue()
                        : (int) Math.round(step.durationValue() * pace / 1_000.0);
                int[] load = loads.computeIfAbsent(step.intensity(), ignored -> new int[2]);
                load[0] += distance * block.repetitions();
                load[1] += duration * block.repetitions();
            }
        }
        List<WeeklyPlanCalculation.IntensityLoad> intensityLoads = loads.entrySet().stream()
                .map(entry -> new WeeklyPlanCalculation.IntensityLoad(
                        entry.getKey(), entry.getValue()[0], entry.getValue()[1])).toList();
        return new WeeklyPlanCalculation.Session(orZero(session.order()),
                intensityLoads.stream().mapToInt(WeeklyPlanCalculation.IntensityLoad::distanceMeters).sum(),
                intensityLoads.stream().mapToInt(WeeklyPlanCalculation.IntensityLoad::durationSeconds).sum(),
                intensityLoads);
    }

    private void validateSchedule(WeeklyPlanGenerationContext context, WeeklyPlanProposal proposal,
                                  int longRuns, int qualitySessions, int raceSessions,
                                  List<String> violations) {
        int maxSessions = Math.min(context.currentRunsPerWeek(), 7 - context.recoveryDaysPerWeek());
        if (proposal.sessions().size() > maxSessions) violations.add("Quantidade de sessões excede corrida ou recuperação informada");
        if (longRuns > 1) violations.add("A semana pode ter no máximo um longo");
        int maxQuality = Boolean.TRUE.equals(context.recoveryWeek()) || Boolean.TRUE.equals(context.taperWeek()) ? 1 : 2;
        if (qualitySessions > maxQuality) violations.add("Quantidade de sessões de qualidade incompatível com a semana");

        boolean raceInWeek = !context.targetRaceDate().isBefore(context.weekStart())
                && !context.targetRaceDate().isAfter(context.weekEnd());
        if (raceInWeek && raceSessions != 1) violations.add("A semana da prova deve conter exatamente uma sessão RACE");
        if (!raceInWeek && raceSessions != 0) violations.add("RACE só pode existir na semana da prova");
        if (!raceInWeek && proposal.sessions().size() >= 3 && longRuns != 1) {
            violations.add("Semanas com três ou mais corridas devem conter um longo");
        }
    }

    private void validateLongRun(WeeklyPlanGenerationContext context, WeeklyPlanProposal.Session session,
                                 WeeklyPlanCalculation.Session calculation, List<String> violations) {
        if (session.scheduledDate() != null && session.scheduledDate().getDayOfWeek() != context.preferredLongRunDay()) {
            violations.add("O longo deve usar o dia preferido informado");
        }
        int weeklyMeters = (int) Math.round(context.effectiveTargetVolumeKm() * 1_000);
        if (calculation.distanceMeters() > weeklyMeters * 30 / 100 || calculation.durationSeconds() > 9_000) {
            violations.add("E_LONG_RUN_LIMIT: longo excede 30% do volume semanal ou 150 minutos");
        }
    }

    private void validateStimuli(WeeklyPlanGenerationContext context, WeeklyPlanProposal.Session session,
                                 List<String> violations) {
        for (WeeklyPlanProposal.Block block : session.blocks()) {
            if (block == null || block.steps() == null || block.repetitions() == null || block.repetitions() <= 0) continue;
            for (int index = 0; index < block.steps().size(); index++) {
                WeeklyPlanProposal.Step step = block.steps().get(index);
                if (step == null || step.kind() != WorkoutStepDefinition.Kind.WORK || step.intensity() == null
                        || step.durationValue() == null || step.durationValue() <= 0) continue;
                int workSeconds = durationSeconds(context, step);
                int workMeters = distanceMeters(context, step);
                if (isQuality(step.intensity()) && (block.repetitions() > 1 || index + 1 < block.steps().size()
                        && block.steps().get(index + 1).kind() == WorkoutStepDefinition.Kind.RECOVERY)) {
                    if (index + 1 >= block.steps().size()) {
                        violations.add(step.intensity() + " repetido exige recuperação");
                        continue;
                    }
                    WeeklyPlanProposal.Step recovery = block.steps().get(index + 1);
                    if (recovery.kind() != WorkoutStepDefinition.Kind.RECOVERY || recovery.intensity() == null) {
                        violations.add(step.intensity() + " repetido exige recuperação ativa");
                        continue;
                    }
                    addIssues(stimulusRules.validateRepetition(step.intensity(), workSeconds,
                            durationSeconds(context, recovery), recovery.intensity()), violations);
                } else if (step.intensity() == DanielsIntensity.T || step.intensity() == DanielsIntensity.M
                        || step.intensity() == DanielsIntensity.E
                        && session.workoutType() == WorkoutType.LONG_RUN) {
                    addIssues(stimulusRules.validateContinuous(step.intensity(), workSeconds, workMeters,
                            (int) Math.round(context.effectiveTargetVolumeKm() * 1_000
                                    * context.paces().forIntensity(DanielsIntensity.E) / 1_000.0),
                            (int) Math.round(context.effectiveTargetVolumeKm() * 1_000)), violations);
                } else if (step.intensity() == DanielsIntensity.I || step.intensity() == DanielsIntensity.R) {
                    violations.add(step.intensity() + " deve ser prescrito em bloco repetido com recuperação");
                }
            }
        }
    }

    private void validateDanielsWeek(WeeklyPlanProposal proposal,
                                     List<WeeklyPlanCalculation.Session> calculations,
                                     List<String> violations) {
        List<TrainingLoad.SessionLoad> loads = new ArrayList<>();
        int distance = 0;
        int duration = 0;
        for (int index = 0; index < calculations.size(); index++) {
            if (proposal.sessions().get(index).workoutType() == WorkoutType.RACE) continue;
            WeeklyPlanCalculation.Session calculation = calculations.get(index);
            distance += calculation.distanceMeters();
            duration += calculation.durationSeconds();
            LocalDate date = proposal.sessions().get(index).scheduledDate();
            if (date == null) continue;
            for (WeeklyPlanCalculation.IntensityLoad load : calculation.intensityLoads()) {
                loads.add(new TrainingLoad.SessionLoad(date, load.intensity(),
                        load.distanceMeters(), load.durationSeconds()));
            }
        }
        TrainingLoad load = new TrainingLoad(distance, duration, distance, duration, 3,
                calculations.size(), loads);
        addIssues(weekValidator.validateWeek(load), violations);
    }

    private void validateHealth(WeeklyPlanGenerationContext context,
                                List<WeeklyPlanCalculation.Session> calculations,
                                List<String> violations) {
        int maxActivePain = context.healthIssues() == null ? 0 : context.healthIssues().stream()
                .filter(issue -> issue.status() == HealthIssueStatus.ACTIVE)
                .map(WeeklyPlanGenerationContext.HealthIssue::painSeverity)
                .filter(Objects::nonNull).max(Integer::compareTo).orElse(0);
        if (maxActivePain >= 4) {
            boolean quality = calculations.stream().flatMap(session -> session.intensityLoads().stream())
                    .anyMatch(load -> isQuality(load.intensity()) && load.durationSeconds() > 0);
            if (quality) violations.add("Lesão ativa moderada impede estímulos T/I/R");
        }
    }

    private void validateAdaptation(WeeklyPlanGenerationContext context,
                                    List<WeeklyPlanCalculation.Session> calculations,
                                    List<String> violations) {
        if (context.adaptation() == null || Boolean.TRUE.equals(context.adaptation().allowIntensity())) return;
        boolean quality = calculations.stream().flatMap(session -> session.intensityLoads().stream())
                .anyMatch(load -> isQuality(load.intensity()) && load.durationSeconds() > 0);
        if (quality) violations.add("Adaptação ativa impede estímulos T/I/R");
    }

    private int durationSeconds(WeeklyPlanGenerationContext context, WeeklyPlanProposal.Step step) {
        return step.durationType() == DurationType.TIME ? step.durationValue()
                : (int) Math.round(step.durationValue() * context.paces().forIntensity(step.intensity()) / 1_000.0);
    }

    private int distanceMeters(WeeklyPlanGenerationContext context, WeeklyPlanProposal.Step step) {
        return step.durationType() == DurationType.DISTANCE ? step.durationValue()
                : (int) Math.round(step.durationValue() * 1_000.0 / context.paces().forIntensity(step.intensity()));
    }

    private void addIssues(List<ValidationIssue> issues, List<String> violations) {
        issues.forEach(issue -> violations.add(issue.code() + ": " + issue.message()));
    }

    private boolean isQuality(DanielsIntensity intensity) {
        return intensity == DanielsIntensity.T || intensity == DanielsIntensity.I || intensity == DanielsIntensity.R;
    }

    private boolean hasQualityBookends(List<WeeklyPlanProposal.Step> steps) {
        if (steps.size() < 3) return false;
        WeeklyPlanProposal.Step first = steps.get(0);
        WeeklyPlanProposal.Step last = steps.get(steps.size() - 1);
        return first.kind() == WorkoutStepDefinition.Kind.WARMUP && first.intensity() == DanielsIntensity.E
                && last.kind() == WorkoutStepDefinition.Kind.COOLDOWN && last.intensity() == DanielsIntensity.E;
    }

    private int orZero(Integer value) { return value == null ? 0 : value; }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
