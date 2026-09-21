package com.aicoach.backend.dto;

import com.aicoach.backend.enums.SegmentQueryType;
import com.aicoach.backend.enums.SegmentResolution;
import com.aicoach.backend.enums.TelemetryField;

import java.math.BigDecimal;
import java.util.List;

public record ActivitySegmentDetailRequest(
        SegmentQueryType queryType,
        BigDecimal start,
        BigDecimal end,
        SegmentResolution resolution,
        List<TelemetryField> fields,
        BigDecimal contextBefore,
        BigDecimal contextAfter,
        String reason) {
}
