package com.aicoach.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record GarminActivityDiscoveryRequest(
        String email,
        String password,
        int limit,
        @JsonProperty("since") LocalDateTime since
) {}
