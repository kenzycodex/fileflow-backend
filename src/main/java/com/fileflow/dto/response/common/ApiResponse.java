package com.fileflow.dto.response.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard API response format for all endpoints
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {

    /**
     * Whether the request was successful
     */
    private boolean success;

    /**
     * Response message
     */
    private String message;

    /**
     * Response data, if any
     */
    private Object data;

    /**
     * Validation errors or other error details
     */
    private Map<String, String> errors;

    /**
     * HTTP status code
     */
    private Integer status;

    /**
     * Request path
     */
    private String path;

    /**
     * Response timestamp
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * Trace ID for debugging
     */
    private String traceId;
}