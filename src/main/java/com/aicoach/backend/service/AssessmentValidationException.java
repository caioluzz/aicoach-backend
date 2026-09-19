package com.aicoach.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AssessmentValidationException extends RuntimeException {
    public AssessmentValidationException(String message) {
        super(message);
    }
}
