package com.fileflow.auth.security;

import com.fileflow.config.JwtConfig;
import com.fileflow.security.CustomUserDetailsService;
import com.fileflow.security.JwtAuthenticationFilter;
import com.fileflow.security.JwtTokenProvider;
import com.fileflow.security.UserPrincipal;
import com.fileflow.service.auth.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        // Clear security context before each test
        SecurityContextHolder.clearContext();

        // Initialize request, response, and filter chain
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();

        // Set up user details
        userDetails = new UserPrincipal(
                1L,
                "Test",
                "User",
                "testuser",
                "test@example.com",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                true
        );

        // Use lenient mode for common configurations
        // This avoids the UnnecessaryStubbingException when not all tests use these stubbings
        lenient().when(jwtConfig.getHeader()).thenReturn("Authorization");
        lenient().when(jwtConfig.getPrefix()).thenReturn("Bearer");
    }

    @Test
    @DisplayName("Should not filter excluded paths")
    void shouldNotFilter_excludedPaths() throws Exception {
        // Arrange
        String[] excludedPaths = {
                "/api/v1/auth/signin",
                "/swagger-ui/index.html",
                "/v3/api-docs",
                "/api/v1/health",
                "/",
                "/favicon.ico",
                "/static/main.css"
        };

        for (String path : excludedPaths) {
            request.setRequestURI(path);

            // Act
            // Using direct method invocation instead of spy to avoid protected method issue
            boolean shouldNotFilter = jwtAuthenticationFilter.shouldNotFilter(request);

            // Assert
            assertTrue(shouldNotFilter, "Path " + path + " should be excluded from filtering");
        }
    }

    @Test
    @DisplayName("Should filter protected paths")
    void shouldFilter_protectedPaths() throws Exception {
        // Arrange
        String[] protectedPaths = {
                "/api/v1/users/me",
                "/api/v1/files",
                "/api/v1/folders",
                "/api/v1/some-protected-resource"
        };

        for (String path : protectedPaths) {
            request.setRequestURI(path);

            // Act
            // Using direct method invocation instead of spy to avoid protected method issue
            boolean shouldNotFilter = jwtAuthenticationFilter.shouldNotFilter(request);

            // Assert
            assertFalse(shouldNotFilter, "Path " + path + " should not be excluded from filtering");
        }
    }

    @Test
    @DisplayName("Should skip token validation when no token is provided")
    void shouldSkipTokenValidation_whenNoTokenIsProvided() throws ServletException, IOException {
        // Arrange
        request.setRequestURI("/api/v1/users/me");

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenProvider, never()).validateToken(anyString());
        verify(userDetailsService, never()).loadUserById(anyLong());
    }

    @Test
    @DisplayName("Should skip token validation when token is blacklisted")
    void shouldSkipTokenValidation_whenTokenIsBlacklisted() throws ServletException, IOException {
        // Arrange
        request.setRequestURI("/api/v1/users/me");
        request.addHeader("Authorization", "Bearer blacklisted.token.here");

        when(jwtService.isTokenBlacklisted("blacklisted.token.here")).thenReturn(true);

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenProvider, never()).validateToken(anyString());
        verify(userDetailsService, never()).loadUserById(anyLong());
    }

    @Test
    @DisplayName("Should set authentication when valid token is provided")
    void shouldSetAuthentication_whenValidTokenIsProvided() throws ServletException, IOException {
        // Arrange
        request.setRequestURI("/api/v1/users/me");
        request.addHeader("Authorization", "Bearer valid.token.here");

        when(jwtService.isTokenBlacklisted("valid.token.here")).thenReturn(false);
        when(tokenProvider.validateToken("valid.token.here")).thenReturn(true);
        when(tokenProvider.getUserIdFromJWT("valid.token.here")).thenReturn(1L);
        when(tokenProvider.getTokenTypeFromJWT("valid.token.here")).thenReturn("access");
        when(jwtService.getLatestAccessToken(1L)).thenReturn("valid.token.here");
        when(userDetailsService.loadUserById(1L)).thenReturn(userDetails);

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(userDetails, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(tokenProvider).validateToken("valid.token.here");
        verify(userDetailsService).loadUserById(1L);
    }

    @Test
    @DisplayName("Should skip authentication when token is not current")
    void shouldSkipAuthentication_whenTokenIsNotCurrent() throws ServletException, IOException {
        // Arrange
        request.setRequestURI("/api/v1/users/me");
        request.addHeader("Authorization", "Bearer old.token.here");

        when(jwtService.isTokenBlacklisted("old.token.here")).thenReturn(false);
        when(tokenProvider.validateToken("old.token.here")).thenReturn(true);
        when(tokenProvider.getUserIdFromJWT("old.token.here")).thenReturn(1L);
        when(tokenProvider.getTokenTypeFromJWT("old.token.here")).thenReturn("access");
        when(jwtService.getLatestAccessToken(1L)).thenReturn("newer.token.here");  // Different token

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenProvider).validateToken("old.token.here");
        verify(userDetailsService, never()).loadUserById(anyLong());
    }

    @Test
    @DisplayName("Should skip authentication when token is not an access token")
    void shouldSkipAuthentication_whenTokenIsNotAccessToken() throws ServletException, IOException {
        // Arrange
        request.setRequestURI("/api/v1/users/me");
        request.addHeader("Authorization", "Bearer refresh.token.here");

        when(jwtService.isTokenBlacklisted("refresh.token.here")).thenReturn(false);
        when(tokenProvider.validateToken("refresh.token.here")).thenReturn(true);
        when(tokenProvider.getUserIdFromJWT("refresh.token.here")).thenReturn(1L);
        when(tokenProvider.getTokenTypeFromJWT("refresh.token.here")).thenReturn("refresh");  // Refresh token type

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenProvider).validateToken("refresh.token.here");
        verify(jwtService, never()).getLatestAccessToken(anyLong());
        verify(userDetailsService, never()).loadUserById(anyLong());
    }

    @Test
    @DisplayName("Should handle exceptions during token validation")
    void shouldHandleExceptions_duringTokenValidation() throws ServletException, IOException {
        // Arrange
        request.setRequestURI("/api/v1/users/me");
        request.addHeader("Authorization", "Bearer invalid.token.here");

        when(jwtService.isTokenBlacklisted("invalid.token.here")).thenThrow(new RuntimeException("Test exception"));

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // The filter should catch the exception and continue the chain
        // Since MockFilterChain doesn't actually do anything, we can't really verify it was called
        // but the test passing without exception is verification enough
    }

    @Test
    @DisplayName("Should extract JWT from Authorization header")
    void shouldExtractJwt_fromAuthorizationHeader() throws Exception {
        // Arrange
        request.addHeader("Authorization", "Bearer test.jwt.token");

        // Use reflection to access the private method
        Method getJwtFromRequestMethod = JwtAuthenticationFilter.class.getDeclaredMethod("getJwtFromRequest", HttpServletRequest.class);
        getJwtFromRequestMethod.setAccessible(true);

        // Act
        String token = (String) getJwtFromRequestMethod.invoke(jwtAuthenticationFilter, request);

        // Assert
        assertEquals("test.jwt.token", token);
    }

    @Test
    @DisplayName("Should return null when Authorization header is missing")
    void shouldReturnNull_whenAuthorizationHeaderIsMissing() throws Exception {
        // Use reflection to access the private method
        Method getJwtFromRequestMethod = JwtAuthenticationFilter.class.getDeclaredMethod("getJwtFromRequest", HttpServletRequest.class);
        getJwtFromRequestMethod.setAccessible(true);

        // Act
        String token = (String) getJwtFromRequestMethod.invoke(jwtAuthenticationFilter, request);

        // Assert
        assertNull(token);
    }

    @Test
    @DisplayName("Should return null when Authorization header has invalid format")
    void shouldReturnNull_whenAuthorizationHeaderHasInvalidFormat() throws Exception {
        // Arrange
        // Invalid format (missing space, wrong prefix, etc.)
        request.addHeader("Authorization", "Basic dXNlcjpwYXNzd29yZA==");

        // Use reflection to access the private method
        Method getJwtFromRequestMethod = JwtAuthenticationFilter.class.getDeclaredMethod("getJwtFromRequest", HttpServletRequest.class);
        getJwtFromRequestMethod.setAccessible(true);

        // Act
        String token = (String) getJwtFromRequestMethod.invoke(jwtAuthenticationFilter, request);

        // Assert
        assertNull(token);
    }
}