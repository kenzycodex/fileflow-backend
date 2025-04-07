package com.fileflow.auth.controller;

import com.fileflow.controller.SocialAuthController;
import com.fileflow.dto.request.auth.FirebaseAuthRequest;
import com.fileflow.dto.response.auth.JwtResponse;
import com.fileflow.dto.response.auth.UserResponse;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.security.JwtTokenProvider;
import com.fileflow.service.auth.AuthService;
import com.fileflow.service.auth.FirebaseAuthService;
import com.fileflow.service.auth.JwtService;
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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SocialAuthControllerTest {

    @Mock
    private FirebaseAuthService firebaseAuthService;

    @Mock
    private AuthService authService;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private SocialAuthController socialAuthController;

    private UserResponse userResponse;
    private ApiResponse logoutResponse;

    @BeforeEach
    void setUp() {
        userResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .emailVerified(true)
                .authProvider("GOOGLE")
                .build();

        logoutResponse = ApiResponse.builder()
                .success(true)
                .message("Logged out successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should authenticate with Firebase successfully")
    void shouldAuthenticateWithFirebaseSuccessfully() {
        // Arrange
        FirebaseAuthRequest request = new FirebaseAuthRequest();
        request.setIdToken("firebase-id-token");

        when(firebaseAuthService.authenticateWithFirebase("firebase-id-token")).thenReturn(userResponse);
        when(tokenProvider.generateToken(1L)).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(1L)).thenReturn("refresh-token");

        // Act
        ResponseEntity<JwtResponse> responseEntity = socialAuthController.authenticateWithFirebase(request);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());

        JwtResponse response = responseEntity.getBody();
        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(userResponse, response.getUser());

        verify(firebaseAuthService).authenticateWithFirebase("firebase-id-token");
        verify(tokenProvider).generateToken(1L);
        verify(tokenProvider).generateRefreshToken(1L);
        verify(jwtService).saveRefreshToken(1L, "refresh-token");
    }

    @Test
    @DisplayName("Should logout successfully")
    void shouldLogoutSuccessfully() {
        // Arrange
        String refreshToken = "refresh-token";

        when(authService.logout(refreshToken)).thenReturn(logoutResponse);

        // Act
        ResponseEntity<ApiResponse> responseEntity = socialAuthController.logout(refreshToken);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());
        assertEquals(logoutResponse, responseEntity.getBody());

        verify(authService).logout(refreshToken);
    }
}