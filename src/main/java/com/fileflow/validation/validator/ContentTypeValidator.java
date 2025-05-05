package com.fileflow.validation.validator;

import com.fileflow.validation.annotation.ValidContentType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Validator for file content types
 */
public class ContentTypeValidator implements ConstraintValidator<ValidContentType, MultipartFile> {

    private Set<String> allowedTypes;
    private boolean allowImages;
    private boolean allowDocuments;
    private boolean allowVideos;
    private boolean allowAudio;

    // Common content types
    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/svg+xml", "image/webp"
    );

    private static final Set<String> DOCUMENT_TYPES = Set.of(
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/rtf", "text/plain"
    );

    private static final Set<String> VIDEO_TYPES = Set.of(
            "video/mp4", "video/mpeg", "video/quicktime", "video/x-msvideo", "video/webm"
    );

    private static final Set<String> AUDIO_TYPES = Set.of(
            "audio/mpeg", "audio/x-wav", "audio/ogg", "audio/aac", "audio/flac"
    );

    @Override
    public void initialize(ValidContentType annotation) {
        this.allowedTypes = new HashSet<>(Arrays.asList(annotation.allowed()));
        this.allowImages = annotation.allowImages();
        this.allowDocuments = annotation.allowDocuments();
        this.allowVideos = annotation.allowVideos();
        this.allowAudio = annotation.allowAudio();
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            // Null/empty files are handled by @NotNull if required
            return true;
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }

        // Check specific allowed types
        if (allowedTypes.contains(contentType)) {
            return true;
        }

        // Check category-based allowed types
        if (allowImages && IMAGE_TYPES.contains(contentType)) {
            return true;
        }

        if (allowDocuments && DOCUMENT_TYPES.contains(contentType)) {
            return true;
        }

        if (allowVideos && VIDEO_TYPES.contains(contentType)) {
            return true;
        }

        if (allowAudio && AUDIO_TYPES.contains(contentType)) {
            return true;
        }

        // If specific types and categories are specified but none match
        if (!allowedTypes.isEmpty() || allowImages || allowDocuments || allowVideos || allowAudio) {
            return false;
        }

        // If no restrictions specified, allow all
        return true;
    }
}