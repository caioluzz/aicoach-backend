package com.aicoach.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class WeeklyPlanStateException extends RuntimeException {
    public WeeklyPlanStateException(String message) {
        super(message);
    }
}
