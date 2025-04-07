package com.fileflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileflow.config.TestSecurityConfig;
import com.fileflow.dto.request.user.PasswordChangeRequest;
import com.fileflow.dto.request.user.UserUpdateRequest;
import com.fileflow.dto.response.auth.UserResponse;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.security.JwtAuthenticationFilter;
import com.fileflow.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
public class UserControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser
    public void testGetCurrentUser() throws Exception {
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .storageQuota(10737418240L) // 10GB
                .storageUsed(2147483648L) // 2GB
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.getCurrentUser()).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.storageQuota").value(10737418240L))
                .andExpect(jsonPath("$.storageUsed").value(2147483648L))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @WithMockUser
    public void testGetUserById() throws Exception {
        UserResponse userResponse = UserResponse.builder()
                .id(2L)
                .username("otheruser")
                .email("other@example.com")
                .firstName("Other")
                .lastName("User")
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.getUserById(eq(2L))).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.username").value("otheruser"))
                .andExpect(jsonPath("$.email").value("other@example.com"))
                .andExpect(jsonPath("$.firstName").value("Other"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @WithMockUser
    public void testUpdateUser() throws Exception {
        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .firstName("Updated")
                .lastName("Name")
                .username("testuser")
                .build();

        UserResponse updatedUser = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com") // Match actual response shown in logs
                .firstName("Updated")
                .lastName("Name")
                .storageQuota(10737418240L) // 10GB
                .storageUsed(2147483648L) // 2GB
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.updateUser(any(UserUpdateRequest.class))).thenReturn(updatedUser);

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com")) // Fixed to match mocked response
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.lastName").value("Name"));
    }

    @Test
    @WithMockUser
    public void testChangePassword() throws Exception {
        // Including special character to pass validation
        PasswordChangeRequest passwordChangeRequest = PasswordChangeRequest.builder()
                .currentPassword("OldPassword123")
                .newPassword("NewPassword456!")  // Added ! as special character
                .confirmPassword("NewPassword456!")  // Added ! as special character
                .build();

        ApiResponse response = ApiResponse.builder()
                .success(true)
                .message("Password changed successfully")
                .timestamp(LocalDateTime.now())
                .build();

        when(userService.changePassword(any(PasswordChangeRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordChangeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    @WithMockUser
    public void testDeleteAccount() throws Exception {
        ApiResponse response = ApiResponse.builder()
                .success(true)
                .message("Account deleted successfully")
                .timestamp(LocalDateTime.now())
                .build();

        when(userService.deleteAccount()).thenReturn(response);

        mockMvc.perform(delete("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account deleted successfully"));
    }

    @Test
    @WithMockUser
    public void testGetStorageInfo() throws Exception {
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .storageQuota(10737418240L) // 10GB
                .storageUsed(2147483648L) // 2GB
                .build();

        when(userService.getUserStorageInfo()).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/users/storage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.storageQuota").value(10737418240L))
                .andExpect(jsonPath("$.storageUsed").value(2147483648L));
    }
}