package com.fileflow.auth.controller;

import com.fileflow.controller.AuthController;
import com.fileflow.dto.request.auth.PasswordResetRequest;
import com.fileflow.dto.request.auth.RefreshTokenRequest;
import com.fileflow.dto.request.auth.SignInRequest;
import com.fileflow.dto.request.auth.SignUpRequest;
import com.fileflow.dto.response.auth.JwtResponse;
import com.fileflow.dto.response.auth.UserResponse;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.service.auth.AuthService;
import com.fileflow.service.security.AuditLogService;
import com.fileflow.service.security.RateLimiterService;
import com.fileflow.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private HttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        mockRequest = mock(HttpServletRequest.class);

        // We don't set up default behavior for ALL tests here anymore
        // Each test will set up only what it needs
    }

    @Test
    @DisplayName("Should authenticate user successfully when valid credentials are provided")
    void shouldAuthenticateUserSuccessfully_whenValidCredentialsProvided() {
        // Arrange
        SignInRequest signInRequest = new SignInRequest();
        signInRequest.setUsernameOrEmail("testuser");
        signInRequest.setPassword("password123");
        signInRequest.setRememberMe(false);

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        JwtResponse expectedResponse = JwtResponse.builder()
                .accessToken("test-access-token")
                .refreshToken("test-refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(userResponse)
                .build();

        when(authService.signIn(any(SignInRequest.class))).thenReturn(expectedResponse);

        // Act
        ResponseEntity<JwtResponse> responseEntity = authController.authenticateUser(signInRequest);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());
        assertEquals(expectedResponse, responseEntity.getBody());
    }

    @Test
    @DisplayName("Should register user successfully when valid signup request is provided")
    void shouldRegisterUserSuccessfully_whenValidSignupRequestProvided() {
        // Arrange
        SignUpRequest signUpRequest = new SignUpRequest();
        signUpRequest.setFirstName("Test");
        signUpRequest.setLastName("User");
        signUpRequest.setUsername("testuser");
        signUpRequest.setEmail("test@example.com");
        signUpRequest.setPassword("password123");
        signUpRequest.setConfirmPassword("password123");

        ApiResponse expectedResponse = ApiResponse.builder()
                .success(true)
                .message("User registered successfully. Please verify your email.")
                .timestamp(LocalDateTime.now())
                .build();

        when(authService.signUp(any(SignUpRequest.class))).thenReturn(expectedResponse);

        // Act
        ResponseEntity<ApiResponse> responseEntity = authController.registerUser(signUpRequest);

        // Assert
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());
        assertEquals(expectedResponse, responseEntity.getBody());
    }

    @Test
    @DisplayName("Should refresh token successfully when valid refresh token is provided")
    void shouldRefreshTokenSuccessfully_whenValidRefreshTokenProvided() {
        // Arrange
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setRefreshToken("valid-refresh-token");

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        JwtResponse expectedResponse = JwtResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(userResponse)
                .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(expectedResponse);

        // Act
        ResponseEntity<JwtResponse> responseEntity = authController.refreshToken(refreshTokenRequest);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());
        assertEquals(expectedResponse, responseEntity.getBody());
    }

    @Test
    @DisplayName("Should request password reset successfully when valid email is provided")
    void shouldRequestPasswordResetSuccessfully_whenValidEmailProvided() {
        // Arrange
        String email = "test@example.com";
        String ipAddress = "127.0.0.1";

        ApiResponse expectedResponse = ApiResponse.builder()
                .success(true)
                .message("Password reset instructions sent to your email")
                .timestamp(LocalDateTime.now())
                .build();

        // Configure mocks for dependencies - only what's needed for this test
        when(securityUtils.getClientIpAddress(mockRequest)).thenReturn(ipAddress);
        when(securityUtils.getUserAgent(mockRequest)).thenReturn("test-user-agent");
        when(rateLimiterService.checkPasswordResetRateLimit(email)).thenReturn(true);
        when(rateLimiterService.checkRateLimit(ipAddress)).thenReturn(true);
        when(authService.forgotPassword(email)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<ApiResponse> responseEntity = authController.forgotPassword(email, mockRequest);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());
        assertEquals(expectedResponse, responseEntity.getBody());

        // Verify audit logging was called
        verify(auditLogService).logPasswordReset(eq(true), eq(email), eq(ipAddress), anyString());
    }

    @Test
    @DisplayName("Should validate reset token successfully")
    void shouldValidateResetTokenSuccessfully() {
        // Arrange
        String token = "valid-reset-token";
        String ipAddress = "127.0.0.1";

        ApiResponse expectedResponse = ApiResponse.builder()
                .success(true)
                .message("Token is valid")
                .timestamp(LocalDateTime.now())
                .build();

        when(securityUtils.getClientIpAddress(mockRequest)).thenReturn(ipAddress);
        when(securityUtils.getUserAgent(mockRequest)).thenReturn("test-user-agent");
        when(rateLimiterService.checkRateLimit(ipAddress)).thenReturn(true);
        when(authService.validateResetToken(token)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<ApiResponse> responseEntity = authController.validateResetToken(token, mockRequest);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());
        assertEquals(expectedResponse, responseEntity.getBody());

        // Verify token validation was logged
        verify(auditLogService).logTokenValidation(
                eq("RESET"),
                eq(true),
                eq(ipAddress),
                anyString(),
                eq(null));
    }

    @Test
    @DisplayName("Should reset password successfully when valid token and request are provided")
    void shouldResetPasswordSuccessfully_whenValidTokenAndRequestProvided() {
        // Arrange
        String token = "valid-reset-token";
        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail("test@example.com");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");
        request.setToken(token);
        String ipAddress = "127.0.0.1";

        ApiResponse expectedResponse = ApiResponse.builder()
                .success(true)
                .message("Password has been reset successfully")
                .timestamp(LocalDateTime.now())
                .build();

        // Configure mocks for dependencies
        when(securityUtils.getClientIpAddress(mockRequest)).thenReturn(ipAddress);
        when(securityUtils.getUserAgent(mockRequest)).thenReturn("test-user-agent");
        when(rateLimiterService.checkRateLimit(ipAddress)).thenReturn(true);
        when(rateLimiterService.checkResetTokenRateLimit(token)).thenReturn(true);
        when(rateLimiterService.checkPasswordResetRateLimit(request.getEmail())).thenReturn(true);
        when(authService.resetPassword(eq(token), any(PasswordResetRequest.class))).thenReturn(expectedResponse);

        // Act
        ResponseEntity<ApiResponse> responseEntity = authController.resetPassword(token, request, mockRequest);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());
        assertEquals(expectedResponse, responseEntity.getBody());

        // Verify audit logging was called
        verify(auditLogService).logPasswordReset(
                eq(true),
                eq(request.getEmail()),
                eq(ipAddress),
                anyString());
    }

    @Test
    @DisplayName("Should return rate limit error when IP rate limit is exceeded")
    void shouldReturnRateLimitError_whenIpRateLimitExceeded() {
        // Arrange
        String token = "valid-reset-token";
        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail("test@example.com");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");
        String ipAddress = "127.0.0.1";

        // Configure mocks to simulate rate limiting
        when(securityUtils.getClientIpAddress(mockRequest)).thenReturn(ipAddress);
        when(rateLimiterService.checkRateLimit(ipAddress)).thenReturn(false);
        // No need to mock other checks since this one should fail first

        // Act
        ResponseEntity<ApiResponse> responseEntity = authController.resetPassword(token, request, mockRequest);

        // Assert
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(false, responseEntity.getBody().isSuccess());
        assertEquals("Too many requests. Please try again later.", responseEntity.getBody().getMessage());
    }

    @Test
    @DisplayName("Should validate token successfully when valid token is provided")
    void shouldValidateTokenSuccessfully_whenValidTokenProvided() {
        // Arrange
        String token = "valid-token";

        ApiResponse expectedResponse = ApiResponse.builder()
                .success(true)
                .message("Token is valid")
                .timestamp(LocalDateTime.now())
                .build();

        when(authService.validateToken(token)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<ApiResponse> responseEntity = authController.validateToken(token);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());
        assertEquals(expectedResponse, responseEntity.getBody());
    }

    @Test
    @DisplayName("Should logout successfully when valid refresh token is provided")
    void shouldLogoutSuccessfully_whenValidRefreshTokenProvided() {
        // Arrange
        String refreshToken = "valid-refresh-token";
        String ipAddress = "127.0.0.1";
        Long userId = 1L; // Mock user ID

        ApiResponse expectedResponse = ApiResponse.builder()
                .success(true)
                .message("Logged out successfully")
                .timestamp(LocalDateTime.now())
                .build();

        // Setup mocks for the updated logout method
        when(securityUtils.getClientIpAddress(mockRequest)).thenReturn(ipAddress);
        when(securityUtils.getUserAgent(mockRequest)).thenReturn("test-user-agent");
        when(securityUtils.getUserIdFromRequest(mockRequest)).thenReturn(userId);
        when(authService.logout(refreshToken)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<ApiResponse> responseEntity = authController.logout(refreshToken, mockRequest);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());
        assertEquals(expectedResponse, responseEntity.getBody());

        // Verify logout was logged
        verify(auditLogService).logLogout(eq(userId), eq(ipAddress), anyString());
    }
}