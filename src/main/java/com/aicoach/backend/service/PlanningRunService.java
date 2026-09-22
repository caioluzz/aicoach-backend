package com.aicoach.backend.service;

import com.aicoach.backend.dto.*;
import com.aicoach.backend.enums.SeasonPlanStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanningRunService {
    private final ActivityService activityService;
    private final SeasonPlanService seasonPlanService;
    private final WeeklyPlanService weeklyPlanService;
    private final Clock clock;

    @Transactional
    public PlanningRunResponse synchronizeAndGenerate(Long athleteId) {
        ActivitySyncResponse sync = activityService.syncGarminActivities(athleteId);
        LocalDate today = LocalDate.now(clock);
        List<SeasonPlanResponse> plans = seasonPlanService.getHistory(athleteId);
        SeasonPlanResponse plan = plans.stream()
                .filter(item -> item.status() == SeasonPlanStatus.APPROVED)
                .findFirst().orElse(null);
        int considered = sync.discoveredCount();
        if (plan == null) {
            return new PlanningRunResponse(athleteId, "NEEDS_PLAN_APPROVAL", false, "REGULAR",
                    today, today.plusDays(6), considered, 0, null, sync,
                    "A sincronização terminou. Aprove o plano geral antes de gerar a semana.");
        }

        boolean sunday = today.getDayOfWeek().getValue() == 7;
        SeasonPlanResponse.Week week = sunday
                ? plan.weeks().stream().filter(item -> item.startDate().isAfter(today)).findFirst().orElse(null)
                : plan.weeks().stream()
                .filter(item -> !today.isBefore(item.startDate()) && !today.isAfter(item.endDate()))
                .findFirst()
                .orElseGet(() -> plan.weeks().stream()
                        .filter(item -> !item.endDate().isBefore(today))
                        .findFirst()
                        .orElse(plan.weeks().stream().max(Comparator.comparing(SeasonPlanResponse.Week::weekNumber)).orElse(null)));
        if (week == null) {
            return new PlanningRunResponse(athleteId, "NO_CURRENT_WEEK", false, "REGULAR",
                    today, today.plusDays(6), considered, 0, null, sync,
                    "O plano geral não possui uma semana compatível com a data atual.");
        }

        List<WeeklyPlanResponse> history = weeklyPlanService.getHistory(athleteId, plan.id(), week.weekNumber());
        if (!history.isEmpty()) {
            WeeklyPlanResponse current = history.get(0);
            String reusedType = !sunday && current.weekStart().equals(today) ? "BRIDGE" : "REGULAR";
            return new PlanningRunResponse(athleteId, "REUSED", true, reusedType, current.weekStart(),
                    current.weekEnd(), considered, 0, current.id(), sync,
                    "A proposta equivalente já existia e foi reutilizada.");
        }

        boolean bridge = !sunday;
        WeeklyPlanResponse generated = bridge
                ? weeklyPlanService.createBridge(athleteId,
                new WeeklyPlanCreateRequest(plan.id(), week.weekNumber()), today, week.endDate())
                : weeklyPlanService.create(athleteId,
                new WeeklyPlanCreateRequest(plan.id(), week.weekNumber()));
        return new PlanningRunResponse(athleteId, "PROPOSAL_CREATED", false,
                bridge ? "BRIDGE" : "REGULAR",
                generated.weekStart(), generated.weekEnd(), considered, 0, generated.id(), sync,
                "Sincronização concluída e proposta semanal validada para revisão humana.");
    }
}
