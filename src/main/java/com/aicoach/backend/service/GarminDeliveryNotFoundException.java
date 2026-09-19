package com.aicoach.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class GarminDeliveryNotFoundException extends RuntimeException {
    public GarminDeliveryNotFoundException(Long id) {
        super("Entrega Garmin não encontrada: " + id);
    }
}
