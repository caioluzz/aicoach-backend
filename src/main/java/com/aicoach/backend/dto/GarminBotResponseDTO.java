package com.aicoach.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;


public record GarminBotResponseDTO(
        @JsonProperty("activity_id")
        Long activityId,

        @JsonProperty("activity_name")
        String activityName,

        @JsonProperty("distance_meters")
        Double distanceMeters,

        @JsonProperty("duration_seconds")
        Double durationSeconds,

        @JsonProperty("started_at")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime startedAt,

        @JsonProperty("average_heart_rate")
        Integer averageHeartRate,

        @JsonProperty("average_speed")
        Double averageSpeed
) {}