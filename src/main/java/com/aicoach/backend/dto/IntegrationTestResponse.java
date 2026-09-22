package com.aicoach.backend.dto;

import java.time.Instant;

public record IntegrationTestResponse(boolean success, String message, Instant validatedAt) {
}
