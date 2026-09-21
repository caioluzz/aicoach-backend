package com.aicoach.backend.models;

import com.aicoach.backend.enums.RacePriority;
import com.aicoach.backend.enums.SecondaryRaceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "secondary_races", uniqueConstraints = @UniqueConstraint(
        name = "uk_secondary_race_plan_date", columnNames = {"global_plan_id", "race_date"}))
@Getter
@Setter
public class SecondaryRace {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "global_plan_id", nullable = false)
    private GlobalPlan globalPlan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "primary_objective_id", nullable = false)
    private Objective primaryObjective;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "race_date", nullable = false)
    private LocalDate raceDate;

    @Column(name = "distance_m", nullable = false)
    private Integer distanceMeters;

    @Column(name = "target_time_s")
    private Integer targetTimeSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RacePriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SecondaryRaceStatus status;

    @Column(name = "rule_version", nullable = false, length = 50)
    private String ruleVersion;

    @Column(name = "window_start", nullable = false)
    private LocalDate windowStart;

    @Column(name = "window_end", nullable = false)
    private LocalDate windowEnd;

    @Column(nullable = false, length = 2000)
    private String rationale;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "secondaryRace", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("proposedDate ASC, id ASC")
    private List<SecondaryRaceAdjustment> adjustments = new ArrayList<>();
}
