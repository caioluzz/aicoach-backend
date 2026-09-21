package com.aicoach.backend.adaptation;

import com.aicoach.backend.enums.AdaptationAlertLevel;
import com.aicoach.backend.enums.ComplianceClassification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AdaptationRuleEngine {
    public static final String VERSION = "feedback-adaptation-rules-v1";

    public Decision evaluate(Input input) {
        if (input.painSeverity() >= 7) {
            return new Decision(AdaptationAlertLevel.STOP_AND_ASSESS, 100, false, true, true,
                    "Dor grave relatada (>= 7/10)",
                    "Suspender os próximos treinos e avaliar a dor antes de retomar o plano.");
        }
        if (input.painSeverity() >= 4) {
            boolean material = input.recentLoadReductionDecisions() >= 2;
            return new Decision(AdaptationAlertLevel.REDUCE_LOAD, 40, false, true, material,
                    material ? "Dor moderada recorrente em pelo menos três decisões na janela" : null,
                    "Reduzir carga e retirar intensidade por dor moderada.");
        }
        if (input.fatigue() >= 8 && input.sleepHours() <= 6.0) {
            boolean material = input.recentLoadReductionDecisions() >= 2;
            return new Decision(AdaptationAlertLevel.REDUCE_LOAD, 35, false, true, material,
                    material ? "Fadiga alta e sono insuficiente recorrentes" : null,
                    "Reduzir carga e intensidade por fadiga alta combinada com sono insuficiente.");
        }
        if (input.perceivedEffort() >= 9 && isExecutionConcern(input.classification(), input.compliancePercentage())) {
            return new Decision(AdaptationAlertLevel.REDUCE_LOAD, 25, false, true, false, null,
                    "Reduzir a carga dos próximos treinos: esforço muito alto e execução discrepante.");
        }
        if (input.fatigue() >= 7 || input.sleepHours() < 6.0 || input.perceivedEffort() >= 9) {
            return new Decision(AdaptationAlertLevel.MONITOR, 0, true, false, false, null,
                    "Monitorar recuperação; uma ocorrência isolada não justifica replanejamento.");
        }
        return new Decision(AdaptationAlertLevel.NONE, 0, true, false, false, null,
                "Feedback e execução dentro dos limites; manter os próximos treinos.");
    }

    private boolean isExecutionConcern(ComplianceClassification classification, BigDecimal percentage) {
        if (classification == null) return false;
        return classification == ComplianceClassification.PARTIAL
                || classification == ComplianceClassification.DIFFERENT
                || classification == ComplianceClassification.EXCEEDED
                || classification == ComplianceClassification.NOT_EXECUTED
                || percentage != null && percentage.compareTo(new BigDecimal("75")) < 0;
    }

    public record Input(int perceivedEffort, int fatigue, double sleepHours, int painSeverity,
                        ComplianceClassification classification, BigDecimal compliancePercentage,
                        long recentLoadReductionDecisions) {}

    public record Decision(AdaptationAlertLevel alertLevel, int loadReductionPercent,
                           boolean allowIntensity, boolean weeklyReviewRequired,
                           boolean seasonPlanReviewProposed, String materialCause, String rationale) {}
}
