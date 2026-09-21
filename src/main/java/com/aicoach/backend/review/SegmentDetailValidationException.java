package com.aicoach.backend.review;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class SegmentDetailValidationException extends RuntimeException {
    public SegmentDetailValidationException(String message) { super(message); }
}
