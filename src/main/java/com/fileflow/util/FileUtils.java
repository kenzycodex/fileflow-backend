package com.fileflow.util;

import com.fileflow.exception.BadRequestException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

public class FileUtils {

    private FileUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Generate a unique filename for storing
     *
     * @param originalFilename the original filename
     * @return unique filename with original extension
     */
    public static String generateUniqueFilename(String originalFilename) {
        String extension = FilenameUtils.getExtension(originalFilename);
        return UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);
    }

    /**
     * Determine file type (extension)
     *
     * @param filename the filename
     * @return the file extension
     */
    public static String getFileExtension(String filename) {
        return FilenameUtils.getExtension(filename);
    }

    /**
     * Determine file type from filename
     *
     * @param filename the filename
     * @return the file type category (document, image, video, audio, other)
     */
    public static String determineFileType(String filename) {
        String extension = getFileExtension(filename).toLowerCase();

        if (Arrays.asList("doc", "docx", "pdf", "txt", "rtf", "odt").contains(extension)) {
            return "document";
        } else if (Arrays.asList("xls", "xlsx", "csv").contains(extension)) {
            return "spreadsheet";
        } else if (Arrays.asList("ppt", "pptx").contains(extension)) {
            return "presentation";
        } else if (Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "svg").contains(extension)) {
            return "image";
        } else if (Arrays.asList("mp4", "avi", "mov", "wmv", "flv", "mkv").contains(extension)) {
            return "video";
        } else if (Arrays.asList("mp3", "wav", "ogg", "flac", "aac").contains(extension)) {
            return "audio";
        } else {
            return "other";
        }
    }

    /**
     * Validate file content type
     *
     * @param file the file to validate
     * @param allowedContentTypes comma-separated list of allowed content types (e.g., "image/*,application/pdf")
     * @return true if content type is allowed, false otherwise
     */
    public static boolean isValidContentType(MultipartFile file, String allowedContentTypes) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }

        String[] allowedTypes = allowedContentTypes.split(",");
        for (String type : allowedTypes) {
            type = type.trim();
            if (type.endsWith("/*")) {
                String prefix = type.substring(0, type.length() - 2);
                if (contentType.startsWith(prefix)) {
                    return true;
                }
            } else if (type.equals(contentType)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Create directories if they don't exist
     *
     * @param dirPath directory path to create
     * @throws IOException if an I/O error occurs
     */
    public static void createDirectories(Path dirPath) throws IOException {
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
    }

    /**
     * Sanitize filename to prevent path traversal and other issues
     *
     * @param filename the filename to sanitize
     * @return sanitized filename
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null) {
            return null;
        }

        // Replace any path separators with underscores
        String sanitized = filename.replaceAll("[\\\\/:*?\"<>|]", "_");

        // Limit length to avoid potential issues
        if (sanitized.length() > 255) {
            String extension = FilenameUtils.getExtension(sanitized);
            String basename = FilenameUtils.getBaseName(sanitized);
            basename = basename.substring(0, 250 - extension.length());
            sanitized = basename + (extension.isEmpty() ? "" : "." + extension);
        }

        return sanitized;
    }

    /**
     * Validate file size
     *
     * @param file the file to validate
     * @param maxSizeInBytes maximum allowed size in bytes
     * @throws BadRequestException if file size exceeds the limit
     */
    public static void validateFileSize(MultipartFile file, long maxSizeInBytes) {
        if (file.getSize() > maxSizeInBytes) {
            throw new BadRequestException("File size exceeds the maximum allowed size");
        }
    }
}