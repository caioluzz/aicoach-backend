package com.aicoach.backend.seasonplan;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
public class SeasonPlanValidator {
    public static final Set<String> REQUIRED_REVISION_CRITERIA = Set.of(
            "HEALTH_CHANGE", "RACE_CHANGE", "INTERRUPTION", "PROGRESS_DIVERGENCE");

    public void validate(SeasonPlanGenerationContext context, SeasonPlanProposal proposal) {
        List<String> violations = new ArrayList<>();
        if (proposal == null) {
            throw new SeasonPlanValidationException(List.of("A proposta está ausente"));
        }
        if (isBlank(proposal.summary())) violations.add("O resumo é obrigatório");
        validateWeeks(context, proposal.weeks(), violations);
        validatePhases(context, proposal.phases(), proposal.weeks(), violations);
        validateRevisionCriteria(proposal.revisionCriteria(), violations);
        if (!violations.isEmpty()) throw new SeasonPlanValidationException(violations);
    }

    private void validateWeeks(SeasonPlanGenerationContext context, List<SeasonPlanProposal.Week> weeks,
                               List<String> violations) {
        if (weeks == null || weeks.size() != context.totalWeeks()) {
            violations.add("A quantidade de semanas deve ser exatamente " + context.totalWeeks());
            return;
        }
        boolean taperStarted = false;
        int consecutiveBuildWeeks = 0;
        double peakVolume = Math.max(context.currentWeeklyVolumeKm(), context.recentAverageWeeklyVolumeKm()) * 1.5;
        for (int index = 0; index < weeks.size(); index++) {
            var week = weeks.get(index);
            int number = index + 1;
            LocalDate expectedStart = context.planStartDate().plusWeeks(index);
            LocalDate expectedEnd = expectedStart.plusDays(6).isAfter(context.raceDate())
                    ? context.raceDate() : expectedStart.plusDays(6);
            if (!Objects.equals(week.weekNumber(), number)) violations.add("Numeração inválida na semana " + number);
            if (!Objects.equals(week.startDate(), expectedStart) || !Objects.equals(week.endDate(), expectedEnd))
                violations.add("Datas inválidas na semana " + number);
            if (week.targetVolumeKm() == null || week.targetVolumeKm() <= 0 || week.targetVolumeKm() > peakVolume)
                violations.add("Volume fora do limite na semana " + number);
            if (isBlank(week.focus())) violations.add("Foco ausente na semana " + number);
            if (week.recoveryWeek() == null || week.taperWeek() == null) {
                violations.add("Flags de recuperação e taper são obrigatórias na semana " + number);
                continue;
            }
            if (week.recoveryWeek() && week.taperWeek())
                violations.add("Uma semana não pode ser recuperação e taper ao mesmo tempo");
            if (taperStarted && !week.taperWeek()) violations.add("O taper deve ser contínuo até a prova");
            taperStarted |= week.taperWeek();

            if (week.recoveryWeek()) consecutiveBuildWeeks = 0;
            else if (!week.taperWeek()) consecutiveBuildWeeks++;
            if (context.totalWeeks() >= 8 && consecutiveBuildWeeks > 4)
                violations.add("Há mais de quatro semanas de carga sem recuperação");

            if (index > 0 && week.targetVolumeKm() != null && weeks.get(index - 1).targetVolumeKm() != null) {
                double prior = weeks.get(index - 1).targetVolumeKm();
                if (week.recoveryWeek() && week.targetVolumeKm() > prior * 0.90)
                    violations.add("Semana de recuperação deve reduzir o volume em pelo menos 10%");
                if (week.taperWeek() && week.targetVolumeKm() > prior * 0.90)
                    violations.add("Semana de taper deve reduzir o volume em pelo menos 10%");
                if (!week.recoveryWeek() && !week.taperWeek()
                        && !Boolean.TRUE.equals(weeks.get(index - 1).recoveryWeek())
                        && week.targetVolumeKm() > prior * 1.10 + 0.01)
                    violations.add("Progressão semanal superior a 10% na semana " + number);
            }
        }
        if (!Boolean.TRUE.equals(weeks.get(weeks.size() - 1).taperWeek()))
            violations.add("A semana da prova deve fazer parte do taper");
        if (context.totalWeeks() >= 8 && weeks.stream().noneMatch(week -> Boolean.TRUE.equals(week.recoveryWeek())))
            violations.add("Planos com oito semanas ou mais exigem semana de recuperação");
        long taperWeeks = weeks.stream().filter(week -> Boolean.TRUE.equals(week.taperWeek())).count();
        if (taperWeeks > 3) violations.add("O taper não pode exceder três semanas");
    }

    private void validatePhases(SeasonPlanGenerationContext context, List<SeasonPlanProposal.Phase> phases,
                                List<SeasonPlanProposal.Week> weeks, List<String> violations) {
        if (phases == null || phases.isEmpty()) {
            violations.add("O plano deve possuir fases");
            return;
        }
        int expectedStart = 1;
        for (int index = 0; index < phases.size(); index++) {
            var phase = phases.get(index);
            if (!Objects.equals(phase.order(), index + 1)) violations.add("Ordem inválida das fases");
            if (phase.phase() == null || isBlank(phase.objective()) || isBlank(phase.expectedProgression()))
                violations.add("Fase incompleta na posição " + (index + 1));
            if (!Objects.equals(phase.startWeek(), expectedStart) || phase.endWeek() == null
                    || phase.endWeek() < expectedStart || phase.endWeek() > context.totalWeeks()) {
                violations.add("Cobertura inválida da fase " + (index + 1));
                return;
            }
            expectedStart = phase.endWeek() + 1;
        }
        if (expectedStart != context.totalWeeks() + 1) violations.add("As fases não cobrem todas as semanas");
        if (weeks != null && !weeks.isEmpty() && phases.get(phases.size() - 1).phase()
                != com.aicoach.backend.enums.TrainingPhase.FINAL)
            violations.add("A fase final deve encerrar o plano");
    }

    private void validateRevisionCriteria(List<SeasonPlanProposal.RevisionCriterion> criteria,
                                          List<String> violations) {
        if (criteria == null) {
            violations.add("Critérios de revisão são obrigatórios");
            return;
        }
        Set<String> codes = new HashSet<>();
        for (var criterion : criteria) {
            if (criterion == null || isBlank(criterion.code()) || isBlank(criterion.description())) {
                violations.add("Critério de revisão incompleto");
            } else if (!codes.add(criterion.code())) {
                violations.add("Critério de revisão duplicado: " + criterion.code());
            }
        }
        if (!codes.containsAll(REQUIRED_REVISION_CRITERIA))
            violations.add("Faltam critérios obrigatórios de revisão");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
