package com.aicoach.backend.models;

import com.aicoach.backend.enums.ActivityReviewStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "activity_reviews")
@Data
public class ActivityReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "activity_id", nullable = false, unique = true)
    private Activity activity;

    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "comparison_id", nullable = false, unique = true)
    private ActivityComparison comparison;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ActivityReviewStatus status;

    @Column(name = "policy_version", nullable = false, length = 50)
    private String policyVersion;

    @Column(name = "policy_reason", nullable = false, length = 500)
    private String policyReason;

    @Column(name = "model_called", nullable = false)
    private boolean modelCalled;

    @Column(name = "assessment", length = 1500)
    private String assessment;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "response_id", length = 100)
    private String responseId;

    @Column(name = "input_tokens") private Integer inputTokens;
    @Column(name = "output_tokens") private Integer outputTokens;
    @Column(name = "latency_ms") private Long latencyMs;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "completed_at") private Instant completedAt;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("requestedAt ASC")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private List<ActivitySegmentDetailRequest> detailRequests = new ArrayList<>();
}
