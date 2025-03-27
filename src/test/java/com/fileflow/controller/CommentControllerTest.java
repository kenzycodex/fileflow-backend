package com.fileflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileflow.config.TestSecurityConfig;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.common.PagedResponse;
import com.fileflow.model.Comment;
import com.fileflow.model.File;
import com.fileflow.model.User;
import com.fileflow.security.JwtAuthenticationFilter;
import com.fileflow.service.comment.CommentService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@Import(TestSecurityConfig.class)
public class CommentControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private File testFile;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testFile = new File();
        testFile.setId(1L);
        testFile.setFilename("test.txt");
        testFile.setUser(testUser);
    }

    @Test
    @WithMockUser
    public void testAddComment() throws Exception {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setCommentText("This is a test comment");
        comment.setFile(testFile);
        comment.setUser(testUser);
        comment.setCreatedAt(LocalDateTime.now());

        when(commentService.addComment(eq(1L), eq("This is a test comment"), isNull()))
                .thenReturn(comment);

        mockMvc.perform(post("/api/v1/comments")
                        .param("fileId", "1")
                        .param("commentText", "This is a test comment"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.commentText").value("This is a test comment"));
    }

    @Test
    @WithMockUser
    public void testAddReplyComment() throws Exception {
        Comment parentComment = new Comment();
        parentComment.setId(1L);
        parentComment.setCommentText("Parent comment");
        parentComment.setFile(testFile);
        parentComment.setUser(testUser);

        Comment replyComment = new Comment();
        replyComment.setId(2L);
        replyComment.setCommentText("This is a reply");
        replyComment.setFile(testFile);
        replyComment.setUser(testUser);
        replyComment.setParentComment(parentComment);
        replyComment.setCreatedAt(LocalDateTime.now());

        when(commentService.addComment(eq(1L), eq("This is a reply"), eq(1L)))
                .thenReturn(replyComment);

        mockMvc.perform(post("/api/v1/comments")
                        .param("fileId", "1")
                        .param("commentText", "This is a reply")
                        .param("parentCommentId", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.commentText").value("This is a reply"));
    }

    @Test
    @WithMockUser
    public void testGetFileComments() throws Exception {
        Comment comment1 = new Comment();
        comment1.setId(1L);
        comment1.setCommentText("Comment 1");
        comment1.setFile(testFile);
        comment1.setUser(testUser);
        comment1.setCreatedAt(LocalDateTime.now());

        Comment comment2 = new Comment();
        comment2.setId(2L);
        comment2.setCommentText("Comment 2");
        comment2.setFile(testFile);
        comment2.setUser(testUser);
        comment2.setCreatedAt(LocalDateTime.now());

        PagedResponse<Comment> response = PagedResponse.<Comment>builder()
                .content(Arrays.asList(comment1, comment2))
                .page(0)
                .size(20)
                .totalElements(2)
                .totalPages(1)
                .last(true)
                .build();

        when(commentService.getFileComments(eq(1L), anyInt(), anyInt(), eq(false)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/comments/files/1")
                        .param("page", "0")
                        .param("size", "20")
                        .param("includeReplies", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].commentText").value("Comment 1"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].commentText").value("Comment 2"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser
    public void testGetCommentReplies() throws Exception {
        Comment parentComment = new Comment();
        parentComment.setId(1L);
        parentComment.setCommentText("Parent comment");
        parentComment.setFile(testFile);
        parentComment.setUser(testUser);

        Comment reply1 = new Comment();
        reply1.setId(2L);
        reply1.setCommentText("Reply 1");
        reply1.setFile(testFile);
        reply1.setUser(testUser);
        reply1.setParentComment(parentComment);
        reply1.setCreatedAt(LocalDateTime.now());

        Comment reply2 = new Comment();
        reply2.setId(3L);
        reply2.setCommentText("Reply 2");
        reply2.setFile(testFile);
        reply2.setUser(testUser);
        reply2.setParentComment(parentComment);
        reply2.setCreatedAt(LocalDateTime.now());

        List<Comment> replies = Arrays.asList(reply1, reply2);

        when(commentService.getCommentReplies(eq(1L))).thenReturn(replies);

        mockMvc.perform(get("/api/v1/comments/1/replies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].commentText").value("Reply 1"))
                .andExpect(jsonPath("$[1].id").value(3))
                .andExpect(jsonPath("$[1].commentText").value("Reply 2"));
    }

    @Test
    @WithMockUser
    public void testGetComment() throws Exception {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setCommentText("Test comment");
        comment.setFile(testFile);
        comment.setUser(testUser);
        comment.setCreatedAt(LocalDateTime.now());

        when(commentService.getComment(eq(1L))).thenReturn(comment);

        mockMvc.perform(get("/api/v1/comments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.commentText").value("Test comment"));
    }

    @Test
    @WithMockUser
    public void testUpdateComment() throws Exception {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setCommentText("Updated comment");
        comment.setFile(testFile);
        comment.setUser(testUser);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        when(commentService.updateComment(eq(1L), eq("Updated comment")))
                .thenReturn(comment);

        mockMvc.perform(put("/api/v1/comments/1")
                        .param("commentText", "Updated comment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.commentText").value("Updated comment"));
    }

    @Test
    @WithMockUser
    public void testDeleteComment() throws Exception {
        ApiResponse response = ApiResponse.builder()
                .success(true)
                .message("Comment deleted successfully")
                .build();

        when(commentService.deleteComment(eq(1L))).thenReturn(response);

        mockMvc.perform(delete("/api/v1/comments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Comment deleted successfully"));
    }

    @Test
    @WithMockUser
    public void testGetUserComments() throws Exception {
        Comment comment1 = new Comment();
        comment1.setId(1L);
        comment1.setCommentText("My comment 1");
        comment1.setFile(testFile);
        comment1.setUser(testUser);
        comment1.setCreatedAt(LocalDateTime.now());

        Comment comment2 = new Comment();
        comment2.setId(2L);
        comment2.setCommentText("My comment 2");
        comment2.setFile(testFile);
        comment2.setUser(testUser);
        comment2.setCreatedAt(LocalDateTime.now());

        PagedResponse<Comment> response = PagedResponse.<Comment>builder()
                .content(Arrays.asList(comment1, comment2))
                .page(0)
                .size(20)
                .totalElements(2)
                .totalPages(1)
                .last(true)
                .build();

        when(commentService.getUserComments(anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/v1/comments/me")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].commentText").value("My comment 1"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].commentText").value("My comment 2"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }
}