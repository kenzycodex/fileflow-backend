package com.fileflow.service.auth;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceLogoutTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private JwtService jwtService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        // Set up the security context for all tests
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testLogoutWithValidToken() {
        // Setup
        String validRefreshToken = "valid-refresh-token";
        Long userId = 1L;

        // Mock token validation
        when(tokenProvider.validateToken(validRefreshToken)).thenReturn(true);
        when(tokenProvider.getUserIdFromJWT(validRefreshToken)).thenReturn(userId);

        // Execute logout
        ApiResponse response = authService.logout(validRefreshToken);

        // Verify
        assertTrue(response.isSuccess());
        assertEquals("Logged out successfully", response.getMessage());

        // Verify the refresh token was removed
        verify(jwtService).removeRefreshToken(userId);
    }

    @Test
    void testLogoutWithInvalidToken() {
        // Setup
        String invalidRefreshToken = "invalid-refresh-token";

        // Mock token validation to fail
        when(tokenProvider.validateToken(invalidRefreshToken)).thenReturn(false);

        // Execute logout
        ApiResponse response = authService.logout(invalidRefreshToken);

        // Verify
        assertTrue(response.isSuccess());
        assertEquals("Logged out successfully", response.getMessage());

        // Verify no refresh token was removed since the token was invalid
        verify(jwtService, never()).removeRefreshToken(any());
    }

    @Test
    void testLogoutWithNullToken() {
        // Execute logout with null token
        ApiResponse response = authService.logout(null);

        // Verify
        assertTrue(response.isSuccess());
        assertEquals("Logged out successfully", response.getMessage());

        // Verify no refresh token was removed
        verify(jwtService, never()).removeRefreshToken(any());
    }

    @Test
    void testLogoutWithEmptyToken() {
        // Execute logout with empty token
        ApiResponse response = authService.logout("");

        // Verify
        assertTrue(response.isSuccess());
        assertEquals("Logged out successfully", response.getMessage());

        // Verify no refresh token was removed
        verify(jwtService, never()).removeRefreshToken(any());
    }

    @Test
    void testLogoutWithTokenValidationException() {
        // Setup
        String refreshToken = "exception-token";

        // Mock token validation to throw exception
        when(tokenProvider.validateToken(refreshToken)).thenThrow(new RuntimeException("Test exception"));

        // Execute logout
        ApiResponse response = authService.logout(refreshToken);

        // Verify that logout still completes successfully despite the exception
        assertTrue(response.isSuccess());
        assertEquals("Logged out successfully", response.getMessage());

        // Verify no refresh token was removed due to the exception
        verify(jwtService, never()).removeRefreshToken(any());
    }
}