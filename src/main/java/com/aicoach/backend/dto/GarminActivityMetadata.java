package com.aicoach.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record GarminActivityMetadata(
        @JsonProperty("activity_id") Long activityId,
        @JsonProperty("activity_name") String activityName,
        @JsonProperty("started_at") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startedAt,
        @JsonProperty("is_vdot_test") Boolean isVdotTest
) {}
