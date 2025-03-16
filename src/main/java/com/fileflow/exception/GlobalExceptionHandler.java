package com.fileflow.exception;

import com.fileflow.dto.response.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        log.error("Resource not found: {}", ex.getMessage());

        ApiResponse apiResponse = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(apiResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse> handleBadRequestException(BadRequestException ex, WebRequest request) {
        log.error("Bad request: {}", ex.getMessage());

        ApiResponse apiResponse = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse> handleUnauthorizedException(UnauthorizedException ex, WebRequest request) {
        log.error("Unauthorized: {}", ex.getMessage());

        ApiResponse apiResponse = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(apiResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.error("Access denied: {}", ex.getMessage());

        ApiResponse apiResponse = ApiResponse.builder()
                .success(false)
                .message("You don't have permission to access this resource")
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(apiResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        log.error("Bad credentials: {}", ex.getMessage());

        ApiResponse apiResponse = ApiResponse.builder()
                .success(false)
                .message("Invalid username or password")
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(apiResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiResponse> handleStorageException(StorageException ex, WebRequest request) {
        log.error("Storage error: {}", ex.getMessage());

        ApiResponse apiResponse = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex, WebRequest request) {
        log.error("File size exceeded: {}", ex.getMessage());

        ApiResponse apiResponse = ApiResponse.builder()
                .success(false)
                .message("File size exceeds the maximum allowed size")
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        log.error("Validation error: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse apiResponse = ApiResponse.builder()
                .success(false)
                .message("Validation failed")
                .timestamp(LocalDateTime.now())
                .data(errors)
                .build();

        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Unhandled exception", ex);

        ApiResponse apiResponse = ApiResponse.builder()
                .success(false)
                .message("An unexpected error occurred")
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}