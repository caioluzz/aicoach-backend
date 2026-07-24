package com.aicoach.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
        Double averageSpeed,

        @JsonProperty("athlete_id")
        Long athleteId,

        @JsonProperty("sport")
        String sport,

        @JsonProperty("sub_sport")
        String subSport,

        @JsonProperty("is_vdot_test")
        Boolean isVdotTest,

        @JsonProperty("ended_at")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime endedAt,

        @JsonProperty("max_speed_kmh")
        BigDecimal maxSpeedKmh,

        @JsonProperty("avg_pace_s_per_km")
        Short avgPaceSPerKm,

        @JsonProperty("best_pace_s_per_km")
        Short bestPaceSPerKm,

        @JsonProperty("max_hr")
        Short maxHr,

        @JsonProperty("avg_cadence")
        Short avgCadence,

        @JsonProperty("max_cadence")
        Short maxCadence,

        @JsonProperty("elevation_gain_m")
        Short elevationGainM,

        @JsonProperty("elevation_loss_m")
        Short elevationLossM,

        @JsonProperty("min_altitude_m")
        BigDecimal minAltitudeM,

        @JsonProperty("max_altitude_m")
        BigDecimal maxAltitudeM,

        @JsonProperty("lap_count")
        Short lapCount,

        @JsonProperty("record_count")
        Short recordCount,

        @JsonProperty("raw_file_path")
        String rawFilePath,

        @JsonProperty("laps")
        List<LapDTO> laps,

        @JsonProperty("activity_records")
        List<ActivityRecordDTO> records
) {}