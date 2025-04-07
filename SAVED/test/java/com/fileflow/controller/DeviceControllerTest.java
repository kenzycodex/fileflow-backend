package com.fileflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileflow.config.TestSecurityConfig;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.common.PagedResponse;
import com.fileflow.model.Device;
import com.fileflow.security.JwtAuthenticationFilter;
import com.fileflow.service.device.DeviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeviceController.class)
@Import(TestSecurityConfig.class)
public class DeviceControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private DeviceService deviceService;

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
    public void testGetUserDevices() throws Exception {
        Device device1 = Device.builder()
                .id(1L)
                .deviceName("Phone")
                .deviceType("MOBILE")
                .platform("Android")
                .lastActive(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        Device device2 = Device.builder()
                .id(2L)
                .deviceName("Laptop")
                .deviceType("DESKTOP")
                .platform("Windows")
                .lastActive(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        PagedResponse<Device> response = PagedResponse.<Device>builder()
                .content(Arrays.asList(device1, device2))
                .page(0)
                .size(20)
                .totalElements(2)
                .totalPages(1)
                .last(true)
                .build();

        when(deviceService.getUserDevices(anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/v1/devices")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].deviceName").value("Phone"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].deviceName").value("Laptop"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser
    public void testRegisterDevice() throws Exception {
        Device device = Device.builder()
                .id(1L)
                .deviceName("New Phone")
                .deviceType("MOBILE")
                .platform("iOS")
                .pushToken("push-token-123")
                .lastActive(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        when(deviceService.registerDevice(
                eq("New Phone"),
                eq("MOBILE"),
                eq("iOS"),
                eq("push-token-123")))
                .thenReturn(device);

        mockMvc.perform(post("/api/v1/devices")
                        .param("deviceName", "New Phone")
                        .param("deviceType", "MOBILE")
                        .param("platform", "iOS")
                        .param("pushToken", "push-token-123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.deviceName").value("New Phone"))
                .andExpect(jsonPath("$.deviceType").value("MOBILE"))
                .andExpect(jsonPath("$.platform").value("iOS"))
                .andExpect(jsonPath("$.pushToken").value("push-token-123"));
    }

    @Test
    @WithMockUser
    public void testUpdateDevice() throws Exception {
        Device device = Device.builder()
                .id(1L)
                .deviceName("Updated Phone")
                .deviceType("MOBILE")
                .platform("iOS")
                .pushToken("new-push-token")
                .lastActive(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        when(deviceService.updateDevice(eq(1L), eq("Updated Phone"), eq("new-push-token")))
                .thenReturn(device);

        mockMvc.perform(put("/api/v1/devices/1")
                        .param("deviceName", "Updated Phone")
                        .param("pushToken", "new-push-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.deviceName").value("Updated Phone"))
                .andExpect(jsonPath("$.pushToken").value("new-push-token"));
    }

    @Test
    @WithMockUser
    public void testDeleteDevice() throws Exception {
        ApiResponse response = ApiResponse.builder()
                .success(true)
                .message("Device deleted successfully")
                .build();

        when(deviceService.deleteDevice(eq(1L))).thenReturn(response);

        mockMvc.perform(delete("/api/v1/devices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Device deleted successfully"));
    }

    @Test
    @WithMockUser
    public void testSyncDevice() throws Exception {
        ApiResponse response = ApiResponse.builder()
                .success(true)
                .message("Device sync initiated")
                .build();

        when(deviceService.syncDevice(eq(1L))).thenReturn(response);

        mockMvc.perform(post("/api/v1/devices/1/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Device sync initiated"));
    }

    @Test
    @WithMockUser
    public void testGetDevice() throws Exception {
        Device device = Device.builder()
                .id(1L)
                .deviceName("Test Phone")
                .deviceType("MOBILE")
                .platform("Android")
                .lastActive(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        when(deviceService.getDevice(eq(1L))).thenReturn(device);

        mockMvc.perform(get("/api/v1/devices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.deviceName").value("Test Phone"))
                .andExpect(jsonPath("$.deviceType").value("MOBILE"))
                .andExpect(jsonPath("$.platform").value("Android"));
    }
}