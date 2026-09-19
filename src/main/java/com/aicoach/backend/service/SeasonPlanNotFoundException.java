package com.aicoach.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SeasonPlanNotFoundException extends RuntimeException {
    public SeasonPlanNotFoundException(Long planId) {
        super("Plano geral não encontrado: " + planId);
    }
}
