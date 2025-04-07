package com.fileflow.auth.controller;

import com.fileflow.controller.MfaController;
import com.fileflow.dto.request.auth.MfaEnableRequest;
import com.fileflow.dto.request.auth.MfaVerifyRequest;
import com.fileflow.dto.response.auth.MfaResponse;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.security.UserPrincipal;
import com.fileflow.service.auth.MfaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MfaControllerTest {

    @Mock
    private MfaService mfaService;

    @InjectMocks
    private MfaController mfaController;

    private UserPrincipal currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new UserPrincipal(
                1L,
                "Test",
                "User",
                "testuser",
                "test@example.com",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                true
        );
    }

    @Test
    @DisplayName("Should return MFA status successfully")
    void shouldReturnMfaStatusSuccessfully() {
        // Arrange
        when(mfaService.isMfaEnabled(1L)).thenReturn(true);

        // Act
        ResponseEntity<ApiResponse> responseEntity = mfaController.getMfaStatus(currentUser);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(true, responseEntity.getBody().isSuccess());
        assertEquals(true, responseEntity.getBody().getData());
        assertEquals("MFA status retrieved successfully", responseEntity.getBody().getMessage());

        verify(mfaService).isMfaEnabled(1L);
    }

    @Test
    @DisplayName("Should setup MFA successfully")
    void shouldSetupMfaSuccessfully() {
        // Arrange
        String secret = "ABCDEFGHIJKLMNOP";
        String qrCodeUrl = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

        when(mfaService.generateMfaSecret(1L)).thenReturn(secret);
        when(mfaService.generateQrCodeImageUri(secret, "testuser")).thenReturn(qrCodeUrl);

        // Act
        ResponseEntity<ApiResponse> responseEntity = mfaController.setupMfa(currentUser);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(true, responseEntity.getBody().isSuccess());
        assertEquals("MFA setup initiated successfully", responseEntity.getBody().getMessage());

        MfaResponse response = (MfaResponse) responseEntity.getBody().getData();
        assertEquals(secret, response.getSecret());
        assertEquals(qrCodeUrl, response.getQrCodeUrl());

        verify(mfaService).generateMfaSecret(1L);
        verify(mfaService).generateQrCodeImageUri(secret, "testuser");
    }

    @Test
    @DisplayName("Should enable MFA successfully")
    void shouldEnableMfaSuccessfully() {
        // Arrange
        MfaEnableRequest request = new MfaEnableRequest();
        request.setVerificationCode("123456");

        doNothing().when(mfaService).enableMfa(1L, "123456");

        // Act
        ResponseEntity<ApiResponse> responseEntity = mfaController.enableMfa(currentUser, request);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(true, responseEntity.getBody().isSuccess());
        assertEquals("MFA enabled successfully", responseEntity.getBody().getMessage());

        verify(mfaService).enableMfa(1L, "123456");
    }

    @Test
    @DisplayName("Should verify MFA code successfully")
    void shouldVerifyMfaCodeSuccessfully() {
        // Arrange
        MfaVerifyRequest request = new MfaVerifyRequest();
        request.setSecret("ABCDEFGHIJKLMNOP");
        request.setVerificationCode("123456");

        when(mfaService.verifyCode("ABCDEFGHIJKLMNOP", "123456")).thenReturn(true);

        // Act
        ResponseEntity<ApiResponse> responseEntity = mfaController.verifyMfa(request);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(true, responseEntity.getBody().isSuccess());
        assertEquals("MFA verification successful", responseEntity.getBody().getMessage());

        verify(mfaService).verifyCode("ABCDEFGHIJKLMNOP", "123456");
    }

    @Test
    @DisplayName("Should reject invalid MFA code")
    void shouldRejectInvalidMfaCode() {
        // Arrange
        MfaVerifyRequest request = new MfaVerifyRequest();
        request.setSecret("ABCDEFGHIJKLMNOP");
        request.setVerificationCode("999999");

        when(mfaService.verifyCode("ABCDEFGHIJKLMNOP", "999999")).thenReturn(false);

        // Act
        ResponseEntity<ApiResponse> responseEntity = mfaController.verifyMfa(request);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(false, responseEntity.getBody().isSuccess());
        assertEquals("Invalid verification code", responseEntity.getBody().getMessage());

        verify(mfaService).verifyCode("ABCDEFGHIJKLMNOP", "999999");
    }

    @Test
    @DisplayName("Should disable MFA successfully")
    void shouldDisableMfaSuccessfully() {
        // Arrange
        doNothing().when(mfaService).disableMfa(1L);

        // Act
        ResponseEntity<ApiResponse> responseEntity = mfaController.disableMfa(currentUser);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(true, responseEntity.getBody().isSuccess());
        assertEquals("MFA disabled successfully", responseEntity.getBody().getMessage());

        verify(mfaService).disableMfa(1L);
    }

    @Test
    @DisplayName("Should send email verification code successfully")
    void shouldSendEmailVerificationCodeSuccessfully() {
        // Arrange
        when(mfaService.generateEmailVerificationCode(1L)).thenReturn("123456");

        // Act
        ResponseEntity<ApiResponse> responseEntity = mfaController.sendEmailVerificationCode(currentUser);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(true, responseEntity.getBody().isSuccess());
        assertEquals("Verification code sent successfully", responseEntity.getBody().getMessage());

        verify(mfaService).generateEmailVerificationCode(1L);
    }
}