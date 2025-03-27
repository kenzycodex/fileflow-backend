package com.fileflow.controller;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.common.SearchResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.dto.response.folder.FolderResponse;
import com.fileflow.model.File;
import com.fileflow.repository.FileRepository;
import com.fileflow.security.JwtAuthenticationFilter;
import com.fileflow.service.search.SearchService;
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
import com.fileflow.config.TestSecurityConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@Import(TestSecurityConfig.class)
public class SearchControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @MockBean
    private FileRepository fileRepository;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser
    public void testSearch() throws Exception {
        // Create mock data
        FileResponse file1 = FileResponse.builder()
                .id(1L)
                .filename("document.txt")
                .fileType("document")
                .build();

        FolderResponse folder1 = FolderResponse.builder()
                .id(2L)
                .folderName("Documents")
                .build();

        SearchResponse response = SearchResponse.builder()
                .files(Arrays.asList(file1))
                .folders(Arrays.asList(folder1))
                .page(0)
                .size(20)
                .totalElements(2)
                .totalPages(1)
                .hasMore(false)
                .query("document")
                .build();

        when(searchService.search(eq("document"), anyInt(), anyInt())).thenReturn(response);

        // Perform the search request
        mockMvc.perform(get("/api/v1/search")
                        .param("query", "document")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files.length()").value(1))
                .andExpect(jsonPath("$.folders.length()").value(1))
                .andExpect(jsonPath("$.files[0].filename").value("document.txt"))
                .andExpect(jsonPath("$.folders[0].folderName").value("Documents"))
                .andExpect(jsonPath("$.query").value("document"));
    }

    @Test
    @WithMockUser
    public void testSearchFiles() throws Exception {
        // Create mock data
        FileResponse file1 = FileResponse.builder()
                .id(1L)
                .filename("document.txt")
                .fileType("document")
                .build();

        SearchResponse response = SearchResponse.builder()
                .files(Arrays.asList(file1))
                .folders(Collections.emptyList())
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .hasMore(false)
                .query("document")
                .build();

        when(searchService.searchFiles(eq("document"), anyInt(), anyInt())).thenReturn(response);

        // Perform the search request
        mockMvc.perform(get("/api/v1/search/files")
                        .param("query", "document")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files.length()").value(1))
                .andExpect(jsonPath("$.folders.length()").value(0))
                .andExpect(jsonPath("$.files[0].filename").value("document.txt"));
    }

    @Test
    @WithMockUser
    public void testSearchByFileType() throws Exception {
        // Create mock data
        FileResponse file1 = FileResponse.builder()
                .id(1L)
                .filename("document.txt")
                .fileType("document")
                .build();

        FileResponse file2 = FileResponse.builder()
                .id(2L)
                .filename("report.txt")
                .fileType("document")
                .build();

        SearchResponse response = SearchResponse.builder()
                .files(Arrays.asList(file1, file2))
                .folders(Collections.emptyList())
                .page(0)
                .size(20)
                .totalElements(2)
                .totalPages(1)
                .hasMore(false)
                .query("txt")
                .build();

        when(searchService.searchByFileType(eq("document"), eq("txt"), anyInt(), anyInt())).thenReturn(response);

        // Perform the search request
        mockMvc.perform(get("/api/v1/search/by-type/document")
                        .param("query", "txt")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files.length()").value(2))
                .andExpect(jsonPath("$.files[0].fileType").value("document"))
                .andExpect(jsonPath("$.files[1].fileType").value("document"));
    }

    @Test
    @WithMockUser
    public void testSearchFileContents() throws Exception {
        // Create mock data
        FileResponse file1 = FileResponse.builder()
                .id(1L)
                .filename("document.txt")
                .fileType("document")
                .build();

        SearchResponse response = SearchResponse.builder()
                .files(Arrays.asList(file1))
                .folders(Collections.emptyList())
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .hasMore(false)
                .query("content search")
                .build();

        when(searchService.searchFileContents(eq("content search"), anyInt(), anyInt())).thenReturn(response);

        // Perform the search request
        mockMvc.perform(get("/api/v1/search/content")
                        .param("query", "content search")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files.length()").value(1))
                .andExpect(jsonPath("$.files[0].filename").value("document.txt"))
                .andExpect(jsonPath("$.query").value("content search"));
    }

    @Test
    @WithMockUser
    public void testSearchByTag() throws Exception {
        // Create mock data
        FileResponse file1 = FileResponse.builder()
                .id(1L)
                .filename("document.txt")
                .fileType("document")
                .build();

        SearchResponse response = SearchResponse.builder()
                .files(Arrays.asList(file1))
                .folders(Collections.emptyList())
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .hasMore(false)
                .query("important")
                .build();

        when(searchService.searchByTag(eq("important"), anyInt(), anyInt())).thenReturn(response);

        // Perform the search request
        mockMvc.perform(get("/api/v1/search/tags/important")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files.length()").value(1))
                .andExpect(jsonPath("$.files[0].filename").value("document.txt"))
                .andExpect(jsonPath("$.query").value("important"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    public void testIndexFile() throws Exception {
        // Create mock file data
        File mockFile = new File();
        mockFile.setId(1L);
        mockFile.setFilename("test.txt");

        // Set up file repository
        when(fileRepository.findById(1L)).thenReturn(Optional.of(mockFile));

        // Set up the search service to do nothing when indexFile is called
        // This is the correct way to mock void methods
        doNothing().when(searchService).indexFile(any(File.class));

        // Perform the index request
        mockMvc.perform(post("/api/v1/search/index-file/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("File indexing initiated"));

        // Verify the service method was called
        verify(searchService).indexFile(any(File.class));
    }
}