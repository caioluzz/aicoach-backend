package com.aicoach.backend.seasonplan;

import com.aicoach.backend.enums.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class SeasonPlanFixtures {
    private SeasonPlanFixtures() {}

    public static SeasonPlanGenerationContext context(LocalDate start, int totalWeeks) {
        return new SeasonPlanGenerationContext(1L, 2, 3L, start, start.plusDays(totalWeeks * 7L - 1),
                totalWeeks, "10K", 10_000, 3_000, RacePriority.A_RACE,
                RunningExperienceLevel.INTERMEDIATE, 35.0, 32.0, 14.0, 4,
                List.of(new SeasonPlanGenerationContext.Availability(DayOfWeek.TUESDAY, 60),
                        new SeasonPlanGenerationContext.Availability(DayOfWeek.SUNDAY, 120)),
                DayOfWeek.SUNDAY, 2, false, List.of(), 45.0,
                new SeasonPlanGenerationContext.PaceProfile(330, 300, 280, 255, 235));
    }

    public static SeasonPlanProposal validProposal(SeasonPlanGenerationContext context) {
        List<SeasonPlanProposal.Week> weeks = new ArrayList<>();
        double priorVolume = 32.0;
        for (int index = 0; index < context.totalWeeks(); index++) {
            LocalDate start = context.planStartDate().plusWeeks(index);
            LocalDate end = start.plusDays(6).isAfter(context.raceDate()) ? context.raceDate() : start.plusDays(6);
            boolean taper = index == context.totalWeeks() - 1;
            boolean recovery = context.totalWeeks() >= 8 && !taper && (index + 1) % 4 == 0;
            double volume;
            if (taper || recovery) volume = Math.round(priorVolume * 0.80 * 10.0) / 10.0;
            else if (index > 0 && Boolean.TRUE.equals(weeks.get(index - 1).recoveryWeek())) volume = 35.0;
            else volume = Math.round(priorVolume * 1.05 * 10.0) / 10.0;
            weeks.add(new SeasonPlanProposal.Week(index + 1, start, end, volume,
                    taper ? "Redução e prova" : recovery ? "Recuperação" : "Desenvolvimento",
                    recovery, taper));
            priorVolume = volume;
        }
        int finalWeek = context.totalWeeks();
        List<SeasonPlanProposal.Phase> phases = finalWeek == 1
                ? List.of(new SeasonPlanProposal.Phase(1, TrainingPhase.FINAL, 1, 1,
                "Chegar recuperado", "Reduzir a carga"))
                : List.of(new SeasonPlanProposal.Phase(1, TrainingPhase.BASE, 1, finalWeek - 1,
                        "Consolidar base", "Aumentar carga de forma conservadora"),
                new SeasonPlanProposal.Phase(2, TrainingPhase.FINAL, finalWeek, finalWeek,
                        "Chegar recuperado", "Reduzir a carga"));
        return new SeasonPlanProposal("Plano estratégico até a prova", phases, weeks,
                List.of(
                        new SeasonPlanProposal.RevisionCriterion("HEALTH_CHANGE", "Revisar diante de dor ou restrição"),
                        new SeasonPlanProposal.RevisionCriterion("RACE_CHANGE", "Revisar se a prova mudar"),
                        new SeasonPlanProposal.RevisionCriterion("INTERRUPTION", "Revisar apó interrupção relevante"),
                        new SeasonPlanProposal.RevisionCriterion("PROGRESS_DIVERGENCE", "Revisar se o progresso divergir")));
    }
}
