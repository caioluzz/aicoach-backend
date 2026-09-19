package com.aicoach.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class SeasonPlanStateException extends RuntimeException {
    public SeasonPlanStateException(String message) {
        super(message);
    }
}
