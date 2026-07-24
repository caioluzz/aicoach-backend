package com.aicoach.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ActivityRecordDTO(
        @JsonProperty("ts")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime ts,

        @JsonProperty("elapsed_s")
        Integer elapsedS,

        @JsonProperty("distance_km")
        BigDecimal distanceKm,

        @JsonProperty("speed_kmh")
        BigDecimal speedKmh,

        @JsonProperty("pace_s_per_km")
        Short paceSPerKm,

        @JsonProperty("heart_rate")
        Short heartRate,

        @JsonProperty("cadence")
        Short cadence,

        @JsonProperty("altitude_m")
        BigDecimal altitudeM
) {}