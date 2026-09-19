package com.aicoach.backend.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static com.aicoach.backend.service.AthleteAssessmentServiceTest.validRequest;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AthleteAssessmentRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsPastTargetRace() {
        AthleteAssessmentRequest base = validRequest(DayOfWeek.SUNDAY);
        AthleteAssessmentRequest.TargetRace invalidRace = new AthleteAssessmentRequest.TargetRace(
                base.targetRace().title(), LocalDate.now().minusDays(1), base.targetRace().distanceMeters(),
                base.targetRace().desiredTimeSeconds(), base.targetRace().priority());
        AthleteAssessmentRequest request = copyWithRace(base, invalidRace);

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("targetRace.date")));
    }

    @Test
    void rejectsUnrealisticDailyAvailability() {
        AthleteAssessmentRequest base = validRequest(DayOfWeek.SUNDAY);
        AthleteAssessmentRequest request = new AthleteAssessmentRequest(
                base.onboardingStatus(), base.physicalProfile(), base.runningProfile(),
                java.util.List.of(new AthleteAssessmentRequest.Availability(DayOfWeek.SUNDAY, 1441)),
                base.preferredLongRunDay(), base.surfaces(), base.equipment(), base.strengthTraining(),
                base.health(), base.recovery(), base.targetRace());

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString()
                        .equals("availability[0].availableMinutes")));
    }

    private AthleteAssessmentRequest copyWithRace(AthleteAssessmentRequest base,
                                                   AthleteAssessmentRequest.TargetRace race) {
        return new AthleteAssessmentRequest(
                base.onboardingStatus(), base.physicalProfile(), base.runningProfile(), base.availability(),
                base.preferredLongRunDay(), base.surfaces(), base.equipment(), base.strengthTraining(),
                base.health(), base.recovery(), race);
    }
}
