package com.aicoach.backend.service;

import com.aicoach.backend.dto.AthleteAssessmentRequest;
import com.aicoach.backend.dto.AthleteAssessmentResponse;
import com.aicoach.backend.enums.*;
import com.aicoach.backend.models.Athlete;
import com.aicoach.backend.models.AthleteAssessment;
import com.aicoach.backend.repository.AthleteAssessmentRepo;
import com.aicoach.backend.repository.AthleteRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AthleteAssessmentServiceTest {

    @Mock AthleteRepo athleteRepo;
    @Mock AthleteAssessmentRepo assessmentRepo;
    @InjectMocks AthleteAssessmentService service;

    @Test
    void createsNextImmutableVersionWithStructuredContext() {
        Athlete athlete = new Athlete();
        athlete.setId(7L);
        AthleteAssessment previous = new AthleteAssessment();
        previous.setVersion(2);
        when(athleteRepo.findByIdForUpdate(7L)).thenReturn(Optional.of(athlete));
        when(assessmentRepo.findTopByAthleteIdOrderByVersionDesc(7L)).thenReturn(Optional.of(previous));
        when(assessmentRepo.save(any())).thenAnswer(invocation -> {
            AthleteAssessment saved = invocation.getArgument(0);
            saved.setId(21L);
            return saved;
        });

        AthleteAssessmentResponse response = service.createVersion(7L, validRequest(DayOfWeek.SUNDAY));

        ArgumentCaptor<AthleteAssessment> captor = ArgumentCaptor.forClass(AthleteAssessment.class);
        verify(assessmentRepo).save(captor.capture());
        AthleteAssessment saved = captor.getValue();
        assertEquals(3, response.version());
        assertEquals(AthleteAssessment.CURRENT_SCHEMA_VERSION, response.schemaVersion());
        assertEquals(2, saved.getAvailability().size());
        assertTrue(saved.getAvailability().stream().allMatch(item -> item.getAssessment() == saved));
        assertEquals(1, saved.getHealthIssues().size());
        assertSame(saved, saved.getHealthIssues().get(0).getAssessment());
        assertEquals(10000, response.targetRace().distanceMeters());
        assertEquals(3000, response.targetRace().desiredTimeSeconds());
    }

    @Test
    void rejectsPreferredLongRunDayOutsideAvailability() {
        Athlete athlete = new Athlete();
        athlete.setId(7L);
        when(athleteRepo.findByIdForUpdate(7L)).thenReturn(Optional.of(athlete));

        AssessmentValidationException exception = assertThrows(AssessmentValidationException.class,
                () -> service.createVersion(7L, validRequest(DayOfWeek.FRIDAY)));

        assertTrue(exception.getMessage().contains("dias disponíveis"));
        verify(assessmentRepo, never()).save(any());
    }

    @Test
    void rejectsDuplicateAvailabilityDays() {
        AthleteAssessmentRequest base = validRequest(DayOfWeek.SUNDAY);
        AthleteAssessmentRequest duplicate = new AthleteAssessmentRequest(
                base.onboardingStatus(), base.physicalProfile(), base.runningProfile(),
                List.of(new AthleteAssessmentRequest.Availability(DayOfWeek.SUNDAY, 90),
                        new AthleteAssessmentRequest.Availability(DayOfWeek.SUNDAY, 120)),
                base.preferredLongRunDay(), base.surfaces(), base.equipment(), base.strengthTraining(),
                base.health(), base.recovery(), base.targetRace());
        Athlete athlete = new Athlete();
        athlete.setId(7L);
        when(athleteRepo.findByIdForUpdate(7L)).thenReturn(Optional.of(athlete));

        assertThrows(AssessmentValidationException.class, () -> service.createVersion(7L, duplicate));
        verify(assessmentRepo, never()).save(any());
    }

    public static AthleteAssessmentRequest validRequest(DayOfWeek longRunDay) {
        return new AthleteAssessmentRequest(
                OnboardingStatus.COMPLETED,
                new AthleteAssessmentRequest.PhysicalProfile(
                        LocalDate.of(1990, 1, 1), 70.0, 175, Gender.MALE),
                new AthleteAssessmentRequest.RunningProfile(
                        RunningExperienceLevel.INTERMEDIATE, 4, 35.0, 32.0, 14.0, 4),
                List.of(new AthleteAssessmentRequest.Availability(DayOfWeek.TUESDAY, 60),
                        new AthleteAssessmentRequest.Availability(DayOfWeek.SUNDAY, 120)),
                longRunDay,
                Set.of(RunningSurface.ROAD, RunningSurface.TRACK),
                Set.of(TrainingEquipment.GPS_WATCH, TrainingEquipment.HEART_RATE_MONITOR),
                new AthleteAssessmentRequest.StrengthTraining(2, "Treino funcional"),
                new AthleteAssessmentRequest.HealthProfile(false, null,
                        List.of(new AthleteAssessmentRequest.HealthIssue("joelho", "Lesão antiga",
                                HealthIssueStatus.RESOLVED, 0, LocalDate.of(2024, 1, 1), null))),
                new AthleteAssessmentRequest.RecoveryProfile(
                        7.5, SleepQuality.GOOD, 2, RoutineType.FIXED, "Trabalho diurno"),
                new AthleteAssessmentRequest.TargetRace(
                        "10K", LocalDate.now().plusMonths(6), 10000, 3000, RacePriority.A_RACE));
    }
}
