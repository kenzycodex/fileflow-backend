package com.fileflow.controller;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.repository.UserRepository;
import com.fileflow.service.security.RateLimiterService;
import com.fileflow.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for validating usernames and emails
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Validation", description = "API endpoints for validating user information")
@Slf4j
public class ValidationController {

    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final RateLimiterService rateLimiterService;

    /**
     * Check if a username is available
     */
    @GetMapping("/check-username")
    @Operation(summary = "Check if a username is available")
    public ResponseEntity<ApiResponse> checkUsername(@RequestParam String username, HttpServletRequest request) {
        // Apply rate limiting to prevent enumeration attacks
        String ipAddress = securityUtils.getClientIpAddress(request);
        rateLimiterService.checkRateLimit(ipAddress);

        // Sanitize input
        username = securityUtils.sanitizeInput(username);
        boolean exists = userRepository.existsByUsername(username);

        Map<String, Boolean> data = new HashMap<>();
        data.put("available", !exists);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Username availability checked")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * Check if an email is available
     */
    @GetMapping("/check-email")
    @Operation(summary = "Check if an email is available")
    public ResponseEntity<ApiResponse> checkEmail(@RequestParam String email, HttpServletRequest request) {
        // Apply rate limiting to prevent enumeration attacks
        String ipAddress = securityUtils.getClientIpAddress(request);
        rateLimiterService.checkRateLimit(ipAddress);

        // Sanitize input
        email = securityUtils.sanitizeInput(email);
        boolean exists = userRepository.existsByEmail(email);

        Map<String, Boolean> data = new HashMap<>();
        data.put("available", !exists);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Email availability checked")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build());
    }
}