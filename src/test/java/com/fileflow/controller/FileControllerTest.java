package com.fileflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileflow.config.TestSecurityConfig;
import com.fileflow.dto.request.file.FileUpdateRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.dto.response.file.FileUploadResponse;
import com.fileflow.security.JwtAuthenticationFilter;
import com.fileflow.service.file.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class)
@Import(TestSecurityConfig.class)
public class FileControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

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
    public void testUploadFile() throws Exception {
        // Create mock data
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World!".getBytes()
        );

        FileUploadResponse response = FileUploadResponse.builder()
                .fileId(1L)
                .filename("test.txt")
                .fileSize(13L)
                .fileType("document")
                .mimeType("text/plain")
                .build();

        when(fileService.uploadFile(any())).thenReturn(response);

        // Perform the upload request
        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("folderId", "1")
                        .param("overwrite", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileId").value(1))
                .andExpect(jsonPath("$.filename").value("test.txt"))
                .andExpect(jsonPath("$.fileSize").value(13));
    }

    @Test
    @WithMockUser
    public void testGetFile() throws Exception {
        // Create mock data
        FileResponse response = FileResponse.builder()
                .id(1L)
                .filename("test.txt")
                .originalFilename("test.txt")
                .fileSize(13L)
                .fileType("document")
                .mimeType("text/plain")
                .parentFolderId(1L)
                .isFavorite(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .downloadUrl("/api/v1/files/download/1")
                .build();

        when(fileService.getFile(1L)).thenReturn(response);

        // Perform the get request
        mockMvc.perform(get("/api/v1/files/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.filename").value("test.txt"));
    }

    @Test
    @WithMockUser
    public void testDownloadFile() throws Exception {
        // Create mock data
        Resource fileResource = new ByteArrayResource("Hello, World!".getBytes());

        FileResponse fileResponse = FileResponse.builder()
                .id(1L)
                .filename("test.txt")
                .mimeType("text/plain")
                .build();

        when(fileService.loadFileAsResource(1L)).thenReturn(fileResource);
        when(fileService.getFile(1L)).thenReturn(fileResponse);

        // Perform the download request
        mockMvc.perform(get("/api/v1/files/download/1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.txt\""))
                .andExpect(content().bytes("Hello, World!".getBytes()));
    }

    @Test
    @WithMockUser
    public void testUpdateFile() throws Exception {
        // Create mock data
        FileUpdateRequest updateRequest = FileUpdateRequest.builder()
                .filename("updated.txt")
                .build();

        FileResponse response = FileResponse.builder()
                .id(1L)
                .filename("updated.txt")
                .originalFilename("test.txt")
                .fileSize(13L)
                .fileType("document")
                .mimeType("text/plain")
                .build();

        when(fileService.updateFile(eq(1L), any(FileUpdateRequest.class))).thenReturn(response);

        // Perform the update request
        mockMvc.perform(put("/api/v1/files/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.filename").value("updated.txt"));
    }

    @Test
    @WithMockUser
    public void testDeleteFile() throws Exception {
        // Create mock data
        ApiResponse response = ApiResponse.builder()
                .success(true)
                .message("File moved to trash")
                .build();

        when(fileService.deleteFile(1L)).thenReturn(response);

        // Perform the delete request
        mockMvc.perform(delete("/api/v1/files/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("File moved to trash"));
    }

    @Test
    @WithMockUser
    public void testGetFilesInFolder() throws Exception {
        // Create mock data
        List<FileResponse> files = Arrays.asList(
                FileResponse.builder().id(1L).filename("file1.txt").build(),
                FileResponse.builder().id(2L).filename("file2.txt").build()
        );

        when(fileService.getFilesInFolder(1L)).thenReturn(files);

        // Perform the get request
        mockMvc.perform(get("/api/v1/files/folder/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].filename").value("file1.txt"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].filename").value("file2.txt"));
    }
}