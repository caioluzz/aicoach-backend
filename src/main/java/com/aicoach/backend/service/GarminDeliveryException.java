package com.aicoach.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class GarminDeliveryException extends RuntimeException {
    public GarminDeliveryException(String message) {
        super(message);
    }
}
