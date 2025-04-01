package com.fileflow.controller;

import com.fileflow.dto.request.auth.FirebaseAuthRequest;
import com.fileflow.dto.response.auth.JwtResponse;
import com.fileflow.dto.response.auth.UserResponse;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.security.JwtTokenProvider;
import com.fileflow.service.auth.AuthService;
import com.fileflow.service.auth.FirebaseAuthService;
import com.fileflow.service.auth.JwtService;
import com.fileflow.util.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth/social")
@RequiredArgsConstructor
@Tag(name = "Social Authentication", description = "Social Authentication API")
@Slf4j
public class SocialAuthController {

    private final FirebaseAuthService firebaseAuthService;
    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;
    private final JwtService jwtService;

    @PostMapping(value = "/firebase", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Authenticate with Firebase token")
    public ResponseEntity<JwtResponse> authenticateWithFirebase(@Valid @RequestBody FirebaseAuthRequest request) {
        log.info("Processing social login with Firebase");

        // Verify and process Firebase token
        UserResponse userResponse = firebaseAuthService.authenticateWithFirebase(request.getIdToken());

        // Generate JWT tokens for our system
        String accessToken = tokenProvider.generateToken(userResponse.getId());
        String refreshToken = tokenProvider.generateRefreshToken(userResponse.getId());

        // Save refresh token
        jwtService.saveRefreshToken(userResponse.getId(), refreshToken);

        JwtResponse jwtResponse = JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(Constants.ACCESS_TOKEN_EXPIRATION / 1000)  // Convert to seconds
                .user(userResponse)
                .build();

        // Use ResponseEntity.ok() and explicitly set Content-Type
        return ResponseEntity
                .ok()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE) // Explicit header for content type
                .contentType(MediaType.APPLICATION_JSON) // Belt and suspenders approach
                .body(jwtResponse);
    }

    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Logout user")
    public ResponseEntity<ApiResponse> logout(@RequestParam(required = false) String refreshToken) {
        ApiResponse response = authService.logout(refreshToken);

        // Use ResponseEntity.ok() and explicitly set Content-Type
        return ResponseEntity
                .ok()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE) // Explicit header for content type
                .contentType(MediaType.APPLICATION_JSON) // Belt and suspenders approach
                .body(response);
    }
}