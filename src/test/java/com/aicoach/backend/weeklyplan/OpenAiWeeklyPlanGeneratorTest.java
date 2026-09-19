package com.aicoach.backend.weeklyplan;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiWeeklyPlanGeneratorTest {
    private final ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void sendsStrictResponsesContractAndParsesStructuredPlan() throws Exception {
        var context = WeeklyPlanFixtures.context(LocalDate.of(2026, 1, 5));
        String proposal = mapper.writeValueAsString(WeeklyPlanFixtures.validProposal(context));
        JsonNode response = mapper.valueToTree(Map.of(
                "id", "resp_week", "status", "completed", "model", "gpt-test",
                "output", List.of(Map.of("type", "message", "content", List.of(
                        Map.of("type", "output_text", "text", proposal)))),
                "usage", Map.of("input_tokens", 90, "output_tokens", 120)));
        StubGenerator generator = new StubGenerator(mapper, response);

        WeeklyPlanGenerationResult result = generator.generate(context);

        assertEquals("resp_week", result.responseId());
        assertEquals(2, result.proposal().sessions().size());
        JsonNode request = mapper.valueToTree(generator.request);
        assertFalse(request.path("store").asBoolean());
        assertEquals("json_schema", request.path("text").path("format").path("type").asText());
        assertTrue(request.path("text").path("format").path("strict").asBoolean());
        assertFalse(request.path("text").path("format").path("schema")
                .path("additionalProperties").asBoolean());
    }

    @Test
    void rejectsRefusal() {
        JsonNode response = mapper.valueToTree(Map.of(
                "status", "completed", "model", "gpt-test",
                "output", List.of(Map.of("type", "message", "content", List.of(
                        Map.of("type", "refusal", "refusal", "cannot comply"))))));
        assertThrows(OpenAiWeeklyPlanException.class, () -> new StubGenerator(mapper, response)
                .generate(WeeklyPlanFixtures.context(LocalDate.of(2026, 1, 5))));
    }

    private static class StubGenerator extends OpenAiWeeklyPlanGenerator {
        private final JsonNode response;
        private Map<String, Object> request;

        StubGenerator(ObjectMapper mapper, JsonNode response) {
            super(mapper, "http://unused.invalid", "test-key", "gpt-test", 4000, 5, 0);
            this.response = response;
        }

        @Override
        JsonNode executeWithRetry(Map<String, Object> body) {
            request = body;
            return response;
        }
    }
}
