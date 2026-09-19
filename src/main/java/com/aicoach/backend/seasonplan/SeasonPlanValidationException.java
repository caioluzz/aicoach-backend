package com.aicoach.backend.seasonplan;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class SeasonPlanValidationException extends RuntimeException {
    private final List<String> violations;

    public SeasonPlanValidationException(List<String> violations) {
        super("A proposta de plano geral foi rejeitada: " + String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
