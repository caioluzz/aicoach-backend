package com.aicoach.backend.secondaryrace;

import com.aicoach.backend.enums.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class SecondaryRaceAdjustmentEngine {
    public static final String VERSION = "secondary-race-rules-v1";

    public Decision evaluate(Input input) {
        if (input.priority() == RacePriority.A_RACE) {
            throw new IllegalArgumentException("A prova intermediária deve ter prioridade B ou C");
        }
        int preDays = input.priority() == RacePriority.B_RACE ? 5 : 3;
        int postDays = input.priority() == RacePriority.B_RACE ? 4 : 2;
        LocalDate windowStart = input.raceDate().minusDays(preDays);
        LocalDate windowEnd = input.raceDate().plusDays(postDays);
        List<Session> candidates = input.sessions().stream()
                .filter(session -> !session.date().isBefore(windowStart) && !session.date().isAfter(windowEnd))
                .toList();

        Session replacedQuality = closestQualityInRaceWeek(candidates, input.raceDate());
        List<Adjustment> adjustments = new ArrayList<>();
        if (replacedQuality == null) {
            adjustments.add(new Adjustment(null, null, RaceAdjustmentWindow.RACE_DAY,
                    SecondaryRaceAdjustmentAction.ADD_RACE, null, input.raceDate(), null,
                    WorkoutType.RACE, null, input.distanceMeters(), null, input.targetTimeSeconds(),
                    100, "Adicionar a prova à semana; não havia treino de qualidade apropriado para substituição."));
        } else {
            adjustments.add(replacement(input, replacedQuality));
        }

        for (Session session : candidates) {
            if (replacedQuality != null && session.id().equals(replacedQuality.id())) continue;
            Adjustment adjustment = surroundingAdjustment(input, session);
            if (adjustment != null) adjustments.add(adjustment);
        }
        adjustments.sort(Comparator.comparing(Adjustment::proposedDate)
                .thenComparing(value -> value.sessionId() == null ? Long.MAX_VALUE : value.sessionId()));
        String rationale = input.priority() == RacePriority.B_RACE
                ? "Prova B tratada como estímulo de qualidade: taper curto e recuperação de até quatro dias."
                : "Prova C tratada como estímulo controlado: ajuste mínimo e recuperação de até dois dias.";
        return new Decision(windowStart, windowEnd, rationale, List.copyOf(adjustments));
    }

    private Session closestQualityInRaceWeek(List<Session> sessions, LocalDate raceDate) {
        LocalDate monday = raceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        return sessions.stream().filter(session -> !session.date().isBefore(monday)
                        && !session.date().isAfter(sunday) && isQuality(session.type()))
                .min(Comparator.comparingLong(session -> Math.abs(
                        ChronoUnit.DAYS.between(session.date(), raceDate)))).orElse(null);
    }

    private Adjustment replacement(Input input, Session session) {
        return new Adjustment(session.id(), session.weeklyPlanId(), RaceAdjustmentWindow.RACE_DAY,
                SecondaryRaceAdjustmentAction.REPLACE_WITH_RACE, session.date(), input.raceDate(),
                session.type(), WorkoutType.RACE, session.distanceMeters(), input.distanceMeters(),
                session.durationSeconds(), input.targetTimeSeconds(), 100,
                "A prova substitui o treino de qualidade mais próximo na mesma semana, evitando duplicar intensidade.");
    }

    private Adjustment surroundingAdjustment(Input input, Session session) {
        long offset = ChronoUnit.DAYS.between(input.raceDate(), session.date());
        if (offset == 0) {
            return change(session, RaceAdjustmentWindow.RACE_DAY,
                    SecondaryRaceAdjustmentAction.REST, WorkoutType.REST, 0,
                    "Remover outra sessão no dia da prova.");
        }
        if (offset < 0) return beforeRace(input.priority(), session, (int) -offset);
        return afterRace(input.priority(), session, (int) offset);
    }

    private Adjustment beforeRace(RacePriority priority, Session session, int daysBefore) {
        if (daysBefore == 1 && priority == RacePriority.B_RACE) {
            return change(session, RaceAdjustmentWindow.PRE_RACE,
                    SecondaryRaceAdjustmentAction.REST, WorkoutType.REST, 0,
                    "Descanso na véspera da prova B.");
        }
        int qualityWindow = priority == RacePriority.B_RACE ? 3 : 2;
        if (isQuality(session.type()) && daysBefore <= qualityWindow) {
            int load = priority == RacePriority.B_RACE ? 60 : 75;
            return change(session, RaceAdjustmentWindow.PRE_RACE,
                    SecondaryRaceAdjustmentAction.REPLACE_WITH_EASY, WorkoutType.EASY_RUN, load,
                    "Retirar intensidade próxima à prova; a competição já ocupa o estímulo de qualidade.");
        }
        if (daysBefore == 1) {
            return change(session, RaceAdjustmentWindow.PRE_RACE,
                    SecondaryRaceAdjustmentAction.REDUCE, session.type(), 50,
                    "Reduzir a sessão da véspera para chegar recuperado à prova.");
        }
        if (priority == RacePriority.B_RACE && daysBefore <= 3 && session.type() != WorkoutType.REST) {
            return change(session, RaceAdjustmentWindow.PRE_RACE,
                    SecondaryRaceAdjustmentAction.REDUCE, session.type(), 75,
                    "Aplicar taper curto sem alterar o objetivo da semana do plano geral.");
        }
        return null;
    }

    private Adjustment afterRace(RacePriority priority, Session session, int daysAfter) {
        if (priority == RacePriority.B_RACE && daysAfter == 1) {
            return change(session, RaceAdjustmentWindow.POST_RACE,
                    SecondaryRaceAdjustmentAction.REST, WorkoutType.REST, 0,
                    "Descanso no primeiro dia após a prova B.");
        }
        int recoveryWindow = priority == RacePriority.B_RACE ? 4 : 2;
        if (daysAfter <= recoveryWindow && session.type() != WorkoutType.REST) {
            int load = priority == RacePriority.B_RACE ? (daysAfter <= 2 ? 50 : 70) : 70;
            return change(session, RaceAdjustmentWindow.POST_RACE,
                    SecondaryRaceAdjustmentAction.RECOVERY, WorkoutType.RECOVERY, load,
                    "Substituir por recuperação pós-prova antes de retomar a progressão planejada.");
        }
        return null;
    }

    private Adjustment change(Session session, RaceAdjustmentWindow window,
                              SecondaryRaceAdjustmentAction action, WorkoutType proposedType,
                              int loadPercent, String rationale) {
        return new Adjustment(session.id(), session.weeklyPlanId(), window, action,
                session.date(), session.date(), session.type(), proposedType,
                session.distanceMeters(), scale(session.distanceMeters(), loadPercent),
                session.durationSeconds(), scale(session.durationSeconds(), loadPercent),
                loadPercent, rationale);
    }

    private Integer scale(Integer value, int percent) {
        if (value == null) return null;
        return BigDecimal.valueOf(value).multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).intValue();
    }

    private boolean isQuality(WorkoutType type) {
        return type == WorkoutType.QUALITY_1 || type == WorkoutType.QUALITY_2;
    }

    public record Input(LocalDate raceDate, Integer distanceMeters, Integer targetTimeSeconds,
                        RacePriority priority, List<Session> sessions) {}
    public record Session(Long id, Long weeklyPlanId, LocalDate date, WorkoutType type,
                          Integer distanceMeters, Integer durationSeconds) {}
    public record Decision(LocalDate windowStart, LocalDate windowEnd, String rationale,
                           List<Adjustment> adjustments) {}
    public record Adjustment(Long sessionId, Long weeklyPlanId, RaceAdjustmentWindow window,
                             SecondaryRaceAdjustmentAction action, LocalDate originalDate,
                             LocalDate proposedDate, WorkoutType originalType, WorkoutType proposedType,
                             Integer originalDistanceMeters, Integer proposedDistanceMeters,
                             Integer originalDurationSeconds, Integer proposedDurationSeconds,
                             Integer loadPercent, String rationale) {}
}
