package com.aicoach.backend.service;

import com.aicoach.backend.dto.AthleteRequestDTO;
import com.aicoach.backend.dto.ObjectiveRequestDTO;
import com.aicoach.backend.enums.Gender;
import com.aicoach.backend.enums.ObjectiveStatus;
import com.aicoach.backend.enums.RacePriority;
import com.aicoach.backend.models.Athlete;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AthleteServiceTest {

    @Mock
    private AthleteRepo athleteRepo;
    @InjectMocks
    private AthleteService athleteService;

    @Test
    void mapsAthleteAndObjectivesBeforeSaving() {
        AthleteRequestDTO request = new AthleteRequestDTO(
                "Runner", "runner@example.test", "credential",
                LocalDate.of(1990, 1, 1), 70.0, 175, Gender.MALE,
                Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
                List.of(new ObjectiveRequestDTO(
                        "10K", 10000, LocalDate.of(2027, 1, 10),
                        RacePriority.A_RACE.name(), ObjectiveStatus.ACTIVE.name())));
        when(athleteRepo.save(any(Athlete.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Athlete saved = athleteService.saveAthlete(request);

        ArgumentCaptor<Athlete> captor = ArgumentCaptor.forClass(Athlete.class);
        verify(athleteRepo).save(captor.capture());
        assertSame(saved, captor.getValue());
        assertEquals("Runner", saved.getName());
        assertEquals(1, saved.getObjectives().size());
        assertSame(saved, saved.getObjectives().get(0).getAthlete());
        assertEquals(RacePriority.A_RACE, saved.getObjectives().get(0).getPriority());
        assertEquals(ObjectiveStatus.ACTIVE, saved.getObjectives().get(0).getStatus());
    }
}
