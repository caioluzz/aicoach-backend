package com.aicoach.backend.models;

import com.aicoach.backend.enums.ActivitySyncStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_sync_state")
@Data
public class ActivitySyncState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false, unique = true)
    private Athlete athlete;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivitySyncStatus status = ActivitySyncStatus.NEVER_RUN;

    @Column(name = "checkpoint_started_at")
    private LocalDateTime checkpointStartedAt;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "discovered_count", nullable = false)
    private int discoveredCount;

    @Column(name = "imported_count", nullable = false)
    private int importedCount;

    @Column(name = "skipped_count", nullable = false)
    private int skippedCount;
}
