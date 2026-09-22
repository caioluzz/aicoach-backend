package com.aicoach.backend.weeklyplan;

import com.aicoach.backend.config.OpenAiRuntimeSettingsProvider;
import com.aicoach.backend.enums.DurationType;
import com.aicoach.backend.enums.WorkoutType;
import com.aicoach.backend.training.daniels.DanielsIntensity;
import com.aicoach.backend.training.workout.WorkoutStepDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
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
public class OpenAiWeeklyPlanGenerator implements WeeklyPlanGenerator {
    private static final String INSTRUCTIONS = """
            Você detalha exatamente uma semana de corrida de um plano geral já aprovado.
            Produza somente sessões de corrida nos dias disponíveis, com blocos e passos executáveis.
            Respeite volume, fase, foco, saúde, recuperação, disponibilidade e adaptação recebidos.
            Quando houver adaptação, reduza targetVolumeKm pelo loadReductionPercent informado
            e não prescreva T/I/R se allowIntensity for falso.
            Use intensidades Daniels E, M, T, I e R, sem inventar ritmos: o backend aplicará o perfil.
            Sessões de qualidade devem começar com WARMUP em E e terminar com COOLDOWN em E.
            Se a prova estiver na semana, inclua uma sessão RACE na data e distância exatas do objetivo.
            Não inclua envio ao Garmin, adaptação pós-treino, diagnóstico ou recomendação médica.
            Não represente dias de descanso como sessões.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final int maxRetries;
    private final OpenAiRuntimeSettingsProvider settingsProvider;
    private final ThreadLocal<String> activeApiKey = new ThreadLocal<>();

    public OpenAiWeeklyPlanGenerator(ObjectMapper objectMapper,
                                     @Value("${openai.api.url:https://api.openai.com/v1}") String apiUrl,
                                     @Value("${openai.api.key:}") String apiKey,
                                     @Value("${openai.model.weekly-planner:gpt-5-mini}") String model,
                                     @Value("${openai.weekly.max-output-tokens:12000}") int maxOutputTokens,
                                     @Value("${openai.timeout-seconds:60}") int timeoutSeconds,
                                     @Value("${openai.max-retries:2}") int maxRetries) {
        this(objectMapper, apiUrl, apiKey, model, maxOutputTokens, timeoutSeconds, maxRetries,
                athleteId -> new OpenAiRuntimeSettingsProvider.Settings(apiKey, model, model, model,
                        maxOutputTokens, maxOutputTokens, maxOutputTokens));
    }

    @Autowired
    public OpenAiWeeklyPlanGenerator(ObjectMapper objectMapper,
                                     @Value("${openai.api.url:https://api.openai.com/v1}") String apiUrl,
                                     @Value("${openai.api.key:}") String apiKey,
                                     @Value("${openai.model.weekly-planner:gpt-5-mini}") String model,
                                     @Value("${openai.weekly.max-output-tokens:12000}") int maxOutputTokens,
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
    public WeeklyPlanGenerationResult generate(WeeklyPlanGenerationContext context) {
        OpenAiRuntimeSettingsProvider.Settings settings = settingsProvider.resolve(context.athleteId());
        if (settings.apiKey() == null || settings.apiKey().isBlank()) {
            throw new OpenAiWeeklyPlanException("OPENAI_API_KEY não foi configurada");
        }
        String selectedModel = settings.weeklyPlannerModel();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", selectedModel);
        body.put("instructions", INSTRUCTIONS);
        body.put("input", serializeContext(context));
        body.put("store", false);
        body.put("max_output_tokens", settings.weeklyMaxOutputTokens());
        body.put("text", Map.of("format", Map.of(
                "type", "json_schema", "name", "weekly_plan",
                "description", "Microciclo semanal detalhado de corrida", "strict", true,
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
                        .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
            } catch (RestClientResponseException exception) {
                if (exception.getStatusCode().value() != 429 && !exception.getStatusCode().is5xxServerError()) {
                    throw new OpenAiWeeklyPlanException("A OpenAI rejeitou o plano semanal (HTTP "
                            + exception.getStatusCode().value() + ")", exception);
                }
                lastFailure = exception;
            } catch (ResourceAccessException exception) {
                lastFailure = exception;
            }
        }
        throw new OpenAiWeeklyPlanException("A OpenAI permaneceu indisponível apó as tentativas configuradas",
                lastFailure);
    }

    private WeeklyPlanGenerationResult parseResponse(JsonNode response, long latencyMs, String selectedModel) {
        if (response == null || !"completed".equals(response.path("status").asText())) {
            String reason = response == null ? "resposta vazia"
                    : response.path("incomplete_details").path("reason").asText("status não concluído");
            throw new OpenAiWeeklyPlanException("Resposta da OpenAI incompleta: " + reason);
        }
        String outputText = null;
        for (JsonNode item : response.path("output")) {
            if (!"message".equals(item.path("type").asText())) continue;
            for (JsonNode content : item.path("content")) {
                if ("refusal".equals(content.path("type").asText())) {
                    throw new OpenAiWeeklyPlanException("A OpenAI recusou a geração do plano semanal");
                }
                if ("output_text".equals(content.path("type").asText())) {
                    outputText = content.path("text").asText(null);
                }
            }
        }
        if (outputText == null || outputText.isBlank()) {
            throw new OpenAiWeeklyPlanException("A OpenAI não retornou o plano semanal estruturado");
        }
        try {
            WeeklyPlanProposal proposal = objectMapper.readValue(outputText, WeeklyPlanProposal.class);
            JsonNode usage = response.path("usage");
            return new WeeklyPlanGenerationResult(proposal, response.path("id").asText(null),
                    response.path("model").asText(selectedModel), nullableInt(usage, "input_tokens"),
                    nullableInt(usage, "output_tokens"), latencyMs);
        } catch (JacksonException exception) {
            throw new OpenAiWeeklyPlanException("A saída semanal não pôde ser desserializada", exception);
        }
    }

    private Integer nullableInt(JsonNode node, String field) {
        return node.has(field) && node.get(field).isNumber() ? node.get(field).intValue() : null;
    }

    private String serializeContext(WeeklyPlanGenerationContext context) {
        try {
            return "Gere o plano semanal para este contexto JSON:\n" + objectMapper.writeValueAsString(context);
        } catch (JacksonException exception) {
            throw new OpenAiWeeklyPlanException("Falha ao preparar o contexto semanal", exception);
        }
    }

    static Map<String, Object> schema() {
        Map<String, Object> step = objectSchema(Map.of(
                "kind", enumSchema(WorkoutStepDefinition.Kind.values()),
                "durationType", enumSchema(DurationType.values()),
                "durationValue", Map.of("type", "integer"),
                "intensity", enumSchema(DanielsIntensity.values()),
                "instruction", Map.of("type", "string")));
        Map<String, Object> block = objectSchema(Map.of(
                "repetitions", Map.of("type", "integer"),
                "steps", Map.of("type", "array", "items", step)));
        List<String> workoutTypes = Arrays.stream(WorkoutType.values())
                .filter(value -> value != WorkoutType.REST).map(Enum::name).toList();
        Map<String, Object> session = objectSchema(Map.of(
                "order", Map.of("type", "integer"),
                "name", Map.of("type", "string"),
                "scheduledDate", Map.of("type", "string"),
                "workoutType", Map.of("type", "string", "enum", workoutTypes),
                "blocks", Map.of("type", "array", "items", block)));
        return objectSchema(Map.of(
                "summary", Map.of("type", "string"),
                "sessions", Map.of("type", "array", "items", session)));
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
