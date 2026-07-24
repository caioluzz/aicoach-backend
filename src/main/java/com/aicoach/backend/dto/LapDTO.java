package com.aicoach.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LapDTO(
        @JsonProperty("lap_number")
        Short lapNumber,

        @JsonProperty("start_time")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime startTime,

        @JsonProperty("duration_s")
        BigDecimal durationS, // Mudou de Integer para BigDecimal para manter precisão de decimais

        @JsonProperty("distance_km")
        BigDecimal distanceKm,

        @JsonProperty("avg_pace_s_per_km")
        Short avgPaceSPerKm,

        @JsonProperty("avg_speed_kmh")
        BigDecimal avgSpeedKmh,

        @JsonProperty("avg_hr")
        Short avgHr,

        @JsonProperty("max_hr")
        Short maxHr,

        @JsonProperty("avg_cadence")
        Short avgCadence,

        @JsonProperty("max_cadence")
        Short maxCadence,

        @JsonProperty("ascent_m")
        Short ascentM,

        @JsonProperty("descent_m")
        Short descentM
) {}