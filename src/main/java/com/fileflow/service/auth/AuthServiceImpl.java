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
                .expiresIn(3600L)
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
                .role(User.Role.USER)
                .storageQuota(1024L * 1024L * 1024L * 10L) // 10GB default
                .storageUsed(0L)
                .createdAt(LocalDateTime.now())
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
                .expiresIn(3600L)
                .user(mapUserToUserResponse(user))
                .build();
    }

    @Override
    public ApiResponse forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // Generate password reset token
        String token = UUID.randomUUID().toString();

        // Save token to database or cache with expiration
        // passwordResetTokenRepository.save(new PasswordResetToken(token, user));

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
        // Validate token and user
        // User user = passwordResetTokenService.validateToken(token);

        // For now, stub implementation
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Delete token after use
        // passwordResetTokenRepository.deleteByToken(token);

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
                .build();
    }
}