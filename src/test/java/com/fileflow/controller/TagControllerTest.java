package com.fileflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileflow.config.TestSecurityConfig;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.model.Tag;
import com.fileflow.model.User;
import com.fileflow.security.JwtAuthenticationFilter;
import com.fileflow.service.tag.TagService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TagController.class)
@Import(TestSecurityConfig.class)
public class TagControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private TagService tagService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    @WithMockUser
    public void testCreateTag() throws Exception {
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setName("Important");
        tag.setColor("#FF0000");
        tag.setUser(testUser);
        tag.setCreatedAt(LocalDateTime.now());

        when(tagService.createTag(eq("Important"), eq("#FF0000"))).thenReturn(tag);

        mockMvc.perform(post("/api/v1/tags")
                        .param("name", "Important")
                        .param("color", "#FF0000"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Important"))
                .andExpect(jsonPath("$.color").value("#FF0000"));
    }

    @Test
    @WithMockUser
    public void testGetUserTags() throws Exception {
        Tag tag1 = new Tag();
        tag1.setId(1L);
        tag1.setName("Important");
        tag1.setColor("#FF0000");
        tag1.setUser(testUser);
        tag1.setCreatedAt(LocalDateTime.now());

        Tag tag2 = new Tag();
        tag2.setId(2L);
        tag2.setName("Work");
        tag2.setColor("#00FF00");
        tag2.setUser(testUser);
        tag2.setCreatedAt(LocalDateTime.now());

        List<Tag> tags = Arrays.asList(tag1, tag2);

        when(tagService.getUserTags()).thenReturn(tags);

        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Important"))
                .andExpect(jsonPath("$[0].color").value("#FF0000"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Work"))
                .andExpect(jsonPath("$[1].color").value("#00FF00"));
    }

    @Test
    @WithMockUser
    public void testUpdateTag() throws Exception {
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setName("Very Important");
        tag.setColor("#FF5500");
        tag.setUser(testUser);
        tag.setCreatedAt(LocalDateTime.now());

        when(tagService.updateTag(eq(1L), eq("Very Important"), eq("#FF5500"))).thenReturn(tag);

        mockMvc.perform(put("/api/v1/tags/1")
                        .param("name", "Very Important")
                        .param("color", "#FF5500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Very Important"))
                .andExpect(jsonPath("$.color").value("#FF5500"));
    }

    @Test
    @WithMockUser
    public void testDeleteTag() throws Exception {
        ApiResponse response = ApiResponse.builder()
                .success(true)
                .message("Tag deleted successfully")
                .build();

        when(tagService.deleteTag(eq(1L))).thenReturn(response);

        mockMvc.perform(delete("/api/v1/tags/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tag deleted successfully"));
    }

    @Test
    @WithMockUser
    public void testAddTagToFile() throws Exception {
        ApiResponse response = ApiResponse.builder()
                .success(true)
                .message("Tag added to file successfully")
                .build();

        when(tagService.addTagToFile(eq(1L), eq(2L))).thenReturn(response);

        mockMvc.perform(post("/api/v1/tags/1/files/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tag added to file successfully"));
    }

    @Test
    @WithMockUser
    public void testRemoveTagFromFile() throws Exception {
        ApiResponse response = ApiResponse.builder()
                .success(true)
                .message("Tag removed from file successfully")
                .build();

        when(tagService.removeTagFromFile(eq(1L), eq(2L))).thenReturn(response);

        mockMvc.perform(delete("/api/v1/tags/1/files/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tag removed from file successfully"));
    }

    @Test
    @WithMockUser
    public void testGetFilesWithTag() throws Exception {
        FileResponse file1 = FileResponse.builder()
                .id(1L)
                .filename("document1.txt")
                .fileType("document")
                .build();

        FileResponse file2 = FileResponse.builder()
                .id(2L)
                .filename("document2.txt")
                .fileType("document")
                .build();

        List<FileResponse> files = Arrays.asList(file1, file2);

        when(tagService.getFilesWithTag(eq(1L))).thenReturn(files);

        mockMvc.perform(get("/api/v1/tags/1/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].filename").value("document1.txt"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].filename").value("document2.txt"));
    }
}