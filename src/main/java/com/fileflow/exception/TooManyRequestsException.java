package com.fileflow.exception;

import lombok.Getter;

/**
 * Exception for rate limiting
 */
@Getter
public class TooManyRequestsException extends RuntimeException {

    private final Long retryAfterSeconds;

    public TooManyRequestsException(String message, Long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}