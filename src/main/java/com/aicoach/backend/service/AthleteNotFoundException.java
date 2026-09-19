package com.aicoach.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AthleteNotFoundException extends RuntimeException {
    public AthleteNotFoundException(Long athleteId) {
        super("Atleta não encontrado: " + athleteId);
    }
}
