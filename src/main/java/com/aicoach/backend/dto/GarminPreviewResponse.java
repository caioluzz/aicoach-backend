package com.aicoach.backend.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record GarminPreviewResponse(Long weeklyPlanId, Integer planVersion, List<Item> sessions) {
    public record Item(Long plannedActivityId, Integer sessionOrder, String name,
                       LocalDate scheduledDate, String contentHash, Map<String, Object> payload) {}
}
