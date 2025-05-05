package com.fileflow.service.storage;

import com.fileflow.exception.StorageException;
import com.fileflow.model.StorageChunk;
import com.fileflow.util.FileUtils;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@Profile("prod")
@Slf4j
public class MinioEnhancedStorageService implements EnhancedStorageService {

    private final MinioClient minioClient;

    @Value("${app.minio.bucket}")
    private String bucketName;

    @Value("${app.minio.previews-prefix:previews/}")
    private String previewsPrefix;

    @Value("${app.minio.thumbnails-prefix:thumbnails/}")
    private String thumbnailsPrefix;

    @Value("${app.minio.temp-prefix:temp/}")
    private String tempPrefix;

    @Autowired
    public MinioEnhancedStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @PostConstruct
    @Override
    public void init() {
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());

            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("Created MinIO bucket: {}", bucketName);
            }
        } catch (Exception e) {
            throw new StorageException("Could not initialize MinIO storage", e);
        }
    }

    // Keep all the other methods exactly as they were in your original code
    // I'm not repeating them here for brevity, but they remain unchanged

    // Implementation methods from your original code
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
        String objectName = getObjectName(directory, sanitizedFilename);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .contentType(file.getContentType())
                    .stream(inputStream, file.getSize(), -1)
                    .build());

            return objectName;
        } catch (Exception e) {
            throw new StorageException("Failed to store file " + filename, e);
        }
    }

    @Override
    public String store(InputStream inputStream, int contentLength, String filename, String directory) throws IOException {
        if (inputStream == null) {
            throw new StorageException("Failed to store file from null input stream");
        }

        String sanitizedFilename = FileUtils.sanitizeFilename(filename);
        String objectName = getObjectName(directory, sanitizedFilename);

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, contentLength, -1)
                    .build());

            return objectName;
        } catch (Exception e) {
            throw new StorageException("Failed to store file from input stream " + filename, e);
        }
    }

    @Override
    public InputStream getInputStream(String filename) throws IOException {
        try {
            GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(filename)
                    .build());

            return response;
        } catch (Exception e) {
            throw new StorageException("Could not get input stream for file: " + filename, e);
        }
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(filename)
                    .build());

            byte[] content = IOUtils.toByteArray(response);
            return new ByteArrayResource(content);
        } catch (Exception e) {
            throw new StorageException("Could not read file: " + filename, e);
        }
    }

    @Override
    public String generatePresignedUrl(String filename, int expiryTimeInMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(filename)
                    .method(Method.GET)
                    .expiry(expiryTimeInMinutes, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Could not generate presigned URL for: " + filename, e);
        }
    }

    @Override
    public boolean delete(String filename) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(filename)
                    .build());
            return true;
        } catch (Exception e) {
            log.error("Error deleting file: {}", filename, e);
            return false;
        }
    }

    @Override
    public Stream<Path> loadAll(String directory) {
        String prefix = directory == null ? "" : directory + "/";

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .recursive(false)
                    .build());

            return StreamSupport.stream(results.spliterator(), false)
                    .map(itemResult -> {
                        try {
                            Item item = itemResult.get();
                            return Paths.get(item.objectName());
                        } catch (Exception e) {
                            throw new StorageException("Failed to list files", e);
                        }
                    });
        } catch (Exception e) {
            throw new StorageException("Failed to list files", e);
        }
    }

    @Override
    public boolean exists(String filename) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(filename)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean copy(String source, String destination) {
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(bucketName)
                    .object(destination)
                    .source(CopySource.builder()
                            .bucket(bucketName)
                            .object(source)
                            .build())
                    .build());
            return true;
        } catch (Exception e) {
            log.error("Error copying file from {} to {}", source, destination, e);
            return false;
        }
    }

    @Override
    public boolean move(String source, String destination) {
        if (copy(source, destination)) {
            return delete(source);
        }
        return false;
    }

    @Override
    public String mergeChunks(List<StorageChunk> chunks, String filename, String directory) throws IOException {
        // Ensure chunks are sorted by chunk number
        chunks.sort(Comparator.comparing(StorageChunk::getChunkNumber));

        // Create a byte array output stream to combine all chunks
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Merge all chunks
        for (StorageChunk chunk : chunks) {
            try {
                GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(chunk.getStoragePath())
                        .build());

                // Copy chunk data to the output stream
                IOUtils.copy(response, outputStream);
            } catch (Exception e) {
                throw new StorageException("Error reading chunk: " + chunk.getStoragePath(), e);
            }
        }

        // Create merged file from the combined chunks
        String sanitizedFilename = FileUtils.sanitizeFilename(filename);
        String objectName = getObjectName(directory, sanitizedFilename);

        try {
            byte[] mergedData = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(mergedData);

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, mergedData.length, -1)
                    .build());

            log.info("Merged {} chunks into file: {}", chunks.size(), objectName);
            return objectName;
        } catch (Exception e) {
            throw new StorageException("Error storing merged file: " + filename, e);
        } finally {
            outputStream.close();
        }
    }

    @Override
    public String generatePreview(String storagePath, String fileType, String mimeType) throws IOException {
        // In a real implementation, this would call a service to generate previews

        if (!exists(storagePath)) {
            throw new StorageException("File not found: " + storagePath);
        }

        // Generate a unique name for the preview
        String extension = getPreviewExtension(fileType, mimeType);
        String baseName = FilenameUtils.getBaseName(storagePath);
        String previewFilename = baseName + "_preview." + extension;
        String previewObjectName = previewsPrefix + previewFilename;

        // For now, just copy the original file for image files
        if (fileType.equals("image")) {
            if (copy(storagePath, previewObjectName)) {
                log.info("Generated preview for image: {}", previewObjectName);
                return previewObjectName;
            }
        } else {
            // In a real implementation, we'd generate previews for different file types
            log.warn("Preview generation not implemented for file type: {}", fileType);
        }

        return null;
    }

    @Override
    public String generateThumbnail(String storagePath, String fileType, String mimeType) throws IOException {
        // Similar to preview generation, but for thumbnails
        // In a real implementation, would call a service to generate thumbnails

        if (!exists(storagePath)) {
            throw new StorageException("File not found: " + storagePath);
        }

        // Generate a unique name for the thumbnail
        String baseName = FilenameUtils.getBaseName(storagePath);
        String thumbnailFilename = baseName + "_thumbnail.jpg";
        String thumbnailObjectName = thumbnailsPrefix + thumbnailFilename;

        // For now, just copy the original file for image files
        if (fileType.equals("image")) {
            if (copy(storagePath, thumbnailObjectName)) {
                log.info("Generated thumbnail for image: {}", thumbnailObjectName);
                return thumbnailObjectName;
            }
        } else {
            // In a real implementation, we'd generate thumbnails for different file types
            log.warn("Thumbnail generation not implemented for file type: {}", fileType);
        }

        return null;
    }

    @Override
    public String convertFile(String storagePath, String targetFormat) throws IOException {
        // Convert file to a different format
        // In a real implementation, would call a service to convert files

        if (!exists(storagePath)) {
            throw new StorageException("File not found: " + storagePath);
        }

        // Generate a filename for the converted file
        String baseName = FilenameUtils.getBaseName(storagePath);
        String convertedFilename = baseName + "." + targetFormat;
        String convertedObjectName = tempPrefix + convertedFilename;

        // For now, just copy the original file (placeholder for actual conversion)
        if (copy(storagePath, convertedObjectName)) {
            log.info("Converted file to {}: {}", targetFormat, convertedObjectName);
            return convertedObjectName;
        }

        return null;
    }

    @Override
    public String extractText(String storagePath, String mimeType) throws IOException {
        // Extract text from file for search indexing
        // In a real implementation, would call a service to extract text

        if (!exists(storagePath)) {
            throw new StorageException("File not found: " + storagePath);
        }

        // For now, just return a placeholder message
        log.info("Text extraction requested for: {}", storagePath);
        return "Text extraction not implemented in this version";
    }

    @Override
    public String generateUploadUrl(String filename, String contentType, int expiryTimeInMinutes) {
        try {
            String sanitizedFilename = FileUtils.sanitizeFilename(filename);

            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(sanitizedFilename)
                    .method(Method.PUT)
                    .expiry(expiryTimeInMinutes, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Could not generate upload URL for: " + filename, e);
        }
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
        try {
            GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(storagePath)
                    .build());

            byte[] fileBytes = IOUtils.toByteArray(response);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileBytes);
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new StorageException("Error computing file hash for: " + storagePath, e);
        }
    }

    // Helper methods

    private String getObjectName(String directory, String filename) {
        return directory == null ? filename : directory + "/" + filename;
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