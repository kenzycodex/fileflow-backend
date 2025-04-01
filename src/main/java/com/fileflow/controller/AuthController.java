package com.fileflow.controller;

import com.fileflow.dto.request.auth.PasswordResetRequest;
import com.fileflow.dto.request.auth.RefreshTokenRequest;
import com.fileflow.dto.request.auth.SignInRequest;
import com.fileflow.dto.request.auth.SignUpRequest;
import com.fileflow.dto.response.auth.JwtResponse;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication API")
public class AuthController {

    private final AuthService authService;

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
    public ResponseEntity<ApiResponse> forgotPassword(@RequestParam String email) {
        ApiResponse response = authService.forgotPassword(email);
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
            @Valid @RequestBody PasswordResetRequest request) {
        ApiResponse response = authService.resetPassword(token, request);
        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @GetMapping(
            value = "/validate-token",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Validate JWT token")
    public ResponseEntity<ApiResponse> validateToken(@RequestParam String token) {
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
    public ResponseEntity<ApiResponse> logout(@RequestParam(required = false) String refreshToken) {
        ApiResponse response = authService.logout(refreshToken);
        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
}