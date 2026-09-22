package com.aicoach.backend.client;

import com.aicoach.backend.dto.GarminActivityDiscoveryRequest;
import com.aicoach.backend.dto.GarminActivityDownloadRequest;
import com.aicoach.backend.dto.GarminActivityMetadata;
import com.aicoach.backend.dto.GarminBotRequestDTO;
import com.aicoach.backend.dto.GarminBotResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GarminBotClient {
    private static final Pattern DETAIL_PATTERN = Pattern.compile(
            "\\\"detail\\\"\\s*:\\s*\\\"([^\\\"]{1,500})\\\"");
    private final RestClient restClient;

    public GarminBotClient(
            @Value("${garmin.bot.url}") String botUrl,
            @Value("${garmin.delivery.adapter-api-key:}") String adapterApiKey,
            @Value("${garmin.sync.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${garmin.sync.read-timeout-ms:60000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(botUrl)
                .requestFactory(requestFactory);
        if (adapterApiKey != null && !adapterApiKey.isBlank()) {
            builder.defaultHeader("X-Adapter-Key", adapterApiKey);
        }
        this.restClient = builder.build();
    }

    public List<GarminBotResponseDTO> fetchActivities(String email, String password, int limit) {
        GarminBotRequestDTO requestPayload = new GarminBotRequestDTO(email, password, limit);

        return invoke(() -> restClient.post()
                .uri("/api/garmin/activities")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestPayload)
                .retrieve()
                .body(new ParameterizedTypeReference<List<GarminBotResponseDTO>>() {}));
    }

    public List<GarminActivityMetadata> discoverActivities(
            String email,
            String password,
            int limit,
            LocalDateTime since) {
        GarminActivityDiscoveryRequest request =
                new GarminActivityDiscoveryRequest(email, password, limit, since);
        return invoke(() -> restClient.post()
                .uri("/api/garmin/activities/discover")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<List<GarminActivityMetadata>>() {}));
    }

    public GarminBotResponseDTO downloadActivity(
            String email,
            String password,
            Long activityId) {
        return invoke(() -> restClient.post()
                .uri("/api/garmin/activities/{activityId}/download", activityId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new GarminActivityDownloadRequest(email, password))
                .retrieve()
                .body(GarminBotResponseDTO.class));
    }

    private <T> T invoke(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            String detail = adapterDetail(exception.getResponseBodyAsString());
            boolean retryable = status >= 500;
            throw new GarminAdapterException(status, detail, retryable);
        } catch (ResourceAccessException exception) {
            throw new GarminAdapterException(503,
                    "O adaptador Garmin local está indisponível.", true);
        } catch (RestClientException exception) {
            throw new GarminAdapterException(502,
                    "O adaptador Garmin devolveu uma resposta incompatível.", false);
        }
    }

    private String adapterDetail(String body) {
        if (body != null) {
            Matcher matcher = DETAIL_PATTERN.matcher(body);
            if (matcher.find() && !matcher.group(1).isBlank()) {
                return matcher.group(1)
                        .replace("\\n", " ")
                        .replace("\\r", " ")
                        .replace("\\t", " ");
            }
        }
        return "O adaptador Garmin não conseguiu concluir a operação.";
    }
}


