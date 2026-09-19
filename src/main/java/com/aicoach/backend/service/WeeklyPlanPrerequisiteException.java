package com.aicoach.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class WeeklyPlanPrerequisiteException extends RuntimeException {
    public WeeklyPlanPrerequisiteException(String message) { super(message); }
}
