package com.aicoach.backend.client;

public class GarminAdapterException extends RuntimeException {
    private final int statusCode;
    private final boolean retryable;

    public GarminAdapterException(int statusCode, String message, boolean retryable) {
        super(message);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
