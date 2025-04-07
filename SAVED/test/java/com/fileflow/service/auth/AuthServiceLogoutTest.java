package com.fileflow.service.auth;

import com.fileflow.config.AppConfig;
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
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the logout functionality in AuthServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AuthServiceLogoutTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private JwtService jwtService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private AppConfig appConfig;

    @Mock
    private AppConfig.SecurityConfig securityConfig;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        // Set up the security context for all tests
        SecurityContextHolder.setContext(securityContext);

        // Configure AppConfig with lenient mode to avoid unnecessary stubbing exceptions
        lenient().when(appConfig.getSecurity()).thenReturn(securityConfig);
        lenient().when(securityConfig.getAccessTokenExpiration()).thenReturn(3600000L);
        lenient().when(securityConfig.getRefreshTokenExpiration()).thenReturn(604800000L);

        // Configure JwtService mock behavior with lenient mode
        lenient().when(jwtService.getLatestAccessToken(anyLong())).thenReturn("access-token");
        lenient().doNothing().when(jwtService).blacklistToken(anyString(), anyLong());
        lenient().doNothing().when(jwtService).removeRefreshToken(anyLong());
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
        assertNotNull(response.getTimestamp());

        // Verify the refresh token was removed and tokens were blacklisted
        verify(jwtService).getLatestAccessToken(userId);
        verify(jwtService, times(2)).blacklistToken(anyString(), anyLong());
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
        verify(jwtService, never()).blacklistToken(anyString(), anyLong());
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
        verify(jwtService, never()).blacklistToken(anyString(), anyLong());
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
        verify(jwtService, never()).blacklistToken(anyString(), anyLong());
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
        verify(jwtService, never()).blacklistToken(anyString(), anyLong());
    }
}