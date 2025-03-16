package com.fileflow.service.user;

import com.fileflow.dto.request.user.PasswordChangeRequest;
import com.fileflow.dto.request.user.UserUpdateRequest;
import com.fileflow.dto.response.auth.UserResponse;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.exception.BadRequestException;
import com.fileflow.exception.ResourceNotFoundException;
import com.fileflow.exception.UnauthorizedException;
import com.fileflow.model.User;
import com.fileflow.repository.UserRepository;
import com.fileflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse getCurrentUser() {
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));

        return mapUserToUserResponse(user);
    }

    @Override
    public UserResponse getUserById(Long id) {
        UserPrincipal currentUser = getCurrentUserPrincipal();

        // Only allow users to access their own info or admins to access any user
        if (!currentUser.getId().equals(id) &&
                !currentUser.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new UnauthorizedException("You are not authorized to access this user information");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        return mapUserToUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UserUpdateRequest updateRequest) {
        UserPrincipal currentUser = getCurrentUserPrincipal();
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        // Update firstname if provided
        if (updateRequest.getFirstName() != null && !updateRequest.getFirstName().isEmpty()) {
            user.setFirstName(updateRequest.getFirstName());
        }

        // Update lastname if provided
        if (updateRequest.getLastName() != null && !updateRequest.getLastName().isEmpty()) {
            user.setLastName(updateRequest.getLastName());
        }

        // Update username if provided
        if (updateRequest.getUsername() != null && !updateRequest.getUsername().isEmpty()
                && !updateRequest.getUsername().equals(user.getUsername())) {

            // Check if the username is already taken
            if (userRepository.existsByUsername(updateRequest.getUsername())) {
                throw new BadRequestException("Username is already taken");
            }

            // Check if username was changed within the last 14 days
            if (user.getLastUsernameChange() != null &&
                    user.getLastUsernameChange().plusDays(14).isAfter(LocalDateTime.now())) {
                throw new BadRequestException("Username can only be changed once every 14 days");
            }

            user.setUsername(updateRequest.getUsername());
            user.setLastUsernameChange(LocalDateTime.now());
        }

        // Update profile image if provided
        if (updateRequest.getProfileImagePath() != null) {
            user.setProfileImagePath(updateRequest.getProfileImagePath());
        }

        User updatedUser = userRepository.save(user);
        return mapUserToUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public ApiResponse changePassword(PasswordChangeRequest passwordChangeRequest) {
        UserPrincipal currentUser = getCurrentUserPrincipal();
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        // Verify current password
        if (!passwordEncoder.matches(passwordChangeRequest.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Check if new password and confirmation match
        if (!passwordChangeRequest.getNewPassword().equals(passwordChangeRequest.getConfirmPassword())) {
            throw new BadRequestException("New password and confirmation do not match");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(passwordChangeRequest.getNewPassword()));
        userRepository.save(user);

        return ApiResponse.builder()
                .success(true)
                .message("Password changed successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public ApiResponse deleteAccount() {
        UserPrincipal currentUser = getCurrentUserPrincipal();
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        // Instead of a hard delete, mark as deleted
        user.setStatus(User.Status.DELETED);
        userRepository.save(user);

        return ApiResponse.builder()
                .success(true)
                .message("Account deleted successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public UserResponse getUserStorageInfo() {
        UserPrincipal currentUser = getCurrentUserPrincipal();
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        return UserResponse.builder()
                .id(user.getId())
                .storageQuota(user.getStorageQuota())
                .storageUsed(user.getStorageUsed())
                .build();
    }

    private UserPrincipal getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        return (UserPrincipal) authentication.getPrincipal();
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