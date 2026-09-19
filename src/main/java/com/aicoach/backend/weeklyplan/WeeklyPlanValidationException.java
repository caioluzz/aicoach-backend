package com.aicoach.backend.weeklyplan;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class WeeklyPlanValidationException extends RuntimeException {
    private final List<String> violations;

    public WeeklyPlanValidationException(List<String> violations) {
        super("Plano semanal inválido: " + String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> getViolations() { return violations; }
}
