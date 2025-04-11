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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for email verification
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Email Verification", description = "API endpoints for email verification")
@Slf4j
public class EmailVerificationController {

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;
    private final SecurityUtils securityUtils;

    /**
     * Verify email using token in path
     * This ensures compatibility with email links
     */
    @GetMapping(
            value = "/verify-email",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Verify email using token from query parameter")
    public ResponseEntity<ApiResponse> verifyEmailGet(@RequestParam String token, HttpServletRequest request) {
        // Apply rate limiting
        String ipAddress = securityUtils.getClientIpAddress(request);
        rateLimiterService.checkRateLimit(ipAddress);

        log.info("Verifying email with token via GET (verify-email): {}", token);
        ApiResponse response = authService.verifyEmail(token);
        return ResponseEntity.ok(response);
    }

    /**
     * Verify email using token
     * Legacy endpoint maintained for backward compatibility
     */
    @GetMapping(
            value = "/verify",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Verify email using token (GET method)")
    public ResponseEntity<ApiResponse> verifyEmail(@RequestParam String token, HttpServletRequest request) {
        // Apply rate limiting
        String ipAddress = securityUtils.getClientIpAddress(request);
        rateLimiterService.checkRateLimit(ipAddress);

        log.info("Verifying email with token via GET: {}", token);
        ApiResponse response = authService.verifyEmail(token);
        return ResponseEntity.ok(response);
    }

    /**
     * Verify email using token (POST version)
     */
    @PostMapping(
            value = "/verify",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Verify email using token (POST version)")
    public ResponseEntity<ApiResponse> verifyEmailPost(@Valid @RequestBody(required = false) EmailVerificationRequest requestBody,
                                                       @RequestParam(required = false) String token,
                                                       HttpServletRequest httpRequest) {
        // Apply rate limiting
        String ipAddress = securityUtils.getClientIpAddress(httpRequest);
        rateLimiterService.checkRateLimit(ipAddress);

        // Use token from body or query param
        String verificationToken = null;

        if (requestBody != null && requestBody.getToken() != null) {
            verificationToken = requestBody.getToken();
            log.info("Verifying email with token from request body: {}", verificationToken);
        } else if (token != null) {
            verificationToken = token;
            log.info("Verifying email with token from query param: {}", verificationToken);
        } else {
            log.warn("No token provided for email verification");
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .success(false)
                            .message("Verification token is required")
                            .build()
            );
        }

        ApiResponse response = authService.verifyEmail(verificationToken);
        return ResponseEntity.ok(response);
    }

    /**
     * Validates email verification token without performing verification
     * Used by the front-end to check if token is valid before showing verification UI
     */
    @GetMapping(
            value = "/validate-verification-token",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Validate verification token without confirming")
    public ResponseEntity<ApiResponse> validateVerificationToken(@RequestParam String token, HttpServletRequest request) {
        // Apply rate limiting
        String ipAddress = securityUtils.getClientIpAddress(request);
        rateLimiterService.checkRateLimit(ipAddress);

        log.info("Validating verification token: {}", token);
        ApiResponse response = authService.validateVerificationToken(token);
        return ResponseEntity.ok(response);
    }

    /**
     * Resend verification email
     */
    @PostMapping(
            value = "/verify/resend",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Resend verification email")
    public ResponseEntity<ApiResponse> resendVerificationEmail(@Valid @RequestBody(required = false) ResendVerificationRequest requestBody,
                                                               @RequestParam(required = false) String email,
                                                               HttpServletRequest httpRequest) {
        // Apply rate limiting
        String ipAddress = securityUtils.getClientIpAddress(httpRequest);
        rateLimiterService.checkRateLimit(ipAddress);

        // Use email from body or query param
        String userEmail = null;

        if (requestBody != null && requestBody.getEmail() != null) {
            userEmail = requestBody.getEmail();
            log.info("Resending verification email to: {} (from request body)", userEmail);
        } else if (email != null) {
            userEmail = email;
            log.info("Resending verification email to: {} (from query param)", userEmail);
        } else {
            log.warn("No email provided for resending verification");
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .success(false)
                            .message("Email is required")
                            .build()
            );
        }

        ApiResponse response = authService.resendVerificationEmail(userEmail);
        return ResponseEntity.ok(response);
    }
}