package com.aicoach.backend.client;

import com.aicoach.backend.dto.GarminBotRequestDTO;
import com.aicoach.backend.dto.GarminBotResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class GarminBotClient {
    private final RestClient restClient;

    public GarminBotClient(@Value("${garmin.bot.url}") String botUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(botUrl)
                .build();
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
}


