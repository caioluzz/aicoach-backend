package com.aicoach.backend.service;

import com.aicoach.backend.client.GarminBotClient;
import com.aicoach.backend.client.GarminAdapterException;
import com.aicoach.backend.dto.*;
import com.aicoach.backend.enums.ActivitySyncStatus;
import com.aicoach.backend.models.Activity;
import com.aicoach.backend.models.ActivitySyncState;
import com.aicoach.backend.models.Athlete;
import com.aicoach.backend.repository.ActivityRepo;
import com.aicoach.backend.repository.ActivitySyncStateRepo;
import com.aicoach.backend.repository.AthleteRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {
    @Mock ActivityRepo activityRepo;
    @Mock AthleteRepo athleteRepo;
    @Mock ActivitySyncStateRepo syncStateRepo;
    @Mock GarminBotClient garminBotClient;
    @Mock ActivityComparisonService activityComparisonService;
    @InjectMocks ActivityService activityService;

    private Athlete athlete;

    @BeforeEach
    void setUp() {
        athlete = new Athlete();
        athlete.setId(7L);
        athlete.setGarminEmail("runner@example.test");
        athlete.setGarminPassword("credential");
        ReflectionTestUtils.setField(activityService, "maxAttempts", 2);
        ReflectionTestUtils.setField(activityService, "retryDelayMs", 0L);
        ReflectionTestUtils.setField(activityService, "discoveryLimit", 100);
        ReflectionTestUtils.setField(activityService, "recoveryWindowHours", 24L);
        lenient().when(athleteRepo.findById(7L)).thenReturn(Optional.of(athlete));
        lenient().when(syncStateRepo.findByAthleteId(7L)).thenReturn(Optional.empty());
        lenient().when(syncStateRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void rejectsDuplicateGarminActivity() {
        Activity activity = new Activity();
        activity.setGarminActivityId(42L);
        when(activityRepo.existsByGarminActivityId(42L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> activityService.saveActivity(activity));
        verify(activityRepo, never()).save(any());
    }

    @Test
    void discoversMetadataButDownloadsAndImportsOnlyNewActivities() {
        LocalDateTime oldTime = LocalDateTime.of(2026, 9, 17, 6, 30);
        LocalDateTime newTime = LocalDateTime.of(2026, 9, 18, 6, 30);
        when(garminBotClient.discoverActivities(anyString(), anyString(), eq(100), isNull()))
                .thenReturn(List.of(
                        new GarminActivityMetadata(98L, "Já importada", oldTime, false),
                        new GarminActivityMetadata(99L, "Teste 3km", newTime, true)));
        when(activityRepo.existsByGarminActivityId(98L)).thenReturn(true);
        when(activityRepo.existsByGarminActivityId(99L)).thenReturn(false);
        when(garminBotClient.downloadActivity(anyString(), anyString(), eq(99L)))
                .thenReturn(activityDetail(99L, newTime));
        when(activityRepo.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ActivitySyncResponse response = activityService.syncGarminActivities(7L);

        assertEquals(ActivitySyncStatus.SUCCESS, response.status());
        assertEquals(2, response.discoveredCount());
        assertEquals(1, response.importedCount());
        assertEquals(1, response.skippedCount());
        assertEquals(newTime, response.checkpointStartedAt());
        verify(garminBotClient, never()).downloadActivity(anyString(), anyString(), eq(98L));
        verify(garminBotClient, times(1)).downloadActivity(anyString(), anyString(), eq(99L));

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepo).saveAndFlush(captor.capture());
        assertTrue(captor.getValue().getIsVdotTest());
        assertEquals(1, captor.getValue().getLaps().size());
        assertEquals(1, captor.getValue().getRecords().size());
    }

    @Test
    void appliesRecoveryWindowToPersistedCheckpoint() {
        LocalDateTime checkpoint = LocalDateTime.of(2026, 9, 18, 12, 0);
        ActivitySyncState state = new ActivitySyncState();
        state.setAthlete(athlete);
        state.setCheckpointStartedAt(checkpoint);
        when(syncStateRepo.findByAthleteId(7L)).thenReturn(Optional.of(state));
        when(garminBotClient.discoverActivities(
                "runner@example.test", "credential", 100, checkpoint.minusHours(24)))
                .thenReturn(List.of());

        ActivitySyncResponse response = activityService.syncGarminActivities(7L);

        assertEquals(ActivitySyncStatus.SUCCESS, response.status());
        assertEquals(checkpoint, response.checkpointStartedAt());
    }

    @Test
    void repeatedDiscoveryNeverDownloadsAnAlreadyImportedActivityAgain() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 9, 18, 6, 30);
        GarminActivityMetadata metadata =
                new GarminActivityMetadata(99L, "Corrida", startedAt, false);
        when(garminBotClient.discoverActivities(anyString(), anyString(), eq(100), isNull()))
                .thenReturn(List.of(metadata));
        when(activityRepo.existsByGarminActivityId(99L)).thenReturn(false, false, true);
        when(garminBotClient.downloadActivity(anyString(), anyString(), eq(99L)))
                .thenReturn(activityDetail(99L, startedAt));
        when(activityRepo.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        activityService.syncGarminActivities(7L);
        ActivitySyncResponse secondRun = activityService.syncGarminActivities(7L);

        verify(garminBotClient, times(1))
                .downloadActivity(anyString(), anyString(), eq(99L));
        assertEquals(0, secondRun.importedCount());
        assertEquals(1, secondRun.skippedCount());
    }

    @Test
    void retriesDiscoveryAndPersistsObservableFailureWithoutMovingCheckpoint() {
        when(garminBotClient.discoverActivities(anyString(), anyString(), anyInt(), isNull()))
                .thenThrow(new IllegalStateException("temporary"));

        ActivitySyncResponse response = activityService.syncGarminActivities(7L);

        assertEquals(ActivitySyncStatus.FAILED, response.status());
        assertNull(response.checkpointStartedAt());
        assertTrue(response.lastError().contains("IllegalStateException"));
        verify(garminBotClient, times(2))
                .discoverActivities(anyString(), anyString(), anyInt(), isNull());
    }

    @Test
    void importsOneActivityDirectlyByGarminIdWithoutDiscovery() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 9, 18, 6, 30);
        when(activityRepo.findByGarminActivityId(99L)).thenReturn(Optional.empty());
        when(garminBotClient.downloadActivity("runner@example.test", "credential", 99L))
                .thenReturn(activityDetail(99L, startedAt));
        when(activityRepo.saveAndFlush(any())).thenAnswer(invocation -> {
            Activity activity = invocation.getArgument(0);
            activity.setId(55L);
            return activity;
        });

        ActivitySummaryResponse response = activityService.importGarminActivity(7L, 99L);

        assertEquals(55L, response.id());
        assertEquals(99L, response.garminActivityId());
        verify(garminBotClient, never()).discoverActivities(anyString(), anyString(), anyInt(), any());
        verify(garminBotClient).downloadActivity("runner@example.test", "credential", 99L);
    }

    @Test
    void doesNotRetryRejectedCredentialsDuringDirectImport() {
        when(activityRepo.findByGarminActivityId(99L)).thenReturn(Optional.empty());
        when(garminBotClient.downloadActivity(anyString(), anyString(), eq(99L)))
                .thenThrow(new GarminAdapterException(401, "Credenciais rejeitadas", false));

        GarminAdapterException failure = assertThrows(GarminAdapterException.class,
                () -> activityService.importGarminActivity(7L, 99L));

        assertEquals(401, failure.getStatusCode());
        verify(garminBotClient, times(1)).downloadActivity(anyString(), anyString(), eq(99L));
    }

    @Test
    void repairsMissingSportWhenDirectImportAlreadyExists() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 9, 21, 20, 48);
        Activity existing = new Activity();
        existing.setId(55L);
        existing.setAthlete(athlete);
        existing.setGarminActivityId(99L);
        existing.setName("Teste 3km");
        existing.setStartedAt(startedAt);
        existing.setDistanceMeters(3014.83);
        existing.setDurationSeconds(1142.936);
        existing.setIsVdotTest(true);
        when(activityRepo.findByGarminActivityId(99L)).thenReturn(Optional.of(existing));
        when(garminBotClient.downloadActivity("runner@example.test", "credential", 99L))
                .thenReturn(activityDetail(99L, startedAt));
        when(activityRepo.saveAndFlush(existing)).thenReturn(existing);

        ActivitySummaryResponse response = activityService.importGarminActivity(7L, 99L);

        assertEquals("running", response.sport());
        verify(activityRepo).saveAndFlush(existing);
    }

    private GarminBotResponseDTO activityDetail(Long id, LocalDateTime startedAt) {
        LapDTO lap = new LapDTO((short) 1, "active", startedAt,
                new BigDecimal("720.5"), new BigDecimal("3.000"), (short) 240,
                new BigDecimal("15.00"), (short) 170, (short) 185,
                (short) 88, (short) 94, (short) 10, (short) 8);
        ActivityRecordDTO record = new ActivityRecordDTO(startedAt, 0,
                BigDecimal.ZERO, new BigDecimal("14.90"), (short) 242,
                (short) 168, (short) 87, new BigDecimal("700.0"));
        return new GarminBotResponseDTO(
                id, "Teste 3km", 3000.0, 720.5, startedAt, 170, 4.16,
                null, "running", "generic", true, startedAt.plusSeconds(721),
                new BigDecimal("18.20"), (short) 240, (short) 210, (short) 185,
                (short) 88, (short) 94, (short) 10, (short) 8,
                new BigDecimal("695.0"), new BigDecimal("710.0"), (short) 1,
                (short) 1, null, List.of(lap), List.of(record));
    }
}
