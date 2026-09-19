package com.aicoach.backend.seasonplan;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class SeasonPlanValidatorTest {
    private final SeasonPlanValidator validator = new SeasonPlanValidator();

    @Test
    void acceptsCoherentStrategicPlan() {
        var context = SeasonPlanFixtures.context(LocalDate.of(2026, 1, 1), 8);
        assertDoesNotThrow(() -> validator.validate(context, SeasonPlanFixtures.validProposal(context)));
    }

    @Test
    void rejectsPlanWithoutRecoveryAndWithBrokenDates() {
        var context = SeasonPlanFixtures.context(LocalDate.of(2026, 1, 1), 8);
        var valid = SeasonPlanFixtures.validProposal(context);
        var weeks = new ArrayList<>(valid.weeks());
        var fourth = weeks.get(3);
        weeks.set(3, new SeasonPlanProposal.Week(4, fourth.startDate().plusDays(1), fourth.endDate(),
                42.0, fourth.focus(), false, false));
        var invalid = new SeasonPlanProposal(valid.summary(), valid.phases(), weeks, valid.revisionCriteria());

        SeasonPlanValidationException error = assertThrows(SeasonPlanValidationException.class,
                () -> validator.validate(context, invalid));

        assertTrue(error.getViolations().stream().anyMatch(item -> item.contains("Datas inválidas")));
        assertTrue(error.getViolations().stream().anyMatch(item -> item.contains("recuperação")));
    }

    @Test
    void rejectsMissingMaterialRevisionCriterion() {
        var context = SeasonPlanFixtures.context(LocalDate.of(2026, 1, 1), 8);
        var valid = SeasonPlanFixtures.validProposal(context);
        var invalid = new SeasonPlanProposal(valid.summary(), valid.phases(), valid.weeks(),
                valid.revisionCriteria().stream().filter(item -> !item.code().equals("HEALTH_CHANGE")).toList());

        assertThrows(SeasonPlanValidationException.class, () -> validator.validate(context, invalid));
    }
}
