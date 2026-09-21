package com.aicoach.backend.review;

import com.aicoach.backend.dto.ActivitySegmentDetailRequest;
import com.aicoach.backend.dto.ActivitySegmentDetails;
import com.aicoach.backend.enums.SegmentQueryType;
import com.aicoach.backend.enums.SegmentResolution;
import com.aicoach.backend.enums.TelemetryField;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.*;

@Component
public class OpenAiActivityCoach implements ActivityCoach {
    private static final String INSTRUCTIONS = """
            Você é o treinador que interpreta uma atividade de corrida já comparada por regras determinísticas.
            Produza uma avaliação curta, prática e em no máximo três frases. Não recalcule o cumprimento.
            O resumo recebido, inclusive o detalhamento por etapa, é a única fonte inicial.
            Use get_activity_segment_details somente quando uma discrepância específica não puder ser explicada pelo resumo.
            Toda chamada deve indicar um motivo concreto, o menor intervalo útil, os campos necessários e pequeno contexto.
            Comece em TEN_SECONDS; só peça FIVE_SECONDS após 10 s sobreposto e RAW após 5 s sobreposto.
            Não peça telemetria por curiosidade, não diagnostique condições médicas e não proponha adaptação da semana seguinte.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final int maxOutputTokens;
    private final int maxRetries;
    private final int maxInspections;

    public OpenAiActivityCoach(ObjectMapper objectMapper,
                               @Value("${openai.api.url:https://api.openai.com/v1}") String apiUrl,
                               @Value("${openai.api.key:}") String apiKey,
                               @Value("${openai.model.activity-review:gpt-5-mini}") String model,
                               @Value("${openai.activity-review.max-output-tokens:2500}") int maxOutputTokens,
                               @Value("${openai.timeout-seconds:60}") int timeoutSeconds,
                               @Value("${openai.max-retries:2}") int maxRetries,
                               @Value("${openai.activity-review.max-inspections:4}") int maxInspections) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 20)));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder().baseUrl(apiUrl).requestFactory(factory).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.maxOutputTokens = maxOutputTokens;
        this.maxRetries = maxRetries;
        this.maxInspections = maxInspections;
    }

    @Override
    public Result review(ActivityReviewContext context, SegmentDetailsTool tool) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ActivityReviewException("OPENAI_API_KEY não foi configurada");
        }
        long startedAt = System.nanoTime();
        int inputTokens = 0;
        int outputTokens = 0;
        String responseId = null;
        List<Object> observations = new ArrayList<>();

        for (int turn = 0; turn <= maxInspections; turn++) {
            JsonNode response = executeWithRetry(body(context, observations));
            responseId = response.path("id").asText(responseId);
            inputTokens += response.path("usage").path("input_tokens").asInt(0);
            outputTokens += response.path("usage").path("output_tokens").asInt(0);
            if (!"completed".equals(response.path("status").asText())) {
                throw new ActivityReviewException("Resposta incompleta da OpenAI para avaliação da atividade");
            }
            JsonNode functionCall = findOutput(response, "function_call");
            if (functionCall != null) {
                if (turn == maxInspections) {
                    throw new ActivityReviewException("O treinador excedeu o limite de inspeções de telemetria");
                }
                ActivitySegmentDetailRequest request = parseRequest(functionCall.path("arguments").asText());
                Map<String, Object> observation = new LinkedHashMap<>();
                observation.put("request", request);
                try {
                    ActivitySegmentDetails details = tool.get(request);
                    observation.put("result", details);
                } catch (SegmentDetailValidationException exception) {
                    observation.put("error", exception.getMessage());
                }
                observations.add(observation);
                continue;
            }
            JsonNode output = findOutputText(response);
            if (output == null) {
                throw new ActivityReviewException("A OpenAI não retornou avaliação nem solicitação de detalhe");
            }
            String assessment = parseAssessment(output.asText());
            if (assessment.length() > 1500) {
                throw new ActivityReviewException("A avaliação excedeu o limite de 1500 caracteres");
            }
            return new Result(assessment, responseId, response.path("model").asText(model),
                    inputTokens, outputTokens, Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
        }
        throw new ActivityReviewException("A avaliação não foi concluída");
    }

    JsonNode executeWithRetry(Map<String, Object> body) {
        RuntimeException last = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return restClient.post().uri("/responses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
            } catch (RestClientResponseException exception) {
                if (exception.getStatusCode().value() != 429 && !exception.getStatusCode().is5xxServerError()) {
                    throw new ActivityReviewException("A OpenAI rejeitou a avaliação (HTTP "
                            + exception.getStatusCode().value() + ")", exception);
                }
                last = exception;
            } catch (ResourceAccessException exception) {
                last = exception;
            }
        }
        throw new ActivityReviewException("A OpenAI permaneceu indisponível após as tentativas configuradas", last);
    }

    private Map<String, Object> body(ActivityReviewContext context, List<Object> observations) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("deterministic_context", context);
        input.put("detail_observations", observations);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("instructions", INSTRUCTIONS);
        body.put("input", write(input));
        body.put("store", false);
        body.put("max_output_tokens", maxOutputTokens);
        body.put("tools", List.of(toolDefinition()));
        body.put("text", Map.of("format", Map.of(
                "type", "json_schema", "name", "activity_review", "strict", true,
                "schema", objectSchema(Map.of("assessment", Map.of("type", "string"))))));
        return body;
    }

    private Map<String, Object> toolDefinition() {
        return Map.of("type", "function", "name", "get_activity_segment_details",
                "description", "Obtém apenas o menor trecho necessário, com motivo auditável e resolução progressiva.",
                "strict", true,
                "parameters", objectSchema(Map.of(
                        "queryType", enumSchema(SegmentQueryType.values()),
                        "start", Map.of("type", "number"),
                        "end", Map.of("type", "number"),
                        "resolution", enumSchema(SegmentResolution.values()),
                        "fields", Map.of("type", "array", "items", enumSchema(TelemetryField.values())),
                        "contextBefore", Map.of("type", "number"),
                        "contextAfter", Map.of("type", "number"),
                        "reason", Map.of("type", "string"))));
    }

    private ActivitySegmentDetailRequest parseRequest(String json) {
        try {
            return objectMapper.readValue(json, ActivitySegmentDetailRequest.class);
        } catch (JacksonException exception) {
            throw new ActivityReviewException("Argumentos inválidos em get_activity_segment_details", exception);
        }
    }

    private String parseAssessment(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            String assessment = node.path("assessment").asText(null);
            if (assessment == null || assessment.isBlank()) {
                throw new ActivityReviewException("A avaliação estruturada veio vazia");
            }
            return assessment.trim();
        } catch (JacksonException exception) {
            throw new ActivityReviewException("A avaliação estruturada não pôde ser lida", exception);
        }
    }

    private JsonNode findOutput(JsonNode response, String type) {
        for (JsonNode item : response.path("output")) {
            if (type.equals(item.path("type").asText())) return item;
        }
        return null;
    }

    private JsonNode findOutputText(JsonNode response) {
        for (JsonNode item : response.path("output")) {
            if (!"message".equals(item.path("type").asText())) continue;
            for (JsonNode content : item.path("content")) {
                if ("refusal".equals(content.path("type").asText())) {
                    throw new ActivityReviewException("A OpenAI recusou a avaliação da atividade");
                }
                if ("output_text".equals(content.path("type").asText())) return content.path("text");
            }
        }
        return null;
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new ActivityReviewException("Falha ao preparar o resumo da atividade", exception);
        }
    }

    private static Map<String, Object> enumSchema(Enum<?>[] values) {
        return Map.of("type", "string", "enum", Arrays.stream(values).map(Enum::name).toList());
    }

    private static Map<String, Object> objectSchema(Map<String, Object> properties) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", new ArrayList<>(properties.keySet()));
        schema.put("additionalProperties", false);
        return schema;
    }
}
