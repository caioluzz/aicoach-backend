package com.aicoach.backend.client;

import com.aicoach.backend.dto.GarminWorkoutContract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.LocalDate;
import java.util.function.Supplier;

@Component
public class GarminWorkoutClient implements GarminWorkoutGateway {
    private final RestClient restClient;
    private final int maxAttempts;
    private final Duration retryDelay;

    public GarminWorkoutClient(
            @Value("${garmin.bot.url}") String botUrl,
            @Value("${garmin.delivery.max-attempts:3}") int maxAttempts,
            @Value("${garmin.delivery.retry-delay-ms:200}") long retryDelayMs,
            @Value("${garmin.delivery.adapter-api-key:}") String adapterApiKey,
            @Value("${garmin.delivery.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${garmin.delivery.read-timeout-ms:30000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        RestClient.Builder builder = RestClient.builder().baseUrl(botUrl).requestFactory(requestFactory);
        if (adapterApiKey != null && !adapterApiKey.isBlank()) {
            builder.defaultHeader("X-Adapter-Key", adapterApiKey);
        }
        this.restClient = builder.build();
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryDelay = Duration.ofMillis(Math.max(0, retryDelayMs));
    }

    @Override
    public Preview preview(GarminWorkoutContract workout) {
        return retry(() -> restClient.post().uri("/api/garmin/workouts/preview")
                .contentType(MediaType.APPLICATION_JSON).body(workout).retrieve().body(Preview.class));
    }

    @Override
    public Delivery deliver(Credentials credentials, String idempotencyKey, GarminWorkoutContract workout) {
        return retry(() -> restClient.post().uri("/api/garmin/workouts/deliver")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new DeliverRequest(credentials, idempotencyKey, workout))
                .retrieve().body(Delivery.class));
    }

    @Override
    public Delivery update(Credentials credentials, Long workoutId, Long scheduledWorkoutId,
                           String idempotencyKey, GarminWorkoutContract workout) {
        return retry(() -> restClient.put().uri("/api/garmin/workouts/{workoutId}", workoutId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateRequest(credentials, idempotencyKey, workout, scheduledWorkoutId))
                .retrieve().body(Delivery.class));
    }

    @Override
    public Confirmation confirm(Credentials credentials, Long workoutId, Long scheduledWorkoutId,
                                LocalDate scheduledDate, String idempotencyKey) {
        return retry(() -> restClient.post().uri("/api/garmin/workouts/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ConfirmRequest(credentials, workoutId, scheduledWorkoutId,
                        scheduledDate, idempotencyKey))
                .retrieve().body(Confirmation.class));
    }

    @Override
    public void cancel(Credentials credentials, Long workoutId, Long scheduledWorkoutId) {
        retry(() -> restClient.post().uri("/api/garmin/workouts/{workoutId}/cancel", workoutId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CancelRequest(credentials, scheduledWorkoutId))
                .retrieve().toBodilessEntity());
    }

    private <T> T retry(Supplier<T> operation) {
        RestClientException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                T result = operation.get();
                if (result == null) throw new RestClientException("Garmin adapter returned an empty response");
                return result;
            } catch (RestClientException exception) {
                if (exception instanceof RestClientResponseException response
                        && response.getStatusCode().is4xxClientError()
                        && response.getStatusCode().value() != 429) {
                    throw exception;
                }
                last = exception;
                if (attempt < maxAttempts && !retryDelay.isZero()) {
                    try {
                        Thread.sleep(retryDelay.multipliedBy(attempt).toMillis());
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw exception;
                    }
                }
            }
        }
        throw last;
    }

    private record DeliverRequest(Credentials credentials, String idempotencyKey,
                                  GarminWorkoutContract workout) {}
    private record UpdateRequest(Credentials credentials, String idempotencyKey,
                                 GarminWorkoutContract workout, Long scheduledWorkoutId) {}
    private record ConfirmRequest(Credentials credentials, Long workoutId, Long scheduledWorkoutId,
                                  LocalDate scheduledDate, String idempotencyKey) {}
    private record CancelRequest(Credentials credentials, Long scheduledWorkoutId) {}
}
