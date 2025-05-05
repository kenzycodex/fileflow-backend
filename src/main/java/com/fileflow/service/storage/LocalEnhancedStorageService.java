package com.fileflow.service.storage;

import com.fileflow.config.StorageConfig;
import com.fileflow.exception.StorageException;
import com.fileflow.model.StorageChunk;
import com.fileflow.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Enhanced local storage service implementation with support for chunked uploads,
 * file previews, and other advanced features.
 */
@Service
@Profile("!prod")
@Slf4j
public class LocalEnhancedStorageService implements EnhancedStorageService {

    private final Path rootLocation;
    private final Path previewsLocation;
    private final Path thumbnailsLocation;
    private final Path tempLocation;

    // Buffer size for file operations
    private static final int BUFFER_SIZE = 8192;

    // Maximum supported preview dimensions
    private static final int MAX_PREVIEW_WIDTH = 800;
    private static final int MAX_PREVIEW_HEIGHT = 600;

    // Maximum supported thumbnail dimensions
    private static final int MAX_THUMBNAIL_WIDTH = 200;
    private static final int MAX_THUMBNAIL_HEIGHT = 200;

    @Autowired
    public LocalEnhancedStorageService(StorageConfig storageConfig) {
        this.rootLocation = storageConfig.fileStorageLocation();
        this.previewsLocation = storageConfig.fileStorageLocation().resolve("previews");
        this.thumbnailsLocation = storageConfig.fileStorageLocation().resolve("thumbnails");
        this.tempLocation = storageConfig.fileStorageLocation().resolve("temp");
    }

    @PostConstruct
    @Override
    public void init() {
        try {
            // Create storage directories if they don't exist
            FileUtils.createDirectories(rootLocation);
            FileUtils.createDirectories(previewsLocation);
            FileUtils.createDirectories(thumbnailsLocation);
            FileUtils.createDirectories(tempLocation);

            log.info("Storage initialized at: {}", rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }

    // Rest of your existing implementation...
    // Keep all the other methods exactly as they were

    @Override
    public String store(MultipartFile file, String directory) throws IOException {
        return store(file, FileUtils.generateUniqueFilename(file.getOriginalFilename()), directory);
    }

    @Override
    public String store(MultipartFile file, String filename, String directory) throws IOException {
        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file " + filename);
        }

        String sanitizedFilename = FileUtils.sanitizeFilename(filename);
        Path targetLocation = getTargetLocation(directory, sanitizedFilename);

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        log.debug("File stored at: {}", targetLocation);

        // Return the path relative to the root location
        return getRelativePath(directory, sanitizedFilename);
    }

    @Override
    public String store(InputStream inputStream, int contentLength, String filename, String directory) throws IOException {
        if (inputStream == null) {
            throw new StorageException("Failed to store file from null input stream");
        }

        String sanitizedFilename = FileUtils.sanitizeFilename(filename);
        Path targetLocation = getTargetLocation(directory, sanitizedFilename);

        Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        log.debug("File stored from input stream at: {}", targetLocation);

        // Return the path relative to the root location
        return getRelativePath(directory, sanitizedFilename);
    }

    @Override
    public InputStream getInputStream(String filename) throws IOException {
        Path file = rootLocation.resolve(filename);

        if (!Files.exists(file)) {
            throw new StorageException("Could not read file: " + filename);
        }

        try {
            return new BufferedInputStream(Files.newInputStream(file), BUFFER_SIZE);
        } catch (IOException e) {
            throw new StorageException("Could not get input stream for file: " + filename, e);
        }
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new StorageException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new StorageException("Could not read file: " + filename, e);
        }
    }

    @Override
    public String generatePresignedUrl(String filename, int expiryTimeInMinutes) {
        // Local implementation doesn't need pre-signed URLs since files are served directly
        // Just return the file path that would be used in a controller
        return "/api/v1/files/download/" + filename;
    }

    @Override
    public boolean delete(String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            log.error("Error deleting file: {}", filename, e);
            return false;
        }
    }

    @Override
    public Stream<Path> loadAll(String directory) {
        Path dirPath = directory == null ? rootLocation : rootLocation.resolve(directory);

        try {
            return Files.walk(dirPath, 1)
                    .filter(path -> !path.equals(dirPath))
                    .map(dirPath::relativize);
        } catch (IOException e) {
            throw new StorageException("Failed to read stored files", e);
        }
    }

    @Override
    public boolean exists(String filename) {
        Path file = rootLocation.resolve(filename);
        return Files.exists(file);
    }

    @Override
    public boolean copy(String source, String destination) {
        try {
            Path sourcePath = rootLocation.resolve(source);
            Path destinationPath = rootLocation.resolve(destination);

            // Create parent directories if they don't exist
            FileUtils.createDirectories(destinationPath.getParent());

            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Copied file from {} to {}", source, destination);
            return true;
        } catch (IOException e) {
            log.error("Error copying file from {} to {}", source, destination, e);
            return false;
        }
    }

    @Override
    public boolean move(String source, String destination) {
        try {
            Path sourcePath = rootLocation.resolve(source);
            Path destinationPath = rootLocation.resolve(destination);

            // Create parent directories if they don't exist
            FileUtils.createDirectories(destinationPath.getParent());

            Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Moved file from {} to {}", source, destination);
            return true;
        } catch (IOException e) {
            log.error("Error moving file from {} to {}", source, destination, e);
            return false;
        }
    }

    @Override
    public String mergeChunks(List<StorageChunk> chunks, String filename, String directory) throws IOException {
        // Ensure chunks are sorted by chunk number
        chunks.sort(Comparator.comparing(StorageChunk::getChunkNumber));

        // Create target file path
        String sanitizedFilename = FileUtils.sanitizeFilename(filename);
        Path targetLocation = getTargetLocation(directory, sanitizedFilename);

        // Create output stream for merged file
        try (OutputStream outputStream = new BufferedOutputStream(
                new FileOutputStream(targetLocation.toFile()), BUFFER_SIZE)) {

            // Merge all chunks into the target file
            for (StorageChunk chunk : chunks) {
                Path chunkPath = rootLocation.resolve(chunk.getStoragePath());

                // Check if chunk exists
                if (!Files.exists(chunkPath)) {
                    throw new StorageException("Chunk not found: " + chunk.getStoragePath());
                }

                // Read chunk and write to target file
                try (InputStream inputStream = new BufferedInputStream(
                        new FileInputStream(chunkPath.toFile()), BUFFER_SIZE)) {

                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
            }
        }

        log.info("Merged {} chunks into file: {}", chunks.size(), targetLocation);

        // Return the path relative to the root location
        return getRelativePath(directory, sanitizedFilename);
    }

    @Override
    public String generatePreview(String storagePath, String fileType, String mimeType) throws IOException {
        // In a real implementation, this would use libraries like Apache PDFBox, POI, etc.
        // or call external services to generate previews

        Path sourcePath = rootLocation.resolve(storagePath);
        if (!Files.exists(sourcePath)) {
            throw new StorageException("File not found: " + storagePath);
        }

        // Generate a unique name for the preview file
        String extension = getPreviewExtension(fileType, mimeType);
        String previewFilename = FilenameUtils.getBaseName(storagePath) + "_preview." + extension;
        Path previewPath = previewsLocation.resolve(previewFilename);

        // For now, just copy the original file for image files,
        // for other types we'd need actual conversion logic
        if (fileType.equals("image")) {
            Files.copy(sourcePath, previewPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Generated preview for image: {}", previewPath);
        } else {
            // In a real implementation, we'd call appropriate converters here
            log.warn("Preview generation not implemented for file type: {}", fileType);
            return null;
        }

        // Return the path relative to the root location
        return "previews/" + previewFilename;
    }

    @Override
    public String generateThumbnail(String storagePath, String fileType, String mimeType) throws IOException {
        // Similar to preview generation, but for thumbnails
        // In a real implementation, would use libraries like Thumbnailator, ImageMagick, etc.

        Path sourcePath = rootLocation.resolve(storagePath);
        if (!Files.exists(sourcePath)) {
            throw new StorageException("File not found: " + storagePath);
        }

        // Generate a unique name for the thumbnail
        String thumbnailFilename = FilenameUtils.getBaseName(storagePath) + "_thumbnail.jpg";
        Path thumbnailPath = thumbnailsLocation.resolve(thumbnailFilename);

        // For now, just copy the original file for image files
        if (fileType.equals("image")) {
            Files.copy(sourcePath, thumbnailPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Generated thumbnail for image: {}", thumbnailPath);
        } else {
            // In a real implementation, we'd generate thumbnails for different file types
            log.warn("Thumbnail generation not implemented for file type: {}", fileType);
            return null;
        }

        // Return the path relative to the root location
        return "thumbnails/" + thumbnailFilename;
    }

    @Override
    public String convertFile(String storagePath, String targetFormat) throws IOException {
        // Convert file to a different format
        // In a real implementation, would use libraries or external services

        Path sourcePath = rootLocation.resolve(storagePath);
        if (!Files.exists(sourcePath)) {
            throw new StorageException("File not found: " + storagePath);
        }

        // Generate a filename for the converted file
        String convertedFilename = FilenameUtils.getBaseName(storagePath) + "." + targetFormat;
        Path convertedPath = tempLocation.resolve(convertedFilename);

        // For now, just copy the original file (placeholder for actual conversion)
        Files.copy(sourcePath, convertedPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Converted file to {}: {}", targetFormat, convertedPath);

        // Return the path relative to the root location
        return "temp/" + convertedFilename;
    }

    @Override
    public String extractText(String storagePath, String mimeType) throws IOException {
        // Extract text from file for search indexing
        // In a real implementation, would use libraries like Apache Tika, PDFBox, POI, etc.

        Path sourcePath = rootLocation.resolve(storagePath);
        if (!Files.exists(sourcePath)) {
            throw new StorageException("File not found: " + storagePath);
        }

        // For now, just return a placeholder message
        log.info("Text extraction requested for: {}", storagePath);
        return "Text extraction not implemented in this version";
    }

    @Override
    public String generateUploadUrl(String filename, String contentType, int expiryTimeInMinutes) {
        // Generate a pre-signed URL for direct upload
        // In a local implementation, not really needed

        return "/api/v1/files/upload/direct";
    }

    @Override
    public String computeHash(MultipartFile file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new StorageException("Error computing file hash", e);
        }
    }

    @Override
    public String computeHash(String storagePath) throws IOException {
        Path filePath = rootLocation.resolve(storagePath);
        if (!Files.exists(filePath)) {
            throw new StorageException("File not found: " + storagePath);
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hash = digest.digest(fileBytes);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new StorageException("Error computing file hash", e);
        }
    }

    // Helper methods

    private Path getTargetLocation(String directory, String filename) throws IOException {
        Path dirPath = directory == null ? rootLocation : rootLocation.resolve(directory);

        // Create directories if they don't exist
        FileUtils.createDirectories(dirPath);

        return dirPath.resolve(filename);
    }

    private String getRelativePath(String directory, String filename) {
        return directory == null ? filename : Paths.get(directory, filename).toString();
    }

    private String getPreviewExtension(String fileType, String mimeType) {
        // Determine appropriate preview format based on file type
        switch (fileType) {
            case "document":
            case "spreadsheet":
            case "presentation":
                return "pdf";
            case "image":
                return "jpg";
            case "video":
                return "jpg"; // Thumbnail for video
            case "audio":
                return "png"; // Waveform image for audio
            default:
                return "png";
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}