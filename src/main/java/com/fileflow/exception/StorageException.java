package com.fileflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

/**
 * Exception thrown when there's a problem with storage operations
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class StorageException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}