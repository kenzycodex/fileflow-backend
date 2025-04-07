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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    @DisplayName("Should authenticate user successfully when valid credentials are provided")
    void shouldAuthenticateUserSuccessfully_whenValidCredentialsProvided() {
        // Arrange
        SignInRequest signInRequest = new SignInRequest();
        signInRequest.setUsernameOrEmail("testuser");
        signInRequest.setPassword("password123");

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

        ApiResponse expectedResponse = ApiResponse.builder()
                .success(true)
                .message("Password reset instructions sent to your email")
                .timestamp(LocalDateTime.now())
                .build();

        when(authService.forgotPassword(email)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<ApiResponse> responseEntity = authController.forgotPassword(email);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());
        assertEquals(expectedResponse, responseEntity.getBody());
    }

    @Test
    @DisplayName("Should reset password successfully when valid token and request are provided")
    void shouldResetPasswordSuccessfully_whenValidTokenAndRequestProvided() {
        // Arrange
        String token = "valid-reset-token";
        PasswordResetRequest request = new PasswordResetRequest();
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        ApiResponse expectedResponse = ApiResponse.builder()
                .success(true)
                .message("Password has been reset successfully")
                .timestamp(LocalDateTime.now())
                .build();

        when(authService.resetPassword(eq(token), any(PasswordResetRequest.class))).thenReturn(expectedResponse);

        // Act
        ResponseEntity<ApiResponse> responseEntity = authController.resetPassword(token, request);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());
        assertEquals(expectedResponse, responseEntity.getBody());
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

        ApiResponse expectedResponse = ApiResponse.builder()
                .success(true)
                .message("Logged out successfully")
                .timestamp(LocalDateTime.now())
                .build();

        when(authService.logout(refreshToken)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<ApiResponse> responseEntity = authController.logout(refreshToken);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());
        assertEquals(expectedResponse, responseEntity.getBody());
    }
}