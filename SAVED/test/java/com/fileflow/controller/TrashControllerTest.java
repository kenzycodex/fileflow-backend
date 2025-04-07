package com.fileflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileflow.config.TestSecurityConfig;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.common.SearchResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.dto.response.folder.FolderResponse;
import com.fileflow.security.JwtAuthenticationFilter;
import com.fileflow.service.trash.TrashService;
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
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrashController.class)
@Import(TestSecurityConfig.class)
public class TrashControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private TrashService trashService;

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
    public void testGetTrashItems() throws Exception {
        FileResponse file1 = FileResponse.builder()
                .id(1L)
                .filename("deleted_file.txt")
                .fileType("document")
                .build();

        FolderResponse folder1 = FolderResponse.builder()
                .id(2L)
                .folderName("Deleted Folder")
                .build();

        SearchResponse response = SearchResponse.builder()
                .files(Arrays.asList(file1))
                .folders(Arrays.asList(folder1))
                .page(0)
                .size(20)
                .totalElements(2)
                .totalPages(1)
                .hasMore(false)
                .build();

        when(trashService.getTrashItems(anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/v1/trash")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files.length()").value(1))
                .andExpect(jsonPath("$.folders.length()").value(1))
                .andExpect(jsonPath("$.files[0].filename").value("deleted_file.txt"))
                .andExpect(jsonPath("$.folders[0].folderName").value("Deleted Folder"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser
    public void testEmptyTrash() throws Exception {
        ApiResponse response = ApiResponse.builder()
                .success(true)
                .message("Trash emptied successfully")
                .timestamp(LocalDateTime.now())
                .build();

        when(trashService.emptyTrash()).thenReturn(response);

        mockMvc.perform(delete("/api/v1/trash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Trash emptied successfully"));
    }

    @Test
    @WithMockUser
    public void testRestoreAllFromTrash() throws Exception {
        ApiResponse response = ApiResponse.builder()
                .success(true)
                .message("All items restored from trash successfully")
                .timestamp(LocalDateTime.now())
                .build();

        when(trashService.restoreAllFromTrash()).thenReturn(response);

        mockMvc.perform(post("/api/v1/trash/restore-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("All items restored from trash successfully"));
    }

    @Test
    @WithMockUser
    public void testGetTrashInfo() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("fileCount", 5);
        data.put("folderCount", 2);
        data.put("totalSize", 10485760L); // 10MB
        data.put("oldestItem", "2023-01-15T12:30:45");
        data.put("autoDeleteDate", "2023-03-15T12:30:45");
        data.put("remainingDays", 30);

        ApiResponse response = ApiResponse.builder()
                .success(true)
                .message("Trash information retrieved successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();

        when(trashService.getTrashInfo()).thenReturn(response);

        mockMvc.perform(get("/api/v1/trash/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Trash information retrieved successfully"))
                .andExpect(jsonPath("$.data.fileCount").value(5))
                .andExpect(jsonPath("$.data.folderCount").value(2))
                .andExpect(jsonPath("$.data.totalSize").value(10485760L))
                .andExpect(jsonPath("$.data.oldestItem").value("2023-01-15T12:30:45"))
                .andExpect(jsonPath("$.data.autoDeleteDate").value("2023-03-15T12:30:45"))
                .andExpect(jsonPath("$.data.remainingDays").value(30));
    }
}