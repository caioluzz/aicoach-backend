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

import java.util.List;
import java.time.LocalDateTime;

@Component
public class GarminBotClient {
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

        return restClient.post()
                .uri("/api/garmin/activities")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestPayload)
                .retrieve()
                .body(new ParameterizedTypeReference<List<GarminBotResponseDTO>>() {});
    }

    public List<GarminActivityMetadata> discoverActivities(
            String email,
            String password,
            int limit,
            LocalDateTime since) {
        GarminActivityDiscoveryRequest request =
                new GarminActivityDiscoveryRequest(email, password, limit, since);
        return restClient.post()
                .uri("/api/garmin/activities/discover")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<List<GarminActivityMetadata>>() {});
    }

    public GarminBotResponseDTO downloadActivity(
            String email,
            String password,
            Long activityId) {
        return restClient.post()
                .uri("/api/garmin/activities/{activityId}/download", activityId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new GarminActivityDownloadRequest(email, password))
                .retrieve()
                .body(GarminBotResponseDTO.class);
    }
}


