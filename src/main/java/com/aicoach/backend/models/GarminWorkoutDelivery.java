package com.aicoach.backend.models;

import com.aicoach.backend.enums.GarminDeliveryStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "garmin_workout_deliveries", uniqueConstraints = @UniqueConstraint(
        name = "uk_garmin_delivery_idempotency",
        columnNames = {"athlete_id", "planned_activity_id", "plan_version", "content_hash"}))
@Getter
@Setter
public class GarminWorkoutDelivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "weekly_plan_id", nullable = false)
    private WeeklyPlan weeklyPlan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planned_activity_id", nullable = false)
    private PlannedActivity plannedActivity;

    @Column(name = "plan_version", nullable = false)
    private Integer planVersion;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GarminDeliveryStatus status;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "external_workout_id")
    private Long externalWorkoutId;

    @Column(name = "external_schedule_id")
    private Long externalScheduleId;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;
}
