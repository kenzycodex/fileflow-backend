package com.fileflow.auth.controller;

import com.fileflow.controller.EmailVerificationController;
import com.fileflow.dto.request.auth.EmailVerificationRequest;
import com.fileflow.dto.request.auth.ResendVerificationRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.service.auth.AuthService;
import com.fileflow.service.security.RateLimiterService;
import com.fileflow.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EmailVerificationControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private EmailVerificationController emailVerificationController;

    private MockHttpServletRequest mockRequest;
    private ApiResponse successResponse;

    @BeforeEach
    void setUp() {
        mockRequest = new MockHttpServletRequest();

        successResponse = ApiResponse.builder()
                .success(true)
                .message("Email verified successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should verify email successfully with token (GET method)")
    void shouldVerifyEmailSuccessfully_withTokenGetMethod() {
        // Arrange
        String token = "valid-verification-token";
        String ipAddress = "127.0.0.1";

        when(securityUtils.getClientIpAddress(mockRequest)).thenReturn(ipAddress);
        when(authService.verifyEmail(token)).thenReturn(successResponse);

        // Act
        ResponseEntity<ApiResponse> responseEntity = emailVerificationController.verifyEmail(token, mockRequest);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(successResponse, responseEntity.getBody());

        verify(securityUtils).getClientIpAddress(mockRequest);
        verify(rateLimiterService).checkRateLimit(ipAddress);
        verify(authService).verifyEmail(token);
    }

    @Test
    @DisplayName("Should verify email successfully with token from request body (POST method)")
    void shouldVerifyEmailSuccessfully_withTokenFromBodyPostMethod() {
        // Arrange
        EmailVerificationRequest request = new EmailVerificationRequest();
        request.setToken("valid-verification-token");

        String ipAddress = "127.0.0.1";

        when(securityUtils.getClientIpAddress(mockRequest)).thenReturn(ipAddress);
        when(authService.verifyEmail("valid-verification-token")).thenReturn(successResponse);

        // Act
        ResponseEntity<ApiResponse> responseEntity = emailVerificationController.verifyEmailPost(request, null, mockRequest);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(successResponse, responseEntity.getBody());

        verify(securityUtils).getClientIpAddress(mockRequest);
        verify(rateLimiterService).checkRateLimit(ipAddress);
        verify(authService).verifyEmail("valid-verification-token");
    }

    @Test
    @DisplayName("Should verify email successfully with token from query param (POST method)")
    void shouldVerifyEmailSuccessfully_withTokenFromQueryParamPostMethod() {
        // Arrange
        String token = "valid-verification-token";
        String ipAddress = "127.0.0.1";

        when(securityUtils.getClientIpAddress(mockRequest)).thenReturn(ipAddress);
        when(authService.verifyEmail(token)).thenReturn(successResponse);

        // Act
        ResponseEntity<ApiResponse> responseEntity = emailVerificationController.verifyEmailPost(null, token, mockRequest);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(successResponse, responseEntity.getBody());

        verify(securityUtils).getClientIpAddress(mockRequest);
        verify(rateLimiterService).checkRateLimit(ipAddress);
        verify(authService).verifyEmail(token);
    }

    @Test
    @DisplayName("Should return bad request when no token provided (POST method)")
    void shouldReturnBadRequest_whenNoTokenProvided_PostMethod() {
        // Arrange
        String ipAddress = "127.0.0.1";
        when(securityUtils.getClientIpAddress(mockRequest)).thenReturn(ipAddress);

        // Act
        ResponseEntity<ApiResponse> responseEntity = emailVerificationController.verifyEmailPost(null, null, mockRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(false, responseEntity.getBody().isSuccess());
        assertEquals("Verification token is required", responseEntity.getBody().getMessage());

        verify(securityUtils).getClientIpAddress(mockRequest);
        verify(rateLimiterService).checkRateLimit(ipAddress);
    }

    @Test
    @DisplayName("Should resend verification email successfully with email from request body")
    void shouldResendVerificationEmailSuccessfully_withEmailFromBody() {
        // Arrange
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("test@example.com");

        String ipAddress = "127.0.0.1";

        ApiResponse resendResponse = ApiResponse.builder()
                .success(true)
                .message("Verification email sent successfully")
                .timestamp(LocalDateTime.now())
                .build();

        when(securityUtils.getClientIpAddress(mockRequest)).thenReturn(ipAddress);
        when(authService.resendVerificationEmail("test@example.com")).thenReturn(resendResponse);

        // Act
        ResponseEntity<ApiResponse> responseEntity = emailVerificationController.resendVerificationEmail(request, null, mockRequest);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(resendResponse, responseEntity.getBody());

        verify(securityUtils).getClientIpAddress(mockRequest);
        verify(rateLimiterService).checkRateLimit(ipAddress);
        verify(authService).resendVerificationEmail("test@example.com");
    }

    @Test
    @DisplayName("Should resend verification email successfully with email from query param")
    void shouldResendVerificationEmailSuccessfully_withEmailFromQueryParam() {
        // Arrange
        String email = "test@example.com";
        String ipAddress = "127.0.0.1";

        ApiResponse resendResponse = ApiResponse.builder()
                .success(true)
                .message("Verification email sent successfully")
                .timestamp(LocalDateTime.now())
                .build();

        when(securityUtils.getClientIpAddress(mockRequest)).thenReturn(ipAddress);
        when(authService.resendVerificationEmail(email)).thenReturn(resendResponse);

        // Act
        ResponseEntity<ApiResponse> responseEntity = emailVerificationController.resendVerificationEmail(null, email, mockRequest);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(resendResponse, responseEntity.getBody());

        verify(securityUtils).getClientIpAddress(mockRequest);
        verify(rateLimiterService).checkRateLimit(ipAddress);
        verify(authService).resendVerificationEmail(email);
    }

    @Test
    @DisplayName("Should return bad request when no email provided for resend")
    void shouldReturnBadRequest_whenNoEmailProvided_ForResend() {
        // Arrange
        String ipAddress = "127.0.0.1";
        when(securityUtils.getClientIpAddress(mockRequest)).thenReturn(ipAddress);

        // Act
        ResponseEntity<ApiResponse> responseEntity = emailVerificationController.resendVerificationEmail(null, null, mockRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(false, responseEntity.getBody().isSuccess());
        assertEquals("Email is required", responseEntity.getBody().getMessage());

        verify(securityUtils).getClientIpAddress(mockRequest);
        verify(rateLimiterService).checkRateLimit(ipAddress);
    }
}