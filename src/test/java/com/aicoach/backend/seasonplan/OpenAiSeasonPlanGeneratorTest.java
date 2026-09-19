package com.aicoach.backend.seasonplan;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiSeasonPlanGeneratorTest {
    private final ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void sendsResponsesApiStructuredOutputContractAndParsesUsage() throws Exception {
        var context = SeasonPlanFixtures.context(LocalDate.of(2026, 1, 1), 8);
        String proposalJson = mapper.writeValueAsString(SeasonPlanFixtures.validProposal(context));
        JsonNode response = mapper.valueToTree(Map.of(
                "id", "resp_123", "status", "completed", "model", "gpt-test",
                "output", List.of(Map.of("type", "message", "content", List.of(
                        Map.of("type", "output_text", "text", proposalJson)))),
                "usage", Map.of("input_tokens", 321, "output_tokens", 654)));
        StubGenerator generator = new StubGenerator(mapper, response);

        SeasonPlanGenerationResult result = generator.generate(context);

        assertEquals("resp_123", result.responseId());
        assertEquals(321, result.inputTokens());
        assertEquals(8, result.proposal().weeks().size());
        assertEquals("gpt-test", generator.request.get("model"));
        assertEquals(false, generator.request.get("store"));
        JsonNode request = mapper.valueToTree(generator.request);
        assertEquals("json_schema", request.path("text").path("format").path("type").asText());
        assertTrue(request.path("text").path("format").path("strict").asBoolean());
        assertFalse(request.path("text").path("format").path("schema").path("additionalProperties").asBoolean());
    }

    @Test
    void rejectsExplicitModelRefusal() {
        JsonNode response = mapper.valueToTree(Map.of(
                "id", "resp_refusal", "status", "completed", "model", "gpt-test",
                "output", List.of(Map.of("type", "message", "content", List.of(
                        Map.of("type", "refusal", "refusal", "cannot comply")))),
                "usage", Map.of("input_tokens", 10, "output_tokens", 2)));

        assertThrows(OpenAiSeasonPlanException.class, () -> new StubGenerator(mapper, response)
                .generate(SeasonPlanFixtures.context(LocalDate.of(2026, 1, 1), 8)));
    }

    private static class StubGenerator extends OpenAiSeasonPlanGenerator {
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
