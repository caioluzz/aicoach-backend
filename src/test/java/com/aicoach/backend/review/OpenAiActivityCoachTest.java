package com.aicoach.backend.review;

import com.aicoach.backend.dto.ActivitySegmentDetails;
import com.aicoach.backend.enums.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiActivityCoachTest {
    private final ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void exposesInternalToolAndReturnsShortStructuredAssessment() throws Exception {
        String arguments = mapper.writeValueAsString(Map.of(
                "queryType", "TIME", "start", 600, "end", 720,
                "resolution", "TEN_SECONDS", "fields", List.of("PACE", "HEART_RATE"),
                "contextBefore", 30, "contextAfter", 30,
                "reason", "Investigar desaceleração na segunda repetição"));
        JsonNode toolCall = mapper.valueToTree(Map.of(
                "id", "resp_1", "status", "completed", "model", "gpt-test",
                "output", List.of(Map.of("type", "function_call", "name", "get_activity_segment_details",
                        "call_id", "call_1", "arguments", arguments)),
                "usage", Map.of("input_tokens", 30, "output_tokens", 10)));
        JsonNode finalResponse = mapper.valueToTree(Map.of(
                "id", "resp_2", "status", "completed", "model", "gpt-test",
                "output", List.of(Map.of("type", "message", "content", List.of(
                        Map.of("type", "output_text", "text", "{\"assessment\":\"Ritmo caiu na segunda repetição, com FC estável.\"}")))),
                "usage", Map.of("input_tokens", 40, "output_tokens", 20)));
        StubCoach coach = new StubCoach(mapper, toolCall, finalResponse);
        AtomicReference<com.aicoach.backend.dto.ActivitySegmentDetailRequest> requested = new AtomicReference<>();

        ActivityCoach.Result result = coach.review(context(), request -> {
            requested.set(request);
            return new ActivitySegmentDetails(44L, request.queryType(), request.start(), request.end(),
                    request.start(), request.end(), request.resolution(), request.fields(), request.reason(),
                    List.of(), List.of());
        });

        assertEquals(SegmentResolution.TEN_SECONDS, requested.get().resolution());
        assertEquals("Ritmo caiu na segunda repetição, com FC estável.", result.assessment());
        assertEquals(70, result.inputTokens());
        JsonNode firstBody = mapper.valueToTree(coach.requests.get(0));
        assertEquals("get_activity_segment_details", firstBody.path("tools").get(0).path("name").asText());
        assertTrue(firstBody.path("tools").get(0).path("strict").asBoolean());
        assertFalse(firstBody.path("store").asBoolean());
    }

    private ActivityReviewContext context() {
        return new ActivityReviewContext(7L, 9L, 44L, LocalDateTime.of(2026, 9, 20, 7, 0),
                "Intervalado", "running", 2400.0, 6000.0, 160, (short) 88, null);
    }

    private static class StubCoach extends OpenAiActivityCoach {
        private final Queue<JsonNode> responses;
        private final List<Map<String, Object>> requests = new java.util.ArrayList<>();

        StubCoach(ObjectMapper mapper, JsonNode... responses) {
            super(mapper, "http://unused.invalid", "test-key", "gpt-test", 1000, 5, 0, 4);
            this.responses = new ArrayDeque<>(List.of(responses));
        }

        @Override
        JsonNode executeWithRetry(Map<String, Object> body) {
            requests.add(body);
            return responses.remove();
        }
    }
}
