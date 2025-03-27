package com.fileflow.service.auth;

import com.fileflow.dto.request.auth.PasswordResetRequest;
import com.fileflow.dto.request.auth.RefreshTokenRequest;
import com.fileflow.dto.request.auth.SignInRequest;
import com.fileflow.dto.request.auth.SignUpRequest;
import com.fileflow.dto.response.auth.JwtResponse;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.exception.BadRequestException;
import com.fileflow.exception.ResourceNotFoundException;
import com.fileflow.exception.UnauthorizedException;
import com.fileflow.model.User;
import com.fileflow.model.UserSettings;
import com.fileflow.repository.UserRepository;
import com.fileflow.repository.UserSettingsRepository;
import com.fileflow.security.JwtTokenProvider;
import com.fileflow.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private JwtService jwtService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private UserPrincipal userPrincipal;

    @BeforeEach
    public void setup() {
        // Setup test user
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .password("encodedPassword")
                .status(User.Status.ACTIVE)
                .role(User.UserRole.USER)
                .storageQuota(10737418240L) // 10GB
                .storageUsed(0L)
                .createdAt(LocalDateTime.now())
                .build();

        // Setup user principal - using builder pattern to match implementation class
        userPrincipal = UserPrincipal.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Test
    public void testSignInSuccess() {
        // Setup mocks
        SignInRequest signInRequest = SignInRequest.builder()
                .usernameOrEmail("testuser")
                .password("Test@123")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateToken(any(Authentication.class))).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(1L)).thenReturn("refresh-token");

        // Call service method
        JwtResponse response = authService.signIn(signInRequest);

        // Verify results
        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("testuser", response.getUser().getUsername());

        // Verify user's last login was updated
        verify(userRepository).save(any(User.class));
        verify(jwtService).saveRefreshToken(eq(1L), eq("refresh-token"));
    }

    @Test
    public void testSignUpSuccess() {
        // Setup mocks
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("NewPass@123")
                .firstName("New")
                .lastName("User")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("NewPass@123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Call service method
        ApiResponse response = authService.signUp(signUpRequest);

        // Verify results
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("User registered successfully", response.getMessage());

        // Verify user and settings were saved
        verify(userRepository).save(any(User.class));
        verify(userSettingsRepository).save(any(UserSettings.class));
    }

    @Test
    public void testSignUpFailureUsernameExists() {
        // Setup mocks
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("existinguser")
                .email("new@example.com")
                .password("NewPass@123")
                .build();

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // Call service method and verify exception
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authService.signUp(signUpRequest);
        });

        assertEquals("Username is already taken", exception.getMessage());

        // Verify no user was saved
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testSignUpFailureEmailExists() {
        // Setup mocks
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("newuser")
                .email("existing@example.com")
                .password("NewPass@123")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Call service method and verify exception
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authService.signUp(signUpRequest);
        });

        assertEquals("Email is already registered", exception.getMessage());

        // Verify no user was saved
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testRefreshTokenSuccess() {
        // Setup mocks
        RefreshTokenRequest refreshTokenRequest = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        when(tokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(tokenProvider.getUserIdFromJWT("valid-refresh-token")).thenReturn(1L);
        when(jwtService.validateRefreshToken(1L, "valid-refresh-token")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateToken(1L)).thenReturn("new-access-token");
        when(tokenProvider.generateRefreshToken(1L)).thenReturn("new-refresh-token");

        // Call service method
        JwtResponse response = authService.refreshToken(refreshTokenRequest);

        // Verify results
        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());

        // Verify refresh token was saved
        verify(jwtService).saveRefreshToken(eq(1L), eq("new-refresh-token"));
    }

    @Test
    public void testRefreshTokenFailureInvalidToken() {
        // Setup mocks
        RefreshTokenRequest refreshTokenRequest = RefreshTokenRequest.builder()
                .refreshToken("invalid-refresh-token")
                .build();

        when(tokenProvider.validateToken("invalid-refresh-token")).thenReturn(false);

        // Call service method and verify exception
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            authService.refreshToken(refreshTokenRequest);
        });

        assertEquals("Invalid refresh token", exception.getMessage());
    }

    @Test
    public void testForgotPasswordSuccess() {
        // Setup mocks
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Call service method
        ApiResponse response = authService.forgotPassword("test@example.com");

        // Verify results
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Password reset instructions sent to your email", response.getMessage());
    }

    @Test
    public void testForgotPasswordFailureUserNotFound() {
        // Setup mocks
        when(userRepository.findByEmail("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        // Call service method and verify exception
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            authService.forgotPassword("nonexistent@example.com");
        });

        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    public void testResetPasswordSuccess() {
        // Setup mocks
        PasswordResetRequest resetRequest = PasswordResetRequest.builder()
                .email("test@example.com")
                .newPassword("NewPass@123")
                .confirmPassword("NewPass@123")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("NewPass@123")).thenReturn("newEncodedPassword");

        // Call service method
        ApiResponse response = authService.resetPassword("valid-token", resetRequest);

        // Verify results
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Password has been reset successfully", response.getMessage());

        // Verify password was updated
        verify(userRepository).save(any(User.class));
    }

    @Test
    public void testValidateTokenSuccess() {
        // Setup mocks
        when(tokenProvider.validateToken("valid-token")).thenReturn(true);

        // Call service method
        ApiResponse response = authService.validateToken("valid-token");

        // Verify results
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Token is valid", response.getMessage());
    }

    @Test
    public void testValidateTokenFailure() {
        // Setup mocks
        when(tokenProvider.validateToken("invalid-token")).thenReturn(false);

        // Call service method and verify exception
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            authService.validateToken("invalid-token");
        });

        assertEquals("Invalid token", exception.getMessage());
    }
}