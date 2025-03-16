package com.fileflow.dto.response.health;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HealthCheckResponse {
    private String status;
    private LocalDateTime timestamp;
    private String application;
    private String environment;
    private Map<String, Map<String, Object>> components;
    private Map<String, Object> metrics;
}