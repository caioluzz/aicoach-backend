package com.aicoach.backend.review;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class ActivityReviewException extends RuntimeException {
    public ActivityReviewException(String message) { super(message); }
    public ActivityReviewException(String message, Throwable cause) { super(message, cause); }
}
