package com.fileflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileflow.config.TestSecurityConfig;
import com.fileflow.dto.request.auth.MfaEnableRequest;
import com.fileflow.dto.request.auth.MfaVerifyRequest;
import com.fileflow.dto.response.auth.MfaResponse;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.security.UserPrincipal;
import com.fileflow.service.auth.MfaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MfaController.class)
@Import(TestSecurityConfig.class)
public class MfaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MfaService mfaService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserPrincipal userPrincipal;

    @BeforeEach
    public void setup() {
        userPrincipal = UserPrincipal.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Test
    public void testGetMfaStatus() throws Exception {
        // Setup mocks
        when(mfaService.isMfaEnabled(1L)).thenReturn(true);

        // Perform the request
        mockMvc.perform(get("/api/v1/auth/mfa/status")
                        .principal(() -> String.valueOf(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("MFA status retrieved successfully"))
                .andExpect(jsonPath("$.data").value(true));

        // Verify service call
        verify(mfaService).isMfaEnabled(1L);
    }

    @Test
    public void testSetupMfa() throws Exception {
        // Setup mocks
        String secret = "ABCDEFGHIJKLMNOP";
        String qrCodeUrl = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAMgAA...";

        when(mfaService.generateMfaSecret(1L)).thenReturn(secret);
        when(mfaService.generateQrCodeImageUri(eq(secret), eq("testuser"))).thenReturn(qrCodeUrl);

        // Perform the request
        mockMvc.perform(post("/api/v1/auth/mfa/setup")
                        .principal(() -> String.valueOf(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("MFA setup initiated successfully"))
                .andExpect(jsonPath("$.data.secret").value(secret))
                .andExpect(jsonPath("$.data.qrCodeUrl").value(qrCodeUrl));

        // Verify service calls
        verify(mfaService).generateMfaSecret(1L);
        verify(mfaService).generateQrCodeImageUri(secret, "testuser");
    }

    @Test
    public void testEnableMfa() throws Exception {
        // Setup request data
        MfaEnableRequest request = new MfaEnableRequest();
        request.setVerificationCode("123456");

        // Setup mocks
        doNothing().when(mfaService).enableMfa(eq(1L), eq("123456"));

        // Perform the request
        mockMvc.perform(post("/api/v1/auth/mfa/enable")
                        .principal(() -> String.valueOf(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("MFA enabled successfully"));

        // Verify service call
        verify(mfaService).enableMfa(1L, "123456");
    }

    @Test
    public void testVerifyMfa() throws Exception {
        // Setup request data
        MfaVerifyRequest request = new MfaVerifyRequest();
        request.setSecret("ABCDEFGHIJKLMNOP");
        request.setVerificationCode("123456");

        // Setup mocks
        when(mfaService.verifyCode(eq("ABCDEFGHIJKLMNOP"), eq("123456"))).thenReturn(true);

        // Perform the request
        mockMvc.perform(post("/api/v1/auth/mfa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("MFA verification successful"));

        // Verify service call
        verify(mfaService).verifyCode("ABCDEFGHIJKLMNOP", "123456");
    }

    @Test
    public void testVerifyMfa_invalidCode() throws Exception {
        // Setup request data
        MfaVerifyRequest request = new MfaVerifyRequest();
        request.setSecret("ABCDEFGHIJKLMNOP");
        request.setVerificationCode("999999");

        // Setup mocks
        when(mfaService.verifyCode(eq("ABCDEFGHIJKLMNOP"), eq("999999"))).thenReturn(false);

        // Perform the request
        mockMvc.perform(post("/api/v1/auth/mfa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value("Invalid verification code"));

        // Verify service call
        verify(mfaService).verifyCode("ABCDEFGHIJKLMNOP", "999999");
    }

    @Test
    public void testDisableMfa() throws Exception {
        // Setup mocks
        doNothing().when(mfaService).disableMfa(1L);

        // Perform the request
        mockMvc.perform(delete("/api/v1/auth/mfa/disable")
                        .principal(() -> String.valueOf(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("MFA disabled successfully"));

        // Verify service call
        verify(mfaService).disableMfa(1L);
    }

    @Test
    public void testSendEmailVerificationCode() throws Exception {
        // Setup mocks
        when(mfaService.generateEmailVerificationCode(1L)).thenReturn("123456");

        // Perform the request
        mockMvc.perform(post("/api/v1/auth/mfa/send-email-code")
                        .principal(() -> String.valueOf(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Verification code sent successfully"));

        // Verify service call
        verify(mfaService).generateEmailVerificationCode(1L);
    }
}
