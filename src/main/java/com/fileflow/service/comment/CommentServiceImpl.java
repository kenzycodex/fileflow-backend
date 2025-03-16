package com.fileflow.service.comment;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.common.PagedResponse;
import com.fileflow.exception.BadRequestException;
import com.fileflow.exception.ForbiddenException;
import com.fileflow.exception.ResourceNotFoundException;
import com.fileflow.model.Comment;
import com.fileflow.model.File;
import com.fileflow.model.User;
import com.fileflow.repository.CommentRepository;
import com.fileflow.repository.FileRepository;
import com.fileflow.repository.UserRepository;
import com.fileflow.security.UserPrincipal;
import com.fileflow.service.activity.ActivityService;
import com.fileflow.service.share.ShareService;
import com.fileflow.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final ShareService shareService;
    private final ActivityService activityService;

    @Override
    @Transactional
    public Comment addComment(Long fileId, String commentText, Long parentCommentId) {
        if (commentText == null || commentText.trim().isEmpty()) {
            throw new BadRequestException("Comment text cannot be empty");
        }

        User currentUser = getCurrentUser();

        // Get file
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        // Check if user has access to file
        if (!file.getUser().getId().equals(currentUser.getId()) &&
                !shareService.isSharedWithUser(fileId, com.fileflow.model.Share.ItemType.FILE, currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to comment on this file");
        }

        // Handle parent comment if provided
        Comment parentComment = null;
        if (parentCommentId != null) {
            parentComment = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", parentCommentId));

            // Ensure parent comment belongs to the same file
            if (!parentComment.getFile().getId().equals(fileId)) {
                throw new BadRequestException("Parent comment does not belong to this file");
            }
        }

        // Create comment
        Comment comment = Comment.builder()
                .file(file)
                .user(currentUser)
                .commentText(commentText)
                .parentComment(parentComment)
                .createdAt(LocalDateTime.now())
                .build();

        Comment savedComment = commentRepository.save(comment);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_COMMENT,
                Constants.ITEM_TYPE_FILE,
                fileId,
                "Added comment to file: " + file.getFilename()
        );

        return savedComment;
    }

    @Override
    public PagedResponse<Comment> getFileComments(Long fileId, int page, int size, boolean includeReplies) {
        User currentUser = getCurrentUser();
        validatePageNumberAndSize(page, size);

        // Get file
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        // Check if user has access to file
        if (!file.getUser().getId().equals(currentUser.getId()) &&
                !shareService.isSharedWithUser(fileId, com.fileflow.model.Share.ItemType.FILE, currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to view comments on this file");
        }

        // Get comments
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Comment> comments;

        if (includeReplies) {
            comments = commentRepository.findByFile(file, pageable);
        } else {
            comments = commentRepository.findByFileAndParentCommentIsNull(file, pageable);
        }

        if (comments.getNumberOfElements() == 0) {
            return new PagedResponse<>(Collections.emptyList(), comments.getNumber(),
                    comments.getSize(), comments.getTotalElements(), comments.getTotalPages(), comments.isLast());
        }

        return new PagedResponse<>(comments.getContent(), comments.getNumber(),
                comments.getSize(), comments.getTotalElements(), comments.getTotalPages(), comments.isLast());
    }

    @Override
    public List<Comment> getCommentReplies(Long commentId) {
        User currentUser = getCurrentUser();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        File file = comment.getFile();

        // Check if user has access to file
        if (!file.getUser().getId().equals(currentUser.getId()) &&
                !shareService.isSharedWithUser(file.getId(), com.fileflow.model.Share.ItemType.FILE, currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to view comments on this file");
        }

        return commentRepository.findByParentComment(comment);
    }

    @Override
    @Transactional
    public Comment updateComment(Long commentId, String commentText) {
        if (commentText == null || commentText.trim().isEmpty()) {
            throw new BadRequestException("Comment text cannot be empty");
        }

        User currentUser = getCurrentUser();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        // Ensure comment belongs to user
        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to update this comment");
        }

        comment.setCommentText(commentText);
        comment.setUpdatedAt(LocalDateTime.now());

        Comment updatedComment = commentRepository.save(comment);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_UPDATE_COMMENT,
                Constants.ITEM_TYPE_FILE,
                comment.getFile().getId(),
                "Updated comment on file: " + comment.getFile().getFilename()
        );

        return updatedComment;
    }

    @Override
    @Transactional
    public ApiResponse deleteComment(Long commentId) {
        User currentUser = getCurrentUser();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        // Ensure comment belongs to user or user is file owner
        if (!comment.getUser().getId().equals(currentUser.getId()) &&
                !comment.getFile().getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to delete this comment");
        }

        // Get file info for logging
        Long fileId = comment.getFile().getId();
        String filename = comment.getFile().getFilename();

        // Delete replies first
        List<Comment> replies = commentRepository.findByParentComment(comment);
        if (!replies.isEmpty()) {
            commentRepository.deleteAll(replies);
        }

        // Delete comment
        commentRepository.delete(comment);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_DELETE_COMMENT,
                Constants.ITEM_TYPE_FILE,
                fileId,
                "Deleted comment from file: " + filename
        );

        return ApiResponse.builder()
                .success(true)
                .message("Comment deleted successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public Comment getComment(Long commentId) {
        User currentUser = getCurrentUser();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        File file = comment.getFile();

        // Check if user has access to file
        if (!file.getUser().getId().equals(currentUser.getId()) &&
                !shareService.isSharedWithUser(file.getId(), com.fileflow.model.Share.ItemType.FILE, currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to view this comment");
        }

        return comment;
    }

    @Override
    public PagedResponse<Comment> getUserComments(int page, int size) {
        User currentUser = getCurrentUser();
        validatePageNumberAndSize(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Comment> comments = commentRepository.findByUser(currentUser, pageable);

        if (comments.getNumberOfElements() == 0) {
            return new PagedResponse<>(Collections.emptyList(), comments.getNumber(),
                    comments.getSize(), comments.getTotalElements(), comments.getTotalPages(), comments.isLast());
        }

        return new PagedResponse<>(comments.getContent(), comments.getNumber(),
                comments.getSize(), comments.getTotalElements(), comments.getTotalPages(), comments.isLast());
    }

    // Helper methods

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void validatePageNumberAndSize(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page number cannot be less than zero.");
        }

        if (size < 1) {
            throw new BadRequestException("Page size must not be less than one.");
        }

        if (size > 100) {
            throw new BadRequestException("Page size must not be greater than 100.");
        }
    }
}