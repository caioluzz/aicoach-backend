package com.aicoach.backend.seasonplan;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class OpenAiSeasonPlanException extends RuntimeException {
    public OpenAiSeasonPlanException(String message) {
        super(message);
    }

    public OpenAiSeasonPlanException(String message, Throwable cause) {
        super(message, cause);
    }
}
