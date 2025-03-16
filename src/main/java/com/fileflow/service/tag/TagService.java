package com.fileflow.service.tag;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.model.Tag;

import java.util.List;

public interface TagService {
    /**
     * Create a new tag
     *
     * @param name tag name
     * @param color optional color (hex code)
     * @return the created tag
     */
    Tag createTag(String name, String color);

    /**
     * Get all tags for current user
     *
     * @return list of tags
     */
    List<Tag> getUserTags();

    /**
     * Update tag
     *
     * @param tagId tag ID
     * @param name new name
     * @param color new color
     * @return the updated tag
     */
    Tag updateTag(Long tagId, String name, String color);

    /**
     * Delete tag
     *
     * @param tagId tag ID
     * @return response with deletion information
     */
    ApiResponse deleteTag(Long tagId);

    /**
     * Add tag to file
     *
     * @param tagId tag ID
     * @param fileId file ID
     * @return response with tagging information
     */
    ApiResponse addTagToFile(Long tagId, Long fileId);

    /**
     * Remove tag from file
     *
     * @param tagId tag ID
     * @param fileId file ID
     * @return response with tagging information
     */
    ApiResponse removeTagFromFile(Long tagId, Long fileId);

    /**
     * Get files with tag
     *
     * @param tagId tag ID
     * @return list of files with the tag
     */
    List<FileResponse> getFilesWithTag(Long tagId);
}