package com.aicoach.backend.service;

import com.aicoach.backend.client.GarminBotClient;
import com.aicoach.backend.dto.ActivityRecordDTO;
import com.aicoach.backend.dto.GarminBotResponseDTO;
import com.aicoach.backend.dto.LapDTO;
import com.aicoach.backend.models.Activity;
import com.aicoach.backend.models.Athlete;
import com.aicoach.backend.repository.ActivityRepo;
import com.aicoach.backend.repository.AthleteRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityRepo activityRepo;
    @Mock
    private AthleteRepo athleteRepo;
    @Mock
    private GarminBotClient garminBotClient;
    @InjectMocks
    private ActivityService activityService;

    @Test
    void rejectsDuplicateGarminActivity() {
        Activity activity = new Activity();
        activity.setGarminActivityId(42L);
        when(activityRepo.existsByGarminActivityId(42L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> activityService.saveActivity(activity));
        verify(activityRepo, never()).save(any());
    }

    @Test
    void firstSyncPersistsVdotTestWithLapsAndTelemetry() {
        Athlete athlete = new Athlete();
        athlete.setId(7L);
        athlete.setGarminEmail("runner@example.test");
        athlete.setGarminPassword("credential");
        LocalDateTime startedAt = LocalDateTime.of(2026, 9, 18, 6, 30);
        LapDTO lap = new LapDTO((short) 1, "active", startedAt,
                new BigDecimal("720.5"), new BigDecimal("3.000"), (short) 240,
                new BigDecimal("15.00"), (short) 170, (short) 185,
                (short) 88, (short) 94, (short) 10, (short) 8);
        ActivityRecordDTO record = new ActivityRecordDTO(startedAt, 0,
                BigDecimal.ZERO, new BigDecimal("14.90"), (short) 242,
                (short) 168, (short) 87, new BigDecimal("700.0"));
        GarminBotResponseDTO response = new GarminBotResponseDTO(
                99L, "Teste 3km", 3000.0, 720.5, startedAt, 170, 4.16,
                null, "running", "generic", true, startedAt.plusSeconds(721),
                new BigDecimal("18.20"), (short) 240, (short) 210, (short) 185,
                (short) 88, (short) 94, (short) 10, (short) 8,
                new BigDecimal("695.0"), new BigDecimal("710.0"), (short) 1,
                (short) 1, null, List.of(lap), List.of(record));

        when(athleteRepo.findById(7L)).thenReturn(Optional.of(athlete));
        when(activityRepo.countByAthleteId(7L)).thenReturn(0L);
        when(garminBotClient.fetchActivities("runner@example.test", "credential", 50))
                .thenReturn(List.of(response));

        activityService.syncGarminActivities(7L);

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepo).save(captor.capture());
        Activity saved = captor.getValue();
        assertTrue(saved.getIsVdotTest());
        assertEquals(99L, saved.getGarminActivityId());
        assertEquals(1, saved.getLaps().size());
        assertSame(saved, saved.getLaps().get(0).getActivity());
        assertEquals(1, saved.getRecords().size());
        assertSame(saved, saved.getRecords().get(0).getActivity());
        assertEquals(startedAt, saved.getRecords().get(0).getId().getTs());
    }
}
