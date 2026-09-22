package com.aicoach.backend.models;

import com.aicoach.backend.utils.EncryptionConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "athlete_openai_settings")
@Data
public class AthleteOpenAiSettings {
    @Id
    @Column(name = "athlete_id")
    private Long athleteId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "athlete_id")
    private Athlete athlete;

    @Convert(converter = EncryptionConverter.class)
    @Column(name = "api_key", nullable = false)
    private String apiKey;

    @Column(name = "planner_model", nullable = false, length = 100)
    private String plannerModel;

    @Column(name = "weekly_planner_model", nullable = false, length = 100)
    private String weeklyPlannerModel;

    @Column(name = "activity_review_model", nullable = false, length = 100)
    private String activityReviewModel;

    @Column(name = "max_output_tokens", nullable = false)
    private Integer maxOutputTokens;

    @Column(name = "weekly_max_output_tokens", nullable = false)
    private Integer weeklyMaxOutputTokens;

    @Column(name = "activity_review_max_output_tokens", nullable = false)
    private Integer activityReviewMaxOutputTokens;

    @Column(name = "last_validated_at")
    private Instant lastValidatedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
