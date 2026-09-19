package com.aicoach.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class SeasonPlanPrerequisiteException extends RuntimeException {
    public SeasonPlanPrerequisiteException(String message) {
        super(message);
    }
}
