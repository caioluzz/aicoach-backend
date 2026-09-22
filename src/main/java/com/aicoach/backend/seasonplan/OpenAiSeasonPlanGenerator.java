package com.aicoach.backend.seasonplan;

import com.aicoach.backend.enums.TrainingPhase;
import com.aicoach.backend.config.OpenAiRuntimeSettingsProvider;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.*;

@Component
public class OpenAiSeasonPlanGenerator implements SeasonPlanGenerator {
    private static final String INSTRUCTIONS = """
            Você cria somente o plano estratégico de uma temporada de corrida até a prova-alvo.
            Respeite exatamente as datas e a quantidade de semanas recebidas. Não prescreva treinos diários,
            sessões, blocos ou repetições. Produza fases, foco semanal, trajetória conservadora de volume,
            semanas de recuperação, taper de uma a três semanas e critérios explícitos de revisão.
            Use somente os dados fornecidos; não faça diagnóstico médico. A semana da prova deve ser taper.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final int maxRetries;
    private final OpenAiRuntimeSettingsProvider settingsProvider;
    private final ThreadLocal<String> activeApiKey = new ThreadLocal<>();

    public OpenAiSeasonPlanGenerator(ObjectMapper objectMapper,
                                     @Value("${openai.api.url:https://api.openai.com/v1}") String apiUrl,
                                     @Value("${openai.api.key:}") String apiKey,
                                     @Value("${openai.model.planner:gpt-5-mini}") String model,
                                     @Value("${openai.max-output-tokens:8000}") int maxOutputTokens,
                                     @Value("${openai.timeout-seconds:60}") int timeoutSeconds,
                                     @Value("${openai.max-retries:2}") int maxRetries) {
        this(objectMapper, apiUrl, apiKey, model, maxOutputTokens, timeoutSeconds, maxRetries,
                athleteId -> new OpenAiRuntimeSettingsProvider.Settings(apiKey, model, model, model,
                        maxOutputTokens, maxOutputTokens, maxOutputTokens));
    }

    @Autowired
    public OpenAiSeasonPlanGenerator(ObjectMapper objectMapper,
                                     @Value("${openai.api.url:https://api.openai.com/v1}") String apiUrl,
                                     @Value("${openai.api.key:}") String apiKey,
                                     @Value("${openai.model.planner:gpt-5-mini}") String model,
                                     @Value("${openai.max-output-tokens:8000}") int maxOutputTokens,
                                     @Value("${openai.timeout-seconds:60}") int timeoutSeconds,
                                     @Value("${openai.max-retries:2}") int maxRetries,
                                     OpenAiRuntimeSettingsProvider settingsProvider) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 20)));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder().baseUrl(apiUrl).requestFactory(requestFactory).build();
        this.objectMapper = objectMapper;
        this.maxRetries = maxRetries;
        this.settingsProvider = settingsProvider;
    }

    @Override
    public SeasonPlanGenerationResult generate(SeasonPlanGenerationContext context) {
        OpenAiRuntimeSettingsProvider.Settings settings = settingsProvider.resolve(context.athleteId());
        if (settings.apiKey() == null || settings.apiKey().isBlank()) {
            throw new OpenAiSeasonPlanException("OPENAI_API_KEY não foi configurada");
        }
        String selectedModel = settings.plannerModel();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", selectedModel);
        body.put("instructions", INSTRUCTIONS);
        body.put("input", serializeContext(context));
        body.put("store", false);
        body.put("max_output_tokens", settings.maxOutputTokens());
        body.put("text", Map.of("format", Map.of(
                "type", "json_schema",
                "name", "season_plan",
                "description", "Plano estratégico de corrida até a prova-alvo",
                "strict", true,
                "schema", schema())));

        long startedAt = System.nanoTime();
        activeApiKey.set(settings.apiKey());
        try {
            JsonNode response = executeWithRetry(body);
            long latencyMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            return parseResponse(response, latencyMs, selectedModel);
        } finally {
            activeApiKey.remove();
        }
    }

    JsonNode executeWithRetry(Map<String, Object> body) {
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return restClient.post().uri("/responses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + activeApiKey.get())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class);
            } catch (RestClientResponseException exception) {
                if (exception.getStatusCode().value() != 429 && !exception.getStatusCode().is5xxServerError()) {
                    throw new OpenAiSeasonPlanException("A OpenAI rejeitou a solicitação (HTTP "
                            + exception.getStatusCode().value() + ")", exception);
                }
                lastFailure = exception;
            } catch (ResourceAccessException exception) {
                lastFailure = exception;
            }
        }
        throw new OpenAiSeasonPlanException("A OpenAI permaneceu indisponível apó as tentativas configuradas",
                lastFailure);
    }

    private SeasonPlanGenerationResult parseResponse(JsonNode response, long latencyMs, String selectedModel) {
        if (response == null || !"completed".equals(response.path("status").asText())) {
            String reason = response == null ? "resposta vazia"
                    : response.path("incomplete_details").path("reason").asText("status não concluído");
            throw new OpenAiSeasonPlanException("Resposta da OpenAI incompleta: " + reason);
        }
        String outputText = null;
        for (JsonNode item : response.path("output")) {
            if (!"message".equals(item.path("type").asText())) continue;
            for (JsonNode content : item.path("content")) {
                if ("refusal".equals(content.path("type").asText())) {
                    throw new OpenAiSeasonPlanException("A OpenAI recusou a geração do plano");
                }
                if ("output_text".equals(content.path("type").asText())) {
                    outputText = content.path("text").asText(null);
                }
            }
        }
        if (outputText == null || outputText.isBlank()) {
            throw new OpenAiSeasonPlanException("A OpenAI não retornou texto estruturado");
        }
        try {
            SeasonPlanProposal proposal = objectMapper.readValue(outputText, SeasonPlanProposal.class);
            JsonNode usage = response.path("usage");
            return new SeasonPlanGenerationResult(proposal, response.path("id").asText(null),
                    response.path("model").asText(selectedModel), nullableInt(usage, "input_tokens"),
                    nullableInt(usage, "output_tokens"), latencyMs);
        } catch (JacksonException exception) {
            throw new OpenAiSeasonPlanException("A saída estruturada não pôde ser desserializada", exception);
        }
    }

    private Integer nullableInt(JsonNode node, String field) {
        return node.has(field) && node.get(field).isNumber() ? node.get(field).intValue() : null;
    }

    private String serializeContext(SeasonPlanGenerationContext context) {
        try {
            return "Gere o plano geral para este contexto JSON:\n" + objectMapper.writeValueAsString(context);
        } catch (JacksonException exception) {
            throw new OpenAiSeasonPlanException("Falha ao preparar contexto do plano", exception);
        }
    }

    static Map<String, Object> schema() {
        Map<String, Object> phase = objectSchema(Map.of(
                "order", Map.of("type", "integer"),
                "phase", Map.of("type", "string", "enum", Arrays.stream(TrainingPhase.values()).map(Enum::name).toList()),
                "startWeek", Map.of("type", "integer"),
                "endWeek", Map.of("type", "integer"),
                "objective", Map.of("type", "string"),
                "expectedProgression", Map.of("type", "string")));
        Map<String, Object> week = objectSchema(Map.of(
                "weekNumber", Map.of("type", "integer"),
                "startDate", Map.of("type", "string"),
                "endDate", Map.of("type", "string"),
                "targetVolumeKm", Map.of("type", "number"),
                "focus", Map.of("type", "string"),
                "recoveryWeek", Map.of("type", "boolean"),
                "taperWeek", Map.of("type", "boolean")));
        Map<String, Object> criterion = objectSchema(Map.of(
                "code", Map.of("type", "string", "enum", SeasonPlanValidator.REQUIRED_REVISION_CRITERIA.stream().sorted().toList()),
                "description", Map.of("type", "string")));
        return objectSchema(Map.of(
                "summary", Map.of("type", "string"),
                "phases", Map.of("type", "array", "items", phase),
                "weeks", Map.of("type", "array", "items", week),
                "revisionCriteria", Map.of("type", "array", "items", criterion)));
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
