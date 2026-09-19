package com.aicoach.backend.models;

import com.aicoach.backend.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "athlete_assessments", uniqueConstraints =
        @UniqueConstraint(name = "uk_assessment_athlete_version", columnNames = {"athlete_id", "version"}))
@Getter
@Setter
public class AthleteAssessment {

    public static final String CURRENT_SCHEMA_VERSION = "1.0";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "schema_version", nullable = false, length = 20)
    private String schemaVersion = CURRENT_SCHEMA_VERSION;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", nullable = false, length = 20)
    private OnboardingStatus onboardingStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "weight_kg", nullable = false)
    private Double weightKg;

    @Column(name = "height_cm", nullable = false)
    private Integer heightCm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", nullable = false, length = 20)
    private RunningExperienceLevel experienceLevel;

    @Column(name = "running_years", nullable = false)
    private Integer runningYears;

    @Column(name = "current_weekly_volume_km", nullable = false)
    private Double currentWeeklyVolumeKm;

    @Column(name = "recent_average_weekly_volume_km", nullable = false)
    private Double recentAverageWeeklyVolumeKm;

    @Column(name = "recent_longest_run_km", nullable = false)
    private Double recentLongestRunKm;

    @Column(name = "current_runs_per_week", nullable = false)
    private Integer currentRunsPerWeek;

    @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayOfWeek ASC")
    private List<AssessmentAvailability> availability = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_long_run_day", nullable = false, length = 20)
    private DayOfWeek preferredLongRunDay;

    @ElementCollection(targetClass = RunningSurface.class)
    @CollectionTable(name = "assessment_surfaces", joinColumns = @JoinColumn(name = "assessment_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "surface", nullable = false, length = 20)
    private Set<RunningSurface> surfaces = new HashSet<>();

    @ElementCollection(targetClass = TrainingEquipment.class)
    @CollectionTable(name = "assessment_equipment", joinColumns = @JoinColumn(name = "assessment_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "equipment", nullable = false, length = 40)
    private Set<TrainingEquipment> equipment = new HashSet<>();

    @Column(name = "strength_sessions_per_week", nullable = false)
    private Integer strengthSessionsPerWeek;

    @Column(name = "strength_training_notes", length = 500)
    private String strengthTrainingNotes;

    @Column(name = "has_medical_restrictions", nullable = false)
    private Boolean hasMedicalRestrictions;

    @Column(name = "medical_restrictions", length = 1000)
    private String medicalRestrictions;

    @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssessmentHealthIssue> healthIssues = new ArrayList<>();

    @Column(name = "average_sleep_hours", nullable = false)
    private Double averageSleepHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "sleep_quality", nullable = false, length = 20)
    private SleepQuality sleepQuality;

    @Column(name = "recovery_days_per_week", nullable = false)
    private Integer recoveryDaysPerWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "routine_type", nullable = false, length = 20)
    private RoutineType routineType;

    @Column(name = "routine_notes", length = 500)
    private String routineNotes;

    @Column(name = "target_race_title", nullable = false)
    private String targetRaceTitle;

    @Column(name = "target_race_date", nullable = false)
    private LocalDate targetRaceDate;

    @Column(name = "target_race_distance_m", nullable = false)
    private Integer targetRaceDistanceMeters;

    @Column(name = "target_race_time_seconds", nullable = false)
    private Integer targetRaceTimeSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_race_priority", nullable = false, length = 20)
    private RacePriority targetRacePriority;

    @PrePersist
    void initializeTimestamps() {
        createdAt = Instant.now();
        if (onboardingStatus == OnboardingStatus.COMPLETED) {
            completedAt = createdAt;
        }
    }
}
