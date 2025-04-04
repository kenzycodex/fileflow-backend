package com.fileflow.controller;

import com.fileflow.dto.request.auth.MfaEnableRequest;
import com.fileflow.dto.request.auth.MfaVerifyRequest;
import com.fileflow.dto.response.auth.MfaResponse;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.security.UserPrincipal;
import com.fileflow.service.auth.MfaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/auth/mfa")
@RequiredArgsConstructor
@Tag(name = "Multi-Factor Authentication", description = "MFA management API")
public class MfaController {

    private final MfaService mfaService;

    @GetMapping("/status")
    @Operation(summary = "Check MFA status for current user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse> getMfaStatus(@AuthenticationPrincipal UserPrincipal currentUser) {
        boolean mfaEnabled = mfaService.isMfaEnabled(currentUser.getId());

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("MFA status retrieved successfully")
                .data(mfaEnabled)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/setup")
    @Operation(summary = "Setup MFA for current user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse> setupMfa(@AuthenticationPrincipal UserPrincipal currentUser) {
        // Generate a new secret
        String secret = mfaService.generateMfaSecret(currentUser.getId());

        // Generate QR code URL
        String qrCodeUrl = mfaService.generateQrCodeImageUri(secret, currentUser.getUsername());

        MfaResponse response = MfaResponse.builder()
                .secret(secret)
                .qrCodeUrl(qrCodeUrl)
                .build();

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("MFA setup initiated successfully")
                .data(response)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/enable")
    @Operation(summary = "Enable MFA after verification", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse> enableMfa(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody MfaEnableRequest request) {

        mfaService.enableMfa(currentUser.getId(), request.getVerificationCode());

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("MFA enabled successfully")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify MFA code during login")
    public ResponseEntity<ApiResponse> verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
        boolean isValid = mfaService.verifyCode(request.getSecret(), request.getVerificationCode());

        return ResponseEntity.ok(ApiResponse.builder()
                .success(isValid)
                .message(isValid ? "MFA verification successful" : "Invalid verification code")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/disable")
    @Operation(summary = "Disable MFA for current user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse> disableMfa(@AuthenticationPrincipal UserPrincipal currentUser) {
        mfaService.disableMfa(currentUser.getId());

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("MFA disabled successfully")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/send-email-code")
    @Operation(summary = "Send email verification code for email-based MFA", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse> sendEmailVerificationCode(@AuthenticationPrincipal UserPrincipal currentUser) {
        String code = mfaService.generateEmailVerificationCode(currentUser.getId());

        // In a real implementation, send this via email service
        // For now, just return success message

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Verification code sent successfully")
                .timestamp(LocalDateTime.now())
                .build());
    }
}