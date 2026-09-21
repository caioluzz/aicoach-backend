package com.aicoach.backend.models;

import com.aicoach.backend.enums.SegmentQueryType;
import com.aicoach.backend.enums.SegmentResolution;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "activity_segment_detail_requests")
@Data
public class ActivitySegmentDetailRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "review_id", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private ActivityReview review;

    @Enumerated(EnumType.STRING) @Column(name = "query_type", nullable = false, length = 20)
    private SegmentQueryType queryType;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private SegmentResolution resolution;

    @Column(name = "range_start", nullable = false, precision = 12, scale = 3)
    private BigDecimal rangeStart;

    @Column(name = "range_end", nullable = false, precision = 12, scale = 3)
    private BigDecimal rangeEnd;

    @Column(name = "context_before", nullable = false, precision = 10, scale = 3)
    private BigDecimal contextBefore;

    @Column(name = "context_after", nullable = false, precision = 10, scale = 3)
    private BigDecimal contextAfter;

    @Column(nullable = false, length = 200)
    private String fields;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "points_returned", nullable = false)
    private Integer pointsReturned;
}
