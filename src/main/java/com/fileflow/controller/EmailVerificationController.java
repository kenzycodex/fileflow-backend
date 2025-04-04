package com.fileflow.controller;

import com.fileflow.dto.request.auth.EmailVerificationRequest;
import com.fileflow.dto.request.auth.ResendVerificationRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.service.auth.AuthService;
import com.fileflow.service.security.RateLimiterService;
import com.fileflow.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for email verification
 */
@RestController
@RequestMapping("/api/v1/auth/verify")
@RequiredArgsConstructor
@Tag(name = "Email Verification", description = "API endpoints for email verification")
@Slf4j
public class EmailVerificationController {

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;
    private final SecurityUtils securityUtils;

    /**
     * Verify email using token
     */
    @GetMapping
    @Operation(summary = "Verify email using token")
    public ResponseEntity<ApiResponse> verifyEmail(@RequestParam String token, HttpServletRequest request) {
        // Apply rate limiting
        String ipAddress = securityUtils.getClientIpAddress(request);
        rateLimiterService.checkRateLimit(ipAddress);

        ApiResponse response = authService.verifyEmail(token);
        return ResponseEntity.ok(response);
    }

    /**
     * Verify email using token (POST version)
     */
    @PostMapping
    @Operation(summary = "Verify email using token (POST version)")
    public ResponseEntity<ApiResponse> verifyEmailPost(@Valid @RequestBody EmailVerificationRequest request,
                                                       HttpServletRequest httpRequest) {
        // Apply rate limiting
        String ipAddress = securityUtils.getClientIpAddress(httpRequest);
        rateLimiterService.checkRateLimit(ipAddress);

        ApiResponse response = authService.verifyEmail(request.getToken());
        return ResponseEntity.ok(response);
    }

    /**
     * Resend verification email
     */
    @PostMapping("/resend")
    @Operation(summary = "Resend verification email")
    public ResponseEntity<ApiResponse> resendVerificationEmail(@Valid @RequestBody ResendVerificationRequest request,
                                                               HttpServletRequest httpRequest) {
        // Apply rate limiting
        String ipAddress = securityUtils.getClientIpAddress(httpRequest);
        rateLimiterService.checkRateLimit(ipAddress);

        ApiResponse response = authService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(response);
    }
}