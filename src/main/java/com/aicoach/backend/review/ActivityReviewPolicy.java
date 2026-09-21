package com.aicoach.backend.review;

import com.aicoach.backend.enums.ComplianceClassification;
import com.aicoach.backend.models.Activity;
import com.aicoach.backend.models.ActivityComparison;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ActivityReviewPolicy {
    public static final String VERSION = "activity-review-policy-v1";

    public Decision decide(ActivityComparison comparison) {
        Activity activity = comparison.getActivity();
        if (activity == null || comparison.getClassification() == ComplianceClassification.NOT_EXECUTED) {
            return Decision.skip("Não há atividade executada nem telemetria para interpretar.",
                    "Treino não executado; não há atividade para avaliar.");
        }
        if (number(activity.getDurationSeconds()) < 600 || number(activity.getDistanceMeters()) < 1000) {
            return Decision.skip("Atividade trivial: menos de 10 minutos ou de 1 km.",
                    "Atividade muito curta; o resumo determinístico é suficiente e não justifica análise por IA.");
        }
        boolean allStepsFulfilled = comparison.getSteps().stream()
                .allMatch(step -> step.getClassification() == ComplianceClassification.FULFILLED);
        if (comparison.getClassification() == ComplianceClassification.FULFILLED
                && percentage(comparison.getCompliancePercentage()) >= 95 && allStepsFulfilled) {
            return Decision.skip("Atividade plenamente cumprida (>= 95%) sem etapa discrepante.",
                    "Treino cumprido conforme o planejado; não foram encontradas discrepâncias que agreguem valor a uma análise adicional.");
        }
        return Decision.callModel("O resumo determinístico contém diferença relevante que pode se beneficiar de interpretação curta.");
    }

    private double number(Number value) { return value == null ? 0 : value.doubleValue(); }
    private double percentage(BigDecimal value) { return value == null ? 0 : value.doubleValue(); }

    public record Decision(boolean callModel, String reason, String deterministicAssessment) {
        static Decision skip(String reason, String assessment) {
            return new Decision(false, reason, assessment);
        }
        static Decision callModel(String reason) {
            return new Decision(true, reason, null);
        }
    }
}
