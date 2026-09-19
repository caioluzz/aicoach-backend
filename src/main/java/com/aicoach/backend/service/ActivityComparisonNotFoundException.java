package com.aicoach.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ActivityComparisonNotFoundException extends RuntimeException {
    public ActivityComparisonNotFoundException(Long activityId) {
        super("Comparação não encontrada para a atividade " + activityId);
    }
}
