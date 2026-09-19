package com.aicoach.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class WeeklyPlanNotFoundException extends RuntimeException {
    public WeeklyPlanNotFoundException(Long id) { super("Plano semanal não encontrado: " + id); }
}
