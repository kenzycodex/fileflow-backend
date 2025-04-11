package com.fileflow.controller;

import com.fileflow.dto.request.auth.PasswordResetRequest;
import com.fileflow.dto.request.auth.RefreshTokenRequest;
import com.fileflow.dto.request.auth.SignInRequest;
import com.fileflow.dto.request.auth.SignUpRequest;
import com.fileflow.dto.response.auth.JwtResponse;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.service.auth.AuthService;
import com.fileflow.service.security.AuditLogService;
import com.fileflow.service.security.RateLimiterService;
import com.fileflow.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication API")
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;
    private final SecurityUtils securityUtils;
    private final AuditLogService auditLogService;

    @PostMapping(
            value = "/signin",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Sign in a user and get JWT tokens")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody SignInRequest signInRequest) {
        JwtResponse response = authService.signIn(signInRequest);
        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @PostMapping(
            value = "/signup",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        ApiResponse response = authService.signUp(signUpRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @PostMapping(
            value = "/refresh",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Refresh JWT token")
    public ResponseEntity<JwtResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        JwtResponse response = authService.refreshToken(refreshTokenRequest);
        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @PostMapping(
            value = "/forgot-password",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Request password reset")
    public ResponseEntity<ApiResponse> forgotPassword(@RequestParam String email, HttpServletRequest request) {
        // Apply rate limiting
        String ipAddress = securityUtils.getClientIpAddress(request);
        boolean emailAllowed = rateLimiterService.checkPasswordResetRateLimit(email);
        boolean ipAllowed = rateLimiterService.checkRateLimit(ipAddress);

        if (!emailAllowed) {
            log.warn("Email rate limit exceeded for password reset: {}", email);
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Too many password reset requests. Please try again later.")
                            .timestamp(LocalDateTime.now())
                            .build());
        }

        if (!ipAllowed) {
            log.warn("IP rate limit exceeded for password reset: {}", ipAddress);
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Too many requests. Please try again later.")
                            .timestamp(LocalDateTime.now())
                            .build());
        }

        log.info("Processing password reset request for email: {}", email);
        ApiResponse response = authService.forgotPassword(email);

        // Audit log the password reset request
        auditLogService.logPasswordReset(
                response.isSuccess(),
                email,
                ipAddress,
                securityUtils.getUserAgent(request)
        );

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    // Support both GET and POST methods for reset-password with token parameter
    @GetMapping(
            value = "/reset-password",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Validate password reset token (GET)")
    public ResponseEntity<ApiResponse> validateResetToken(@RequestParam String token, HttpServletRequest request) {
        // Apply rate limiting
        String ipAddress = securityUtils.getClientIpAddress(request);
        boolean allowed = rateLimiterService.checkRateLimit(ipAddress);

        if (!allowed) {
            log.warn("IP rate limit exceeded for token validation: {}", ipAddress);
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Too many requests. Please try again later.")
                            .timestamp(LocalDateTime.now())
                            .build());
        }

        log.info("Validating password reset token (GET): {}", token);
        ApiResponse response = authService.validateResetToken(token);

        // Log token validation
        auditLogService.logTokenValidation(
                "RESET",
                response.isSuccess(),
                ipAddress,
                securityUtils.getUserAgent(request),
                null // User ID is unknown at this point
        );

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @PostMapping(
            value = "/reset-password",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Reset password with token")
    public ResponseEntity<ApiResponse> resetPassword(
            @RequestParam String token,
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {
        // Apply enhanced rate limiting
        String ipAddress = securityUtils.getClientIpAddress(httpRequest);

        // First check IP-based rate limit
        if (!rateLimiterService.checkRateLimit(ipAddress)) {
            log.warn("IP rate limit exceeded for password reset: {}", ipAddress);
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Too many requests. Please try again later.")
                            .timestamp(LocalDateTime.now())
                            .build());
        }

        // Then check token-based rate limit to prevent brute force
        if (!rateLimiterService.checkResetTokenRateLimit(token)) {
            log.warn("Token rate limit exceeded for password reset token: {}", token);
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Too many attempts with this token. Please request a new password reset.")
                            .timestamp(LocalDateTime.now())
                            .build());
        }

        // If email is provided, also rate limit by email
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (!rateLimiterService.checkPasswordResetRateLimit(request.getEmail())) {
                log.warn("Email rate limit exceeded for password reset: {}", request.getEmail());
                return ResponseEntity
                        .status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ApiResponse.builder()
                                .success(false)
                                .message("Too many password reset attempts. Please try again later.")
                                .timestamp(LocalDateTime.now())
                                .build());
            }
        }

        log.info("Processing password reset with token: {}", token);
        ApiResponse response = authService.resetPassword(token, request);

        // Audit logging for security
        auditLogService.logPasswordReset(
                response.isSuccess(),
                request.getEmail(),
                securityUtils.getClientIpAddress(httpRequest),
                securityUtils.getUserAgent(httpRequest)
        );

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * Validate JWT token or verification token
     * Enhanced to handle both types of tokens
     */
    @GetMapping(
            value = "/validate-token",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Validate JWT token or verification token")
    public ResponseEntity<ApiResponse> validateToken(@RequestParam String token) {
        log.info("Token validation request received for token: {}", token.substring(0, Math.min(10, token.length())) + "...");

        // First try to validate as a verification token if it appears to be a UUID
        if (token.length() == 36 && token.contains("-")) { // UUID format likely indicates an email verification token
            log.info("Token appears to be a verification token (UUID format), validating...");
            try {
                ApiResponse response = authService.validateVerificationToken(token);
                if (response.isSuccess()) {
                    return ResponseEntity
                            .ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(response);
                }
                // If verification token validation failed, continue to try as JWT token
                log.info("Verification token validation failed, trying as JWT token");
            } catch (Exception e) {
                log.warn("Error validating verification token: {}", e.getMessage());
                // Continue to try as regular JWT token
            }
        }

        // Try as regular JWT token
        ApiResponse response = authService.validateToken(token);
        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @PostMapping(
            value = "/logout",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Logout user")
    public ResponseEntity<ApiResponse> logout(@RequestParam(required = false) String refreshToken, HttpServletRequest request) {
        // Get user details if possible from the JWT token
        Long userId = null;
        try {
            userId = securityUtils.getUserIdFromRequest(request);
        } catch (Exception e) {
            log.debug("Could not extract user ID from request: {}", e.getMessage());
        }

        ApiResponse response = authService.logout(refreshToken);

        // Only log if we have a user ID
        if (userId != null) {
            auditLogService.logLogout(
                    userId,
                    securityUtils.getClientIpAddress(request),
                    securityUtils.getUserAgent(request)
            );
        }

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
}