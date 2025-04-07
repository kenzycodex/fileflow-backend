package com.fileflow.service.auth;

import com.fileflow.config.AppConfig;
import com.fileflow.config.TestConfig;
import com.fileflow.config.TestValidationConfig;
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
import com.fileflow.service.email.EmailService;
import com.fileflow.service.security.RateLimiterService;
import com.fileflow.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Import({TestConfig.class, TestValidationConfig.class})
@ActiveProfiles("test")
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
    private EmailService emailService;

    @Mock
    private Authentication authentication;

    @Mock
    private AppConfig appConfig;

    @Mock
    private AppConfig.SecurityConfig securityConfig;

    @Mock
    private AppConfig.PasswordStrength passwordStrength;

    @Mock
    private AppConfig.EmailConfig emailConfig;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private HttpServletRequest request;

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
                .authProvider(User.AuthProvider.LOCAL)
                .storageQuota(10737418240L) // 10GB
                .storageUsed(0L)
                .createdAt(LocalDateTime.now())
                .build();

        // Setup user principal
        userPrincipal = UserPrincipal.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .isActive(true)
                .build();

        // Configure AppConfig for tests with lenient mode
        lenient().when(appConfig.getSecurity()).thenReturn(securityConfig);
        lenient().when(securityConfig.getPasswordStrength()).thenReturn(passwordStrength);
        lenient().when(securityConfig.getAccessTokenExpiration()).thenReturn(3600000L);
        lenient().when(securityConfig.getRefreshTokenExpiration()).thenReturn(604800000L);

        lenient().when(passwordStrength.getMinLength()).thenReturn(8);
        lenient().when(passwordStrength.isRequireDigits()).thenReturn(true);
        lenient().when(passwordStrength.isRequireLowercase()).thenReturn(true);
        lenient().when(passwordStrength.isRequireUppercase()).thenReturn(true);
        lenient().when(passwordStrength.isRequireSpecial()).thenReturn(true);

        lenient().when(appConfig.getEmail()).thenReturn(emailConfig);
        lenient().when(emailConfig.getEmailVerificationExpiryHours()).thenReturn(24L);
        lenient().when(emailConfig.getPasswordResetExpiryHours()).thenReturn(24L);

        // Configure mock behavior for SecurityUtils
        lenient().when(securityUtils.getClientIpAddress(any(HttpServletRequest.class))).thenReturn("127.0.0.1");
        lenient().when(securityUtils.getUserAgent(any(HttpServletRequest.class))).thenReturn("Test User Agent");

        // Configure RateLimiterService to avoid NullPointerExceptions
        lenient().doNothing().when(rateLimiterService).checkLoginRateLimit(anyString());
        lenient().doNothing().when(rateLimiterService).checkIpRateLimit(anyString());
        lenient().doNothing().when(rateLimiterService).checkSignupRateLimit(anyString());
        lenient().doNothing().when(rateLimiterService).checkPasswordResetRateLimit(anyString());
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
        assertEquals("LOCAL", response.getUser().getAuthProvider()); // Verify auth provider

        // Verify user's last login was updated
        verify(userRepository).save(any(User.class));
        verify(jwtService).saveRefreshToken(eq(1L), eq("refresh-token"));
    }

    @Test
    public void testSignUpSuccess() {
        // Setup mocks with argument matchers
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("NewPass@123")
                .confirmPassword("NewPass@123")
                .firstName("New")
                .lastName("User")
                .build();

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Call service method
        ApiResponse response = authService.signUp(signUpRequest);

        // Verify results
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("User registered successfully"));

        // Verify user and settings were saved
        verify(userRepository).save(any(User.class));
        verify(userSettingsRepository).save(any(UserSettings.class));
    }

    @Test
    public void testSignUpFailureUsernameExists() {
        // Setup mocks with argument matchers
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("existinguser")
                .email("new@example.com")
                .password("NewPass@123")
                .confirmPassword("NewPass@123")
                .build();

        when(userRepository.existsByUsername(anyString())).thenReturn(true);

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
        // Setup mocks with argument matchers
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("newuser")
                .email("existing@example.com")
                .password("NewPass@123")
                .confirmPassword("NewPass@123")
                .build();

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

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
        when(tokenProvider.getTokenFamilyFromJWT("valid-refresh-token")).thenReturn("token-family");
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
        assertEquals("LOCAL", response.getUser().getAuthProvider()); // Verify auth provider

        // Verify refresh token was rotated
        verify(jwtService).rotateRefreshToken(eq(1L), eq("valid-refresh-token"), eq("new-refresh-token"), eq("token-family"));
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
        User localUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .authProvider(User.AuthProvider.LOCAL)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(localUser));

        // Call service method
        ApiResponse response = authService.forgotPassword("test@example.com");

        // Verify results
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Password reset instructions sent to your email", response.getMessage());

        // Verify token was set and user was saved
        verify(userRepository).save(any(User.class));
        verify(emailService).sendPasswordResetEmail(any(User.class), anyString());
    }

    @Test
    public void testForgotPasswordFailureUserNotFound() {
        // Setup mocks
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Call service method and verify exception
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            authService.forgotPassword("nonexistent@example.com");
        });

        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    public void testForgotPasswordFailureSocialLogin() {
        // Setup mocks with a social login user
        User socialUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .authProvider(User.AuthProvider.GOOGLE) // Social login provider
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(socialUser));

        // Call service method and verify exception
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authService.forgotPassword("test@example.com");
        });

        assertEquals("Password reset is not available for social login accounts", exception.getMessage());
    }

    @Test
    public void testResetPasswordSuccess() {
        // Setup mocks
        PasswordResetRequest resetRequest = PasswordResetRequest.builder()
                .email("test@example.com")
                .newPassword("NewPass@123")
                .confirmPassword("NewPass@123")
                .token("valid-token")
                .build();

        // Set reset token on test user
        testUser.setResetPasswordToken("valid-token");
        testUser.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1)); // Not expired

        when(userRepository.findByResetPasswordToken("valid-token")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("NewPass@123")).thenReturn("newEncodedPassword");

        // Call service method
        ApiResponse response = authService.resetPassword("valid-token", resetRequest);

        // Verify results
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Password has been reset successfully", response.getMessage());

        // Verify password was updated and token cleared
        verify(userRepository).save(any(User.class));
        assertNull(testUser.getResetPasswordToken());
        assertNull(testUser.getResetPasswordTokenExpiry());
    }

    @Test
    public void testResetPasswordFailureTokenExpired() {
        // Setup mocks
        PasswordResetRequest resetRequest = PasswordResetRequest.builder()
                .email("test@example.com")
                .newPassword("NewPass@123")
                .confirmPassword("NewPass@123")
                .token("expired-token")
                .build();

        // Set expired token on test user
        testUser.setResetPasswordToken("expired-token");
        testUser.setResetPasswordTokenExpiry(LocalDateTime.now().minusHours(1)); // Expired

        when(userRepository.findByResetPasswordToken("expired-token")).thenReturn(Optional.of(testUser));

        // Call service method and verify exception
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authService.resetPassword("expired-token", resetRequest);
        });

        assertEquals("Token has expired", exception.getMessage());
    }

    @Test
    public void testResetPasswordFailurePasswordsMismatch() {
        // Setup mocks
        PasswordResetRequest resetRequest = PasswordResetRequest.builder()
                .email("test@example.com")
                .newPassword("NewPass@123")
                .confirmPassword("DifferentPass@123") // Mismatch
                .token("valid-token")
                .build();

        // Set token on test user
        testUser.setResetPasswordToken("valid-token");
        testUser.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1)); // Not expired

        when(userRepository.findByResetPasswordToken("valid-token")).thenReturn(Optional.of(testUser));

        // Call service method and verify exception
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authService.resetPassword("valid-token", resetRequest);
        });

        assertEquals("Passwords do not match", exception.getMessage());
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