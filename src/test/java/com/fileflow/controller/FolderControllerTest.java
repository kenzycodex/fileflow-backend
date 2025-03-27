package com.fileflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileflow.config.TestSecurityConfig;
import com.fileflow.dto.request.folder.FolderCreateRequest;
import com.fileflow.dto.request.folder.FolderUpdateRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.dto.response.folder.FolderContentsResponse;
import com.fileflow.dto.response.folder.FolderResponse;
import com.fileflow.security.JwtAuthenticationFilter;
import com.fileflow.service.folder.FolderService;
import com.fileflow.service.search.SearchService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FolderController.class)
@Import(TestSecurityConfig.class)
public class FolderControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private FolderService folderService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private SearchService searchService;

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
    public void testCreateFolder() throws Exception {
        // Create mock data
        FolderCreateRequest createRequest = FolderCreateRequest.builder()
                .folderName("Test Folder")
                .parentFolderId(1L)
                .build();

        FolderResponse response = FolderResponse.builder()
                .id(1L)
                .folderName("Test Folder")
                .parentFolderId(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isFavorite(false)
                .build();

        when(folderService.createFolder(any())).thenReturn(response);

        // Perform the create request
        mockMvc.perform(post("/api/v1/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.folderName").value("Test Folder"))
                .andExpect(jsonPath("$.parentFolderId").value(1));
    }

    @Test
    @WithMockUser
    public void testGetFolder() throws Exception {
        // Create mock data
        FolderResponse response = FolderResponse.builder()
                .id(1L)
                .folderName("Test Folder")
                .parentFolderId(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isFavorite(false)
                .build();

        when(folderService.getFolder(1L)).thenReturn(response);

        // Perform the get request
        mockMvc.perform(get("/api/v1/folders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.folderName").value("Test Folder"));
    }

    @Test
    @WithMockUser
    public void testGetFolderContents() throws Exception {
        // Create mock data
        List<FileResponse> files = Arrays.asList(
                FileResponse.builder().id(1L).filename("file1.txt").build(),
                FileResponse.builder().id(2L).filename("file2.txt").build()
        );

        List<FolderResponse> folders = Arrays.asList(
                FolderResponse.builder().id(2L).folderName("Subfolder 1").build(),
                FolderResponse.builder().id(3L).folderName("Subfolder 2").build()
        );

        FolderContentsResponse response = FolderContentsResponse.builder()
                .folderId(1L)
                .folderName("Test Folder")
                .files(files)
                .folders(folders)
                .build();

        when(folderService.getFolderContents(1L)).thenReturn(response);

        // Perform the get request
        mockMvc.perform(get("/api/v1/folders/1/contents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").value(1))
                .andExpect(jsonPath("$.folderName").value("Test Folder"))
                .andExpect(jsonPath("$.files.length()").value(2))
                .andExpect(jsonPath("$.folders.length()").value(2))
                .andExpect(jsonPath("$.files[0].id").value(1))
                .andExpect(jsonPath("$.folders[0].id").value(2));
    }

    @Test
    @WithMockUser
    public void testUpdateFolder() throws Exception {
        // Create mock data
        FolderUpdateRequest updateRequest = FolderUpdateRequest.builder()
                .folderName("Updated Folder")
                .build();

        FolderResponse response = FolderResponse.builder()
                .id(1L)
                .folderName("Updated Folder")
                .parentFolderId(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isFavorite(false)
                .build();

        when(folderService.updateFolder(eq(1L), any(FolderUpdateRequest.class))).thenReturn(response);

        // Perform the update request
        mockMvc.perform(put("/api/v1/folders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.folderName").value("Updated Folder"));
    }

    @Test
    @WithMockUser
    public void testDeleteFolder() throws Exception {
        // Create mock data
        ApiResponse response = ApiResponse.builder()
                .success(true)
                .message("Folder moved to trash")
                .build();

        when(folderService.deleteFolder(1L)).thenReturn(response);

        // Perform the delete request
        mockMvc.perform(delete("/api/v1/folders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Folder moved to trash"));
    }

    @Test
    @WithMockUser
    public void testGetRootFolders() throws Exception {
        // Create mock data
        List<FolderResponse> folders = Arrays.asList(
                FolderResponse.builder().id(1L).folderName("Folder 1").build(),
                FolderResponse.builder().id(2L).folderName("Folder 2").build()
        );

        when(folderService.getRootFolders()).thenReturn(folders);

        // Perform the get request
        mockMvc.perform(get("/api/v1/folders/root"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].folderName").value("Folder 1"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].folderName").value("Folder 2"));
    }

    @Test
    @WithMockUser
    public void testGetRootFolderContents() throws Exception {
        // Create mock data
        List<FileResponse> files = Arrays.asList(
                FileResponse.builder().id(1L).filename("file1.txt").build(),
                FileResponse.builder().id(2L).filename("file2.txt").build()
        );

        List<FolderResponse> folders = Arrays.asList(
                FolderResponse.builder().id(2L).folderName("Subfolder 1").build(),
                FolderResponse.builder().id(3L).folderName("Subfolder 2").build()
        );

        FolderContentsResponse response = FolderContentsResponse.builder()
                .folderId(null)
                .folderName("Root")
                .files(files)
                .folders(folders)
                .build();

        when(folderService.getFolderContents(null)).thenReturn(response);

        // Perform the get request
        mockMvc.perform(get("/api/v1/folders/root/contents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderName").value("Root"))
                .andExpect(jsonPath("$.files.length()").value(2))
                .andExpect(jsonPath("$.folders.length()").value(2));
    }

    @Test
    @WithMockUser
    public void testMoveFolder() throws Exception {
        // Create mock data
        FolderResponse response = FolderResponse.builder()
                .id(1L)
                .folderName("Test Folder")
                .parentFolderId(2L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isFavorite(false)
                .build();

        when(folderService.moveFolder(1L, 2L)).thenReturn(response);

        // Perform the move request
        mockMvc.perform(post("/api/v1/folders/1/move")
                        .param("destinationFolderId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.parentFolderId").value(2));
    }

    @Test
    @WithMockUser
    public void testGetFolderPath() throws Exception {
        // Create mock data
        List<FolderResponse> path = Arrays.asList(
                FolderResponse.builder().id(1L).folderName("Root").parentFolderId(null).build(),
                FolderResponse.builder().id(2L).folderName("Folder 1").parentFolderId(1L).build(),
                FolderResponse.builder().id(3L).folderName("Subfolder").parentFolderId(2L).build()
        );

        when(folderService.getFolderPath(3L)).thenReturn(path);

        // Perform the get request
        mockMvc.perform(get("/api/v1/folders/3/path"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].folderName").value("Root"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].folderName").value("Folder 1"))
                .andExpect(jsonPath("$[2].id").value(3))
                .andExpect(jsonPath("$[2].folderName").value("Subfolder"));
    }
}