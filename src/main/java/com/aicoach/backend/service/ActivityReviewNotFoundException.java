package com.aicoach.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ActivityReviewNotFoundException extends RuntimeException {
    public ActivityReviewNotFoundException(Long activityId) {
        super("Avaliação não encontrada para a atividade " + activityId);
    }
}
