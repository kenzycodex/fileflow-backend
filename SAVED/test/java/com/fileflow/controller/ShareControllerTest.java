package com.fileflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileflow.config.TestSecurityConfig;
import com.fileflow.dto.request.share.ShareCreateRequest;
import com.fileflow.dto.request.share.ShareUpdateRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.common.PagedResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.dto.response.folder.FolderContentsResponse;
import com.fileflow.dto.response.share.ShareResponse;
import com.fileflow.security.JwtAuthenticationFilter;
import com.fileflow.service.file.FileService;
import com.fileflow.service.folder.FolderService;
import com.fileflow.service.search.SearchService;
import com.fileflow.service.share.ShareService;
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
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShareController.class)
@Import(TestSecurityConfig.class)
public class ShareControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private ShareService shareService;

    @MockBean
    private FileService fileService;

    @MockBean
    private FolderService folderService;

    @MockBean
    private SearchService searchService;

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
    public void testCreateShare() throws Exception {
        ShareCreateRequest createRequest = ShareCreateRequest.builder()
                .itemId(1L)
                .itemType("FILE")
                .expiryDate(LocalDateTime.now().plusDays(7))
                .password("password123")
                .passwordProtected(true)
                .permissions("READ")
                .build();

        ShareResponse response = ShareResponse.builder()
                .id(1L)
                .itemId(1L)
                .itemType("FILE")
                .itemName("document.txt")
                .shareLink("abc123")
                .expiryDate(LocalDateTime.now().plusDays(7))
                .passwordProtected(true)
                .permissions("READ")
                .createdAt(LocalDateTime.now())
                .ownerName("testuser")
                .build();

        when(shareService.createShare(any(ShareCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.itemId").value(1))
                .andExpect(jsonPath("$.itemType").value("FILE"))
                .andExpect(jsonPath("$.shareLink").value("abc123"))
                .andExpect(jsonPath("$.passwordProtected").value(true))
                .andExpect(jsonPath("$.permissions").value("READ"));
    }

    @Test
    @WithMockUser
    public void testGetShare() throws Exception {
        ShareResponse response = ShareResponse.builder()
                .id(1L)
                .itemId(1L)
                .itemType("FILE")
                .itemName("document.txt")
                .shareLink("abc123")
                .passwordProtected(true)
                .permissions("READ")
                .createdAt(LocalDateTime.now())
                .ownerName("testuser")
                .build();

        when(shareService.getShare(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/shares/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.itemId").value(1))
                .andExpect(jsonPath("$.itemType").value("FILE"))
                .andExpect(jsonPath("$.shareLink").value("abc123"));
    }

    @Test
    @WithMockUser
    public void testUpdateShare() throws Exception {
        ShareUpdateRequest updateRequest = ShareUpdateRequest.builder()
                .expiryDate(LocalDateTime.now().plusDays(14))
                .password("newpassword")
                .isPasswordProtected(true)
                .permissions("READ_WRITE")
                .build();

        ShareResponse response = ShareResponse.builder()
                .id(1L)
                .itemId(1L)
                .itemType("FILE")
                .itemName("document.txt")
                .shareLink("abc123")
                .expiryDate(LocalDateTime.now().plusDays(14))
                .passwordProtected(true)
                .permissions("READ_WRITE")
                .createdAt(LocalDateTime.now())
                .ownerName("testuser")
                .build();

        when(shareService.updateShare(eq(1L), any(ShareUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/shares/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.permissions").value("READ_WRITE"))
                .andExpect(jsonPath("$.passwordProtected").value(true));
    }

    @Test
    @WithMockUser
    public void testDeleteShare() throws Exception {
        ApiResponse response = ApiResponse.builder()
                .success(true)
                .message("Share deleted successfully")
                .build();

        when(shareService.deleteShare(1L)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/shares/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Share deleted successfully"));
    }

    @Test
    @WithMockUser
    public void testGetOutgoingShares() throws Exception {
        ShareResponse share1 = ShareResponse.builder()
                .id(1L)
                .itemType("FILE")
                .itemName("document.txt")
                .shareLink("abc123")
                .build();

        ShareResponse share2 = ShareResponse.builder()
                .id(2L)
                .itemType("FOLDER")
                .itemName("Documents")
                .shareLink("def456")
                .build();

        PagedResponse<ShareResponse> response = PagedResponse.<ShareResponse>builder()
                .content(Arrays.asList(share1, share2))
                .page(0)
                .size(20)
                .totalElements(2)
                .totalPages(1)
                .last(true)
                .build();

        when(shareService.getOutgoingShares(anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/v1/shares/outgoing")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].itemType").value("FILE"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].itemType").value("FOLDER"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser
    public void testGetIncomingShares() throws Exception {
        ShareResponse share1 = ShareResponse.builder()
                .id(1L)
                .itemType("FILE")
                .itemName("document.txt")
                .shareLink("abc123")
                .ownerName("otheruser")
                .build();

        PagedResponse<ShareResponse> response = PagedResponse.<ShareResponse>builder()
                .content(Arrays.asList(share1))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(shareService.getIncomingShares(anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/v1/shares/incoming")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].itemType").value("FILE"))
                .andExpect(jsonPath("$.content[0].ownerName").value("otheruser"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser
    public void testGetShareByLink() throws Exception {
        ShareResponse response = ShareResponse.builder()
                .id(1L)
                .itemId(1L)
                .itemType("FILE")
                .itemName("document.txt")
                .shareLink("abc123")
                .passwordProtected(false)
                .permissions("READ")
                .createdAt(LocalDateTime.now())
                .ownerName("testuser")
                .build();

        // Fix: Use explicit null parameter instead of anyString() to match exact signature
        when(shareService.getShareByLink(eq("abc123"), isNull())).thenReturn(response);

        mockMvc.perform(get("/api/v1/shares/links/abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.itemId").value(1))
                .andExpect(jsonPath("$.itemType").value("FILE"))
                .andExpect(jsonPath("$.shareLink").value("abc123"));
    }

    @Test
    @WithMockUser
    public void testValidateSharePassword() throws Exception {
        ApiResponse response = ApiResponse.builder()
                .success(true)
                .message("Password valid")
                .build();

        when(shareService.validateSharePassword(eq(1L), eq("password123"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/shares/1/validate-password")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password valid"));
    }

    @Test
    @WithMockUser
    public void testGetSharedFile() throws Exception {
        ShareResponse shareResponse = ShareResponse.builder()
                .id(1L)
                .itemId(1L)
                .itemType("FILE") // Important: properly set item type to FILE
                .shareLink("abc123")
                .build();

        FileResponse fileResponse = FileResponse.builder()
                .id(1L)
                .filename("document.txt")
                .fileSize(1024L)
                .fileType("document")
                .build();

        // Fix: Use explicit null parameter instead of anyString() to match exact signature
        when(shareService.getShareByLink(eq("abc123"), isNull())).thenReturn(shareResponse);
        when(fileService.getFile(1L)).thenReturn(fileResponse);

        mockMvc.perform(get("/api/v1/shares/links/abc123/file"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.filename").value("document.txt"));
    }

    @Test
    @WithMockUser
    public void testGetSharedFolder() throws Exception {
        ShareResponse shareResponse = ShareResponse.builder()
                .id(1L)
                .itemId(1L)
                .itemType("FOLDER") // Important: properly set item type to FOLDER
                .shareLink("abc123")
                .build();

        FolderContentsResponse folderContentsResponse = FolderContentsResponse.builder()
                .folderId(1L)
                .folderName("Documents")
                .files(Collections.emptyList())
                .folders(Collections.emptyList())
                .build();

        // Fix: Use explicit null parameter instead of anyString() to match exact signature
        when(shareService.getShareByLink(eq("abc123"), isNull())).thenReturn(shareResponse);
        when(folderService.getFolderContents(1L)).thenReturn(folderContentsResponse);

        mockMvc.perform(get("/api/v1/shares/links/abc123/folder"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").value(1))
                .andExpect(jsonPath("$.folderName").value("Documents"));
    }
}