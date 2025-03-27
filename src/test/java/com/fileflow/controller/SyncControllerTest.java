package com.fileflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileflow.config.TestSecurityConfig;
import com.fileflow.dto.request.sync.DeviceRegistrationRequest;
import com.fileflow.dto.request.sync.PushTokenUpdateRequest;
import com.fileflow.dto.request.sync.SyncBatchRequest;
import com.fileflow.dto.request.sync.SyncItemRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.sync.DeviceResponse;
import com.fileflow.dto.response.sync.SyncStatusResponse;
import com.fileflow.model.Device;
import com.fileflow.model.SyncQueue;
import com.fileflow.security.JwtAuthenticationFilter;
import com.fileflow.service.sync.SyncService;
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
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SyncController.class)
@Import(TestSecurityConfig.class)
public class SyncControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private SyncService syncService;

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
    public void testRegisterDevice() throws Exception {
        // Create mock request and response
        DeviceRegistrationRequest request = DeviceRegistrationRequest.builder()
                .deviceName("Test Phone")
                .deviceType("MOBILE")
                .platform("Android")
                .build();

        Device device = Device.builder()
                .id(1L)
                .deviceName("Test Phone")
                .deviceType("MOBILE")
                .platform("Android")
                .lastActive(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        when(syncService.registerDevice(eq("Test Phone"), eq("MOBILE"), eq("Android")))
                .thenReturn(device);

        // Perform the request
        mockMvc.perform(post("/api/v1/sync/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.deviceName").value("Test Phone"))
                .andExpect(jsonPath("$.deviceType").value("MOBILE"))
                .andExpect(jsonPath("$.platform").value("Android"));
    }

    @Test
    @WithMockUser
    public void testUpdatePushToken() throws Exception {
        // Create mock request and response
        PushTokenUpdateRequest request = PushTokenUpdateRequest.builder()
                .pushToken("new-push-token")
                .build();

        Device device = Device.builder()
                .id(1L)
                .deviceName("Test Phone")
                .deviceType("MOBILE")
                .platform("Android")
                .lastActive(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        when(syncService.updateDevicePushToken(eq(1L), eq("new-push-token")))
                .thenReturn(device);

        // Perform the request
        mockMvc.perform(put("/api/v1/sync/devices/1/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.deviceName").value("Test Phone"));
    }

    @Test
    @WithMockUser
    public void testGetSyncStatus() throws Exception {
        SyncStatusResponse response = SyncStatusResponse.builder()
                .deviceId(1L)
                .lastSyncDate(LocalDateTime.now())
                .pendingItemsCount(5)
                .build();

        when(syncService.getSyncStatus(eq(1L))).thenReturn(response);

        mockMvc.perform(get("/api/v1/sync/status/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value(1))
                .andExpect(jsonPath("$.pendingItemsCount").value(5));
    }

    @Test
    @WithMockUser
    public void testAddToSyncQueue() throws Exception {
        SyncItemRequest request = SyncItemRequest.builder()
                .actionType("CREATE")
                .itemId(1L)
                .itemType("FILE")
                .dataPayload("{\"name\":\"test.txt\"}")
                .build();

        ApiResponse response = ApiResponse.builder()
                .success(true)
                .message("Item added to sync queue")
                .build();

        when(syncService.addToSyncQueue(eq(1L), any(SyncItemRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/sync/queue/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Item added to sync queue"));
    }

    @Test
    @WithMockUser
    public void testGetPendingSyncItems() throws Exception {
        SyncQueue item1 = new SyncQueue();
        item1.setId(1L);
        item1.setActionType("CREATE");
        item1.setItemId(1L);
        item1.setItemType("FILE");
        item1.setDataPayload("{\"name\":\"test.txt\"}");

        SyncQueue item2 = new SyncQueue();
        item2.setId(2L);
        item2.setActionType("UPDATE");
        item2.setItemId(2L);
        item2.setItemType("FOLDER");
        item2.setDataPayload("{\"name\":\"Documents\"}");

        List<SyncQueue> syncItems = Arrays.asList(item1, item2);

        when(syncService.getPendingSyncItems(eq(1L))).thenReturn(syncItems);

        mockMvc.perform(get("/api/v1/sync/queue/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].actionType").value("CREATE"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].actionType").value("UPDATE"));
    }

    @Test
    @WithMockUser
    public void testProcessSyncBatch() throws Exception {
        SyncBatchRequest request = SyncBatchRequest.builder()
                .successfulItems(Arrays.asList(1L))
                .failedItems(Arrays.asList())
                .build();

        SyncStatusResponse response = SyncStatusResponse.builder()
                .deviceId(1L)
                .lastSyncDate(LocalDateTime.now())
                .pendingItemsCount(0)
                .build();

        when(syncService.processSyncBatch(eq(1L), any(SyncBatchRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/sync/process/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value(1))
                .andExpect(jsonPath("$.pendingItemsCount").value(0));
    }

    @Test
    @WithMockUser
    public void testMarkSyncItemProcessed() throws Exception {
        ApiResponse response = ApiResponse.builder()
                .success(true)
                .message("Sync item marked as processed")
                .build();

        when(syncService.markSyncItemProcessed(eq(1L), eq(true))).thenReturn(response);

        mockMvc.perform(post("/api/v1/sync/queue/1/complete")
                        .param("success", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Sync item marked as processed"));
    }
}