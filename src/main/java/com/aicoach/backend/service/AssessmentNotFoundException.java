package com.aicoach.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AssessmentNotFoundException extends RuntimeException {
    public AssessmentNotFoundException(Long athleteId) {
        super("Nenhuma anamnese encontrada para o atleta: " + athleteId);
    }
}
