package com.fileflow.service.comment;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.common.PagedResponse;
import com.fileflow.model.Comment;

import java.util.List;

public interface CommentService {
    /**
     * Add comment to file
     *
     * @param fileId file ID
     * @param commentText comment text
     * @param parentCommentId optional parent comment ID for replies
     * @return the created comment
     */
    Comment addComment(Long fileId, String commentText, Long parentCommentId);

    /**
     * Get comments for file
     *
     * @param fileId file ID
     * @param page page number
     * @param size page size
     * @param includeReplies whether to include replies
     * @return paged list of comments
     */
    PagedResponse<Comment> getFileComments(Long fileId, int page, int size, boolean includeReplies);

    /**
     * Get replies to comment
     *
     * @param commentId parent comment ID
     * @return list of replies
     */
    List<Comment> getCommentReplies(Long commentId);

    /**
     * Update comment
     *
     * @param commentId comment ID
     * @param commentText new comment text
     * @return the updated comment
     */
    Comment updateComment(Long commentId, String commentText);

    /**
     * Delete comment
     *
     * @param commentId comment ID
     * @return response with deletion information
     */
    ApiResponse deleteComment(Long commentId);

    /**
     * Get comment by ID
     *
     * @param commentId comment ID
     * @return the comment
     */
    Comment getComment(Long commentId);

    /**
     * Get comments by current user
     *
     * @param page page number
     * @param size page size
     * @return paged list of comments
     */
    PagedResponse<Comment> getUserComments(int page, int size);
}