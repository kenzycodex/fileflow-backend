package com.fileflow.controller;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.common.PagedResponse;
import com.fileflow.model.Comment;
import com.fileflow.service.comment.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "File comments API")
@SecurityRequirement(name = "bearerAuth")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @Operation(summary = "Add a comment to a file")
    public ResponseEntity<Comment> addComment(
            @RequestParam Long fileId,
            @RequestParam @NotBlank String commentText,
            @RequestParam(required = false) Long parentCommentId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addComment(fileId, commentText, parentCommentId));
    }

    @GetMapping("/files/{fileId}")
    @Operation(summary = "Get comments for a file")
    public ResponseEntity<PagedResponse<Comment>> getFileComments(
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeReplies) {
        return ResponseEntity.ok(commentService.getFileComments(fileId, page, size, includeReplies));
    }

    @GetMapping("/{commentId}/replies")
    @Operation(summary = "Get replies to a comment")
    public ResponseEntity<List<Comment>> getCommentReplies(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.getCommentReplies(commentId));
    }

    @GetMapping("/{commentId}")
    @Operation(summary = "Get a comment by ID")
    public ResponseEntity<Comment> getComment(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.getComment(commentId));
    }

    @PutMapping("/{commentId}")
    @Operation(summary = "Update a comment")
    public ResponseEntity<Comment> updateComment(
            @PathVariable Long commentId,
            @RequestParam @NotBlank String commentText) {
        return ResponseEntity.ok(commentService.updateComment(commentId, commentText));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete a comment")
    public ResponseEntity<ApiResponse> deleteComment(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.deleteComment(commentId));
    }

    @GetMapping("/me")
    @Operation(summary = "Get comments by current user")
    public ResponseEntity<PagedResponse<Comment>> getUserComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(commentService.getUserComments(page, size));
    }
}
