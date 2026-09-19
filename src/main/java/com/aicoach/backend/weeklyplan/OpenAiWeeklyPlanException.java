package com.aicoach.backend.weeklyplan;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class OpenAiWeeklyPlanException extends RuntimeException {
    public OpenAiWeeklyPlanException(String message) { super(message); }
    public OpenAiWeeklyPlanException(String message, Throwable cause) { super(message, cause); }
}
