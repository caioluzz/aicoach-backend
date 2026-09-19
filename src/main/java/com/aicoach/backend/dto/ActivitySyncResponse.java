package com.aicoach.backend.dto;

import com.aicoach.backend.enums.ActivitySyncStatus;

import java.time.LocalDateTime;

public record ActivitySyncResponse(
        Long athleteId,
        ActivitySyncStatus status,
        LocalDateTime checkpointStartedAt,
        LocalDateTime lastAttemptAt,
        LocalDateTime lastSuccessAt,
        int discoveredCount,
        int importedCount,
        int skippedCount,
        String lastError
) {}
