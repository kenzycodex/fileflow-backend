package com.fileflow.service.tag;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.exception.BadRequestException;
import com.fileflow.exception.ResourceNotFoundException;
import com.fileflow.model.File;
import com.fileflow.model.FileTag;
import com.fileflow.model.Tag;
import com.fileflow.model.User;
import com.fileflow.repository.FileRepository;
import com.fileflow.repository.FileTagRepository;
import com.fileflow.repository.TagRepository;
import com.fileflow.repository.UserRepository;
import com.fileflow.security.UserPrincipal;
import com.fileflow.service.activity.ActivityService;
import com.fileflow.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final FileTagRepository fileTagRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;

    @Override
    @Transactional
    public Tag createTag(String name, String color) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("Tag name cannot be empty");
        }

        User currentUser = getCurrentUser();

        // Check if tag with same name already exists for this user
        if (tagRepository.existsByUserAndName(currentUser, name)) {
            throw new BadRequestException("Tag with name '" + name + "' already exists");
        }

        Tag tag = Tag.builder()
                .user(currentUser)
                .name(name)
                .color(color != null ? color : "#3498db") // Default to blue
                .createdAt(LocalDateTime.now())
                .build();

        Tag savedTag = tagRepository.save(tag);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_CREATE_TAG,
                Constants.ITEM_TYPE_TAG,
                savedTag.getId(),
                "Created tag: " + savedTag.getName()
        );

        return savedTag;
    }

    @Override
    public List<Tag> getUserTags() {
        User currentUser = getCurrentUser();
        return tagRepository.findByUser(currentUser);
    }

    @Override
    @Transactional
    public Tag updateTag(Long tagId, String name, String color) {
        User currentUser = getCurrentUser();

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", tagId));

        // Ensure tag belongs to user
        if (!tag.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Tag not found or does not belong to you");
        }

        // Update name if provided
        if (name != null && !name.trim().isEmpty() && !name.equals(tag.getName())) {
            // Check for duplicates
            if (tagRepository.existsByUserAndName(currentUser, name)) {
                throw new BadRequestException("Tag with name '" + name + "' already exists");
            }

            tag.setName(name);
        }

        // Update color if provided
        if (color != null && !color.trim().isEmpty()) {
            tag.setColor(color);
        }

        Tag updatedTag = tagRepository.save(tag);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_UPDATE_TAG,
                Constants.ITEM_TYPE_TAG,
                updatedTag.getId(),
                "Updated tag: " + updatedTag.getName()
        );

        return updatedTag;
    }

    @Override
    @Transactional
    public ApiResponse deleteTag(Long tagId) {
        User currentUser = getCurrentUser();

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", tagId));

        // Ensure tag belongs to user
        if (!tag.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Tag not found or does not belong to you");
        }

        String tagName = tag.getName();

        // Delete tag (file tags will be deleted by cascade)
        tagRepository.delete(tag);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_DELETE_TAG,
                Constants.ITEM_TYPE_TAG,
                null,
                "Deleted tag: " + tagName
        );

        return ApiResponse.builder()
                .success(true)
                .message("Tag deleted successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public ApiResponse addTagToFile(Long tagId, Long fileId) {
        User currentUser = getCurrentUser();

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", tagId));

        // Ensure tag belongs to user
        if (!tag.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Tag not found or does not belong to you");
        }

        File file = fileRepository.findByIdAndUserAndIsDeletedFalse(fileId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        // Check if already tagged
        if (fileTagRepository.findByFileAndTag(file, tag).isPresent()) {
            return ApiResponse.builder()
                    .success(true)
                    .message("File already has this tag")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // Create file tag
        FileTag fileTag = FileTag.builder()
                .file(file)
                .tag(tag)
                .createdAt(LocalDateTime.now())
                .build();

        fileTagRepository.save(fileTag);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_TAG_FILE,
                Constants.ITEM_TYPE_FILE,
                fileId,
                "Added tag '" + tag.getName() + "' to file: " + file.getFilename()
        );

        return ApiResponse.builder()
                .success(true)
                .message("Tag added to file successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public ApiResponse removeTagFromFile(Long tagId, Long fileId) {
        User currentUser = getCurrentUser();

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", tagId));

        // Ensure tag belongs to user
        if (!tag.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Tag not found or does not belong to you");
        }

        File file = fileRepository.findByIdAndUserAndIsDeletedFalse(fileId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        // Find and delete file tag
        FileTag fileTag = fileTagRepository.findByFileAndTag(file, tag)
                .orElseThrow(() -> new BadRequestException("File does not have this tag"));

        fileTagRepository.delete(fileTag);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_UNTAG_FILE,
                Constants.ITEM_TYPE_FILE,
                fileId,
                "Removed tag '" + tag.getName() + "' from file: " + file.getFilename()
        );

        return ApiResponse.builder()
                .success(true)
                .message("Tag removed from file successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public List<FileResponse> getFilesWithTag(Long tagId) {
        User currentUser = getCurrentUser();

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", tagId));

        // Ensure tag belongs to user
        if (!tag.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Tag not found or does not belong to you");
        }

        // Get all file tags for this tag
        List<FileTag> fileTags = fileTagRepository.findByTag(tag);

        // Extract files and map to responses
        return fileTags.stream()
                .map(FileTag::getFile)
                .filter(file -> !file.isDeleted() && file.getUser().getId().equals(currentUser.getId()))
                .map(this::mapFileToFileResponse)
                .collect(Collectors.toList());
    }

    // Helper methods

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private FileResponse mapFileToFileResponse(File file) {
        String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/files/download/")
                .path(file.getId().toString())
                .toUriString();

        return FileResponse.builder()
                .id(file.getId())
                .filename(file.getFilename())
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getFileSize())
                .fileType(file.getFileType())
                .mimeType(file.getMimeType())
                .parentFolderId(file.getParentFolder() != null ? file.getParentFolder().getId() : null)
                .parentFolderName(file.getParentFolder() != null ? file.getParentFolder().getFolderName() : null)
                .isFavorite(file.isFavorite())
                .downloadUrl(downloadUrl)
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .lastAccessed(file.getLastAccessed())
                .owner(file.getUser().getUsername())
                .ownerId(file.getUser().getId())
                .build();
    }
}