package com.fileflow.service.auth;

import com.fileflow.config.AppConfig;
import com.fileflow.dto.request.auth.PasswordResetRequest;
import com.fileflow.dto.request.auth.RefreshTokenRequest;
import com.fileflow.dto.request.auth.SignInRequest;
import com.fileflow.dto.request.auth.SignUpRequest;
import com.fileflow.dto.response.auth.JwtResponse;
import com.fileflow.dto.response.auth.UserResponse;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.exception.BadRequestException;
import com.fileflow.exception.ResourceNotFoundException;
import com.fileflow.exception.UnauthorizedException;
import com.fileflow.model.User;
import com.fileflow.model.UserMfa;
import com.fileflow.model.UserSettings;
import com.fileflow.repository.UserMfaRepository;
import com.fileflow.repository.UserRepository;
import com.fileflow.repository.UserSettingsRepository;
import com.fileflow.security.JwtTokenProvider;
import com.fileflow.security.UserPrincipal;
import com.fileflow.service.email.EmailService;
import com.fileflow.service.security.RateLimiterService;
import com.fileflow.util.Constants;
import com.fileflow.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final UserMfaRepository userMfaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final SecurityUtils securityUtils;
    private final RateLimiterService rateLimiterService;
    private final AppConfig appConfig;
    private final HttpServletRequest request;

    // Add this field to hold email verification requirement
    private boolean requireEmailVerification = true;

    @Override
    @Transactional
    public JwtResponse signIn(SignInRequest signInRequest) {
        String username = signInRequest.getUsernameOrEmail();

        try {
            // Apply rate limiting
            rateLimiterService.checkLoginRateLimit(username);
            String ipAddress = securityUtils.getClientIpAddress(request);
            rateLimiterService.checkIpRateLimit(ipAddress);

            // Check if account is locked
            if (jwtService.isUserLocked(username)) {
                Long remainingTime = jwtService.getLockoutTimeRemaining(username);
                throw new LockedException("Account is temporarily locked. Try again in " + remainingTime + " seconds.");
            }

            // Perform authentication
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            signInRequest.getUsernameOrEmail(),
                            signInRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));

            // Clear any failed login attempts
            jwtService.clearFailedLogins(username);
            rateLimiterService.resetLimit(username);

            // Check for email verification if required
            if (!user.isEmailVerified() && requireEmailVerification) {
                throw new BadRequestException("Email not verified. Please check your email and verify your account.");
            }

            // Update last login time and IP
            user.setLastLogin(LocalDateTime.now());
            user.setLastLoginIp(ipAddress);
            userRepository.save(user);

            // Track login for security
            String userAgent = securityUtils.getUserAgent(request);
            securityUtils.trackSuccessfulLogin(user, ipAddress, userAgent);

            // Check for suspicious activity
            if (appConfig.getSecurity().isDetectUnusualActivity()) {
                boolean suspicious = securityUtils.checkSuspiciousActivity(user, ipAddress, userAgent);
                if (suspicious) {
                    log.warn("Suspicious login detected for user ID: {}", user.getId());
                    // We still allow login but have alerted the user
                }
            }

            // Generate tokens
            String accessToken = tokenProvider.generateToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(userPrincipal.getId());

            // Save the tokens
            jwtService.saveRefreshToken(userPrincipal.getId(), refreshToken);

            // Check if MFA is required for this user
            boolean mfaRequired = false;
            if (appConfig.getMfa().isEnabled()) {
                mfaRequired = userMfaRepository.findByUserId(user.getId())
                        .map(UserMfa::isEnabled)
                        .orElse(false);
            }

            return JwtResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(appConfig.getSecurity().getAccessTokenExpiration() / 1000) // Convert to seconds
                    .user(mapUserToUserResponse(user))
                    .mfaRequired(mfaRequired)
                    .build();

        } catch (BadCredentialsException e) {
            // Record failed login and check for lockout
            boolean locked = jwtService.recordFailedLogin(username);
            if (locked) {
                // Find user by username or email to send notification
                userRepository.findByUsernameOrEmail(username, username).ifPresent(user -> {
                    emailService.sendAccountLockedEmail(user, appConfig.getSecurity().getLockoutDurationMinutes());
                });

                log.warn("Account locked due to too many failed attempts: {}", username);
                throw new LockedException("Account locked due to too many failed attempts. Try again later.");
            }

            log.warn("Failed login attempt for username: {}", username);
            throw e;
        }
    }

    @Override
    @Transactional
    public ApiResponse signUp(SignUpRequest signUpRequest) {
        // Apply rate limiting to prevent enumeration attacks
        String ipAddress = securityUtils.getClientIpAddress(request);
        rateLimiterService.checkSignupRateLimit(ipAddress);

        // Validate request
        validateSignUpRequest(signUpRequest);

        // Check if username is already taken
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }

        // Check if email is already registered
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        // Create new user
        User user = User.builder()
                .firstName(signUpRequest.getFirstName())
                .lastName(signUpRequest.getLastName())
                .username(signUpRequest.getUsername())
                .email(signUpRequest.getEmail())
                .password(passwordEncoder.encode(signUpRequest.getPassword()))
                .status(User.Status.ACTIVE)
                .role(User.UserRole.USER)
                .storageQuota(Constants.DEFAULT_STORAGE_QUOTA)
                .storageUsed(0L)
                .createdAt(LocalDateTime.now())
                .registrationIp(ipAddress)
                .enabled(true)
                .emailVerified(false) // Require email verification
                .authProvider(User.AuthProvider.LOCAL)
                .build();

        // Generate email verification token
        if (requireEmailVerification) {
            String verificationToken = UUID.randomUUID().toString();
            user.setEmailVerificationToken(verificationToken);
            user.setEmailVerificationTokenExpiry(
                    LocalDateTime.now().plusHours(appConfig.getEmail().getEmailVerificationExpiryHours()));
        } else {
            user.setEmailVerified(true);
        }

        User savedUser = userRepository.save(user);

        // Create default user settings
        UserSettings userSettings = UserSettings.builder()
                .user(savedUser)
                .themePreference(UserSettings.ThemePreference.LIGHT)
                .notificationEmail(true)
                .notificationInApp(true)
                .defaultView(UserSettings.DefaultView.GRID)
                .build();

        userSettingsRepository.save(userSettings);

        // Create root folder for user
        createRootFolder(savedUser);

        // Send verification email if required
        if (requireEmailVerification) {
            emailService.sendWelcomeEmail(savedUser, savedUser.getEmailVerificationToken());
        }

        return ApiResponse.builder()
                .success(true)
                .message(requireEmailVerification
                        ? "User registered successfully. Please verify your email."
                        : "User registered successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public JwtResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getRefreshToken();

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        Long userId = tokenProvider.getUserIdFromJWT(refreshToken);
        String tokenFamily = tokenProvider.getTokenFamilyFromJWT(refreshToken);

        // Verify the token exists in our storage and is valid
        if (!jwtService.validateRefreshToken(userId, refreshToken)) {
            // Possible token reuse attack - revoke all tokens
            jwtService.revokeAllUserTokens(userId);
            throw new UnauthorizedException("Invalid refresh token. All sessions have been terminated.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Generate new tokens
        String newAccessToken = tokenProvider.generateToken(userId);
        String newRefreshToken = tokenProvider.generateRefreshToken(userId);

        // Rotate the refresh token (invalidate old one, save new one)
        jwtService.rotateRefreshToken(userId, refreshToken, newRefreshToken, tokenFamily);

        return JwtResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(appConfig.getSecurity().getAccessTokenExpiration() / 1000) // Convert to seconds
                .user(mapUserToUserResponse(user))
                .build();
    }

    @Override
    public ApiResponse forgotPassword(String email) {
        // Apply rate limiting
        rateLimiterService.checkPasswordResetRateLimit(email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // Only allow password reset for local accounts
        if (user.getAuthProvider() != User.AuthProvider.LOCAL) {
            throw new BadRequestException("Password reset is not available for social login accounts");
        }

        // Generate password reset token
        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(
                LocalDateTime.now().plusHours(appConfig.getEmail().getPasswordResetExpiryHours()));
        userRepository.save(user);

        // Send email with reset link
        emailService.sendPasswordResetEmail(user, token);

        return ApiResponse.builder()
                .success(true)
                .message("Password reset instructions sent to your email")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public ApiResponse resetPassword(String token, PasswordResetRequest request) {
        // Find user by reset password token
        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired token"));

        // Check if token is expired
        if (user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token has expired");
        }

        // Check if passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        // Validate password strength
        validatePasswordStrength(request.getNewPassword());

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        user.setPasswordUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Revoke all existing tokens for security
        jwtService.revokeAllUserTokens(user.getId());

        return ApiResponse.builder()
                .success(true)
                .message("Password has been reset successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public ApiResponse validateToken(String token) {
        boolean isValid = tokenProvider.validateToken(token);

        if (!isValid) {
            throw new UnauthorizedException("Invalid token");
        }

        return ApiResponse.builder()
                .success(true)
                .message("Token is valid")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional
    public void updateFirebaseUid(Long userId, String firebaseUid, String provider) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setFirebaseUid(firebaseUid);
        user.setAuthProvider(mapProviderFromString(provider));
        userRepository.save(user);

        log.info("Updated Firebase UID for user ID: {}", userId);
    }

    @Override
    @Transactional
    public UserResponse authenticateFirebaseUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Update last login time and IP
        user.setLastLogin(LocalDateTime.now());
        user.setLastLoginIp(securityUtils.getClientIpAddress(request));
        userRepository.save(user);

        // Track login for security
        String userAgent = securityUtils.getUserAgent(request);
        securityUtils.trackSuccessfulLogin(user, user.getLastLoginIp(), userAgent);

        // Return user details
        return mapUserToUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse createFirebaseUser(String firebaseUid, String email, String username,
                                           String firstName, String lastName, String profileImageUrl,
                                           String provider) {
        // Ensure username is unique
        String uniqueUsername = ensureUniqueUsername(username);
        String ipAddress = securityUtils.getClientIpAddress(request);

        // Create the user
        User user = User.builder()
                .firebaseUid(firebaseUid)
                .email(email)
                .username(uniqueUsername)
                .firstName(firstName)
                .lastName(lastName)
                .profileImagePath(profileImageUrl)
                .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Random password
                .status(User.Status.ACTIVE)
                .role(User.UserRole.USER)
                .storageQuota(Constants.DEFAULT_STORAGE_QUOTA)
                .storageUsed(0L)
                .createdAt(LocalDateTime.now())
                .lastLogin(LocalDateTime.now())
                .registrationIp(ipAddress)
                .lastLoginIp(ipAddress)
                .enabled(true)
                .emailVerified(true) // Social logins have verified emails
                .authProvider(mapProviderFromString(provider))
                .build();

        User savedUser = userRepository.save(user);

        // Create default user settings
        UserSettings userSettings = UserSettings.builder()
                .user(savedUser)
                .themePreference(UserSettings.ThemePreference.LIGHT)
                .notificationEmail(true)
                .notificationInApp(true)
                .defaultView(UserSettings.DefaultView.GRID)
                .build();

        userSettingsRepository.save(userSettings);

        // Create root folder for user
        createRootFolder(savedUser);

        // Track login
        String userAgent = securityUtils.getUserAgent(request);
        securityUtils.trackSuccessfulLogin(savedUser, ipAddress, userAgent);

        log.info("Created new user from Firebase authentication: {}", email);

        return mapUserToUserResponse(savedUser);
    }

    @Override
    public ApiResponse logout(String refreshToken) {
        Long userId = null;

        if (refreshToken != null && !refreshToken.isEmpty()) {
            try {
                if (tokenProvider.validateToken(refreshToken)) {
                    userId = tokenProvider.getUserIdFromJWT(refreshToken);

                    // Blacklist the refresh token
                    String accessToken = jwtService.getLatestAccessToken(userId);
                    if (accessToken != null) {
                        jwtService.blacklistToken(accessToken, appConfig.getSecurity().getAccessTokenExpiration());
                    }

                    jwtService.blacklistToken(refreshToken, appConfig.getSecurity().getRefreshTokenExpiration());
                    jwtService.removeRefreshToken(userId);
                }
            } catch (Exception e) {
                log.warn("Error during logout for token: {}", e.getMessage());
                // Continue with logout even if token validation fails
            }
        }

        // Clear security context
        SecurityContextHolder.clearContext();

        // Log the event if we have a user ID
        if (userId != null) {
            log.info("User logged out: {}", userId);
        }

        return ApiResponse.builder()
                .success(true)
                .message("Logged out successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public ApiResponse verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));

        // Check if token is expired
        if (user.getEmailVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Verification token has expired. Please request a new one.");
        }

        // Mark email as verified
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiry(null);
        userRepository.save(user);

        return ApiResponse.builder()
                .success(true)
                .message("Email verified successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public ApiResponse resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // Check if email is already verified
        if (user.isEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }

        // Generate new verification token
        String verificationToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationTokenExpiry(
                LocalDateTime.now().plusHours(appConfig.getEmail().getEmailVerificationExpiryHours()));
        userRepository.save(user);

        // Send verification email
        emailService.sendWelcomeEmail(user, verificationToken);

        return ApiResponse.builder()
                .success(true)
                .message("Verification email sent successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private void createRootFolder(User user) {
        // This will be implemented in the FolderService
        // For now, we'll skip it as it depends on FolderService implementation
    }

    private UserResponse mapUserToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .username(user.getUsername())
                .email(user.getEmail())
                .profileImagePath(user.getProfileImagePath())
                .createdAt(user.getCreatedAt())
                .storageQuota(user.getStorageQuota())
                .storageUsed(user.getStorageUsed())
                .role(user.getRole().name())
                .emailVerified(user.isEmailVerified())
                .authProvider(user.getAuthProvider().name())
                .build();
    }

    /**
     * Ensure username is unique by appending numbers if needed
     * Uses a transaction with proper exception handling to avoid race conditions
     */
    @Transactional
    private String ensureUniqueUsername(String baseUsername) {
        String username = baseUsername;
        int counter = 1;
        int maxAttempts = 10;

        // The User entity already has a unique constraint on username
        // so we just need to handle the potential exception
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                if (userRepository.existsByUsername(username)) {
                    username = baseUsername + counter;
                    counter++;
                } else {
                    // Username is available
                    return username;
                }
            } catch (Exception e) {
                // Another transaction might have grabbed this username
                // Try with the next counter value
                username = baseUsername + counter;
                counter++;
            }
        }

        // If we reach here, we've tried multiple times without success
        // Use a timestamp-based approach as fallback
        return baseUsername + "_" + System.currentTimeMillis() % 10000;
    }

    /**
     * Map provider string to AuthProvider enum
     */
    private User.AuthProvider mapProviderFromString(String provider) {
        if (provider == null) {
            return User.AuthProvider.LOCAL;
        }

        if (provider.contains("google")) {
            return User.AuthProvider.GOOGLE;
        } else if (provider.contains("github")) {
            return User.AuthProvider.GITHUB;
        } else if (provider.contains("microsoft")) {
            return User.AuthProvider.MICROSOFT;
        } else if (provider.contains("apple")) {
            return User.AuthProvider.APPLE;
        }

        return User.AuthProvider.LOCAL;
    }

    /**
     * Validate password strength based on configuration
     */
    private void validatePasswordStrength(String password) {
        if (password == null) {
            throw new BadRequestException("Password cannot be null");
        }

        AppConfig.PasswordStrength config = appConfig.getSecurity().getPasswordStrength();

        // Check length
        if (password.length() < config.getMinLength()) {
            throw new BadRequestException("Password must be at least " + config.getMinLength() + " characters long");
        }

        // Check for digits
        if (config.isRequireDigits() && !password.matches(".*\\d.*")) {
            throw new BadRequestException("Password must contain at least one digit");
        }

        // Check for lowercase letters
        if (config.isRequireLowercase() && !password.matches(".*[a-z].*")) {
            throw new BadRequestException("Password must contain at least one lowercase letter");
        }

        // Check for uppercase letters
        if (config.isRequireUppercase() && !password.matches(".*[A-Z].*")) {
            throw new BadRequestException("Password must contain at least one uppercase letter");
        }

        // Check for special characters
        if (config.isRequireSpecial() && !password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new BadRequestException("Password must contain at least one special character");
        }
    }

    /**
     * Validate signup request
     */
    private void validateSignUpRequest(SignUpRequest request) {
        // Check if passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        // Validate password strength
        validatePasswordStrength(request.getPassword());

        // Sanitize inputs to prevent XSS
        request.setFirstName(securityUtils.sanitizeInput(request.getFirstName()));
        request.setLastName(securityUtils.sanitizeInput(request.getLastName()));
        request.setUsername(securityUtils.sanitizeInput(request.getUsername()));
    }
}