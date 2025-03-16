package com.fileflow.controller;

import com.fileflow.dto.request.user.PasswordChangeRequest;
import com.fileflow.dto.request.user.UserUpdateRequest;
import com.fileflow.dto.response.auth.UserResponse;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User management API")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<UserResponse> updateUser(@Valid @RequestBody UserUpdateRequest updateRequest) {
        return ResponseEntity.ok(userService.updateUser(updateRequest));
    }

    @PutMapping("/password")
    @Operation(summary = "Change user password")
    public ResponseEntity<ApiResponse> changePassword(@Valid @RequestBody PasswordChangeRequest passwordChangeRequest) {
        return ResponseEntity.ok(userService.changePassword(passwordChangeRequest));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete current user account")
    public ResponseEntity<ApiResponse> deleteAccount() {
        return ResponseEntity.ok(userService.deleteAccount());
    }

    @GetMapping("/storage")
    @Operation(summary = "Get storage usage information")
    public ResponseEntity<UserResponse> getStorageInfo() {
        return ResponseEntity.ok(userService.getUserStorageInfo());
    }
}
