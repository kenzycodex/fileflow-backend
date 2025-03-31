package com.fileflow.service.auth;

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
import com.fileflow.model.UserSettings;
import com.fileflow.repository.UserRepository;
import com.fileflow.repository.UserSettingsRepository;
import com.fileflow.security.JwtTokenProvider;
import com.fileflow.security.UserPrincipal;
import com.fileflow.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final JwtService jwtService;

    @Override
    public JwtResponse signIn(SignInRequest signInRequest) {
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

        // Update last login time
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Generate tokens
        String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal.getId());

        // Save the refresh token
        jwtService.saveRefreshToken(userPrincipal.getId(), refreshToken);

        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(Constants.ACCESS_TOKEN_EXPIRATION / 1000) // Convert to seconds
                .user(mapUserToUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public ApiResponse signUp(SignUpRequest signUpRequest) {
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
                .enabled(true)
                .authProvider(User.AuthProvider.LOCAL)
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

        return ApiResponse.builder()
                .success(true)
                .message("User registered successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public JwtResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getRefreshToken();

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        Long userId = tokenProvider.getUserIdFromJWT(refreshToken);

        // Verify the token exists in our storage and is valid
        if (!jwtService.validateRefreshToken(userId, refreshToken)) {
            throw new UnauthorizedException("Refresh token is not valid");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Generate new tokens
        String newAccessToken = tokenProvider.generateToken(userId);
        String newRefreshToken = tokenProvider.generateRefreshToken(userId);

        // Update the refresh token in storage
        jwtService.saveRefreshToken(userId, newRefreshToken);

        return JwtResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(Constants.ACCESS_TOKEN_EXPIRATION / 1000) // Convert to seconds
                .user(mapUserToUserResponse(user))
                .build();
    }

    @Override
    public ApiResponse forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // Only allow password reset for local accounts
        if (user.getAuthProvider() != User.AuthProvider.LOCAL) {
            throw new BadRequestException("Password reset is not available for social login accounts");
        }

        // Generate password reset token
        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(24)); // Token valid for 24 hours
        userRepository.save(user);

        // Send email with reset link
        // emailService.sendPasswordResetEmail(user.getEmail(), token);

        return ApiResponse.builder()
                .success(true)
                .message("Password reset instructions sent to your email")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
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

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);

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

        // Update last login time
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

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
                .enabled(true)
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

        log.info("Created new user from Firebase authentication: {}", email);

        return mapUserToUserResponse(savedUser);
    }

    @Override
    public ApiResponse logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isEmpty()) {
            try {
                if (tokenProvider.validateToken(refreshToken)) {
                    Long userId = tokenProvider.getUserIdFromJWT(refreshToken);
                    jwtService.removeRefreshToken(userId);
                }
            } catch (Exception e) {
                log.warn("Error during logout for token: {}", e.getMessage());
                // Continue with logout even if token validation fails
            }
        }

        // Clear security context
        SecurityContextHolder.clearContext();

        return ApiResponse.builder()
                .success(true)
                .message("Logged out successfully")
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
                .authProvider(user.getAuthProvider().name())
                .build();
    }

    /**
     * Ensure username is unique by appending numbers if needed
     */
    private String ensureUniqueUsername(String baseUsername) {
        String username = baseUsername;
        int counter = 1;

        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
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
}