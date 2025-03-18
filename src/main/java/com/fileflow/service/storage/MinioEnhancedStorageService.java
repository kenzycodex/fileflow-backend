package com.fileflow.service.storage;

import com.fileflow.config.StorageConfig;
import com.fileflow.exception.StorageException;
import com.fileflow.model.StorageChunk;
import com.fileflow.util.FileUtils;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Implementation of EnhancedStorageService using MinIO object storage
 */
@Service
@Profile("minio")
@RequiredArgsConstructor
@Slf4j
public class MinioEnhancedStorageService implements EnhancedStorageService {

    private final MinioClient minioClient;
    private final StorageConfig storageConfig;

    // The bucket name to use for file storage
    private String bucketName;

    // Temporary directory for processing files
    private final Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "fileflow-temp");

    // Buffer size for file operations
    private static final int BUFFER_SIZE = 8192;

    @PostConstruct
    @Override
    public void init() {
        try {
            // Create temp directory if it doesn't exist
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }

            // Set bucket name from config
            this.bucketName = System.getProperty("app.minio.bucket", "fileflow");

            // Check if bucket exists, create if it doesn't
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());

            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());

                log.info("Created MinIO bucket: {}", bucketName);
            }

            log.info("MinIO storage initialized with bucket: {}", bucketName);
        } catch (Exception e) {
            throw new StorageException("Could not initialize MinIO storage", e);
        }
    }

    @Override
    public String store(MultipartFile file, String directory) throws IOException {
        return store(file, FileUtils.generateUniqueFilename(file.getOriginalFilename()), directory);
    }

    @Override
    public String store(MultipartFile file, String filename, String directory) throws IOException {
        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file " + filename);
        }

        String objectName = getObjectName(directory, filename);

        try {
            // Set content type metadata
            Map<String, String> headers = new HashMap<>();
            if (file.getContentType() != null) {
                headers.put("Content-Type", file.getContentType());
            }

            // Upload to MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .headers(headers)
                            .build());

            log.info("Stored file in MinIO: {}", objectName);
            return objectName;
        } catch (Exception e) {
            throw new StorageException("Failed to store file " + filename, e);
        }
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            GetObjectResponse response = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .build());

            // Read the object into memory
            // For larger files, we might want to use InputStreamResource instead
            byte[] content = response.readAllBytes();
            return new ByteArrayResource(content);
        } catch (Exception e) {
            throw new StorageException("Could not read file: " + filename, e);
        }
    }

    @Override
    public String generatePresignedUrl(String filename, int expiryTimeInMinutes) {
        try {
            // Generate a pre-signed URL for accessing the file
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(filename)
                            .expiry(expiryTimeInMinutes, TimeUnit.MINUTES)
                            .build());
        } catch (Exception e) {
            throw new StorageException("Could not generate pre-signed URL for: " + filename, e);
        }
    }

    @Override
    public String generateUploadUrl(String filename, String contentType, int expiryTimeInMinutes) {
        try {
            // Generate a pre-signed URL for uploading a file
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(filename)
                            .expiry(expiryTimeInMinutes, TimeUnit.MINUTES)
                            .build());
        } catch (Exception e) {
            throw new StorageException("Could not generate pre-signed upload URL for: " + filename, e);
        }
    }

    @Override
    public boolean delete(String filename) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
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
        throw new UnsupportedOperationException("MinIO storage does not support listing all files as Path stream");
    }

    @Override
    public boolean exists(String filename) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
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
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .source(CopySource.builder()
                                    .bucket(bucketName)
                                    .object(source)
                                    .build())
                            .bucket(bucketName)
                            .object(destination)
                            .build());
            return true;
        } catch (Exception e) {
            log.error("Error copying file from {} to {}", source, destination, e);
            return false;
        }
    }

    @Override
    public boolean move(String source, String destination) {
        // Copy first, then delete if successful
        boolean copySuccess = copy(source, destination);
        if (copySuccess) {
            return delete(source);
        }
        return false;
    }

    @Override
    public String mergeChunks(List<StorageChunk> chunks, String filename, String directory) throws IOException {
        // Sort chunks by chunk number
        chunks.sort(Comparator.comparing(StorageChunk::getChunkNumber));

        // Create destination object name
        String objectName = getObjectName(directory, filename);

        // Create a temporary file for merging
        Path tempFile = Files.createTempFile(tempDir, "merge-", filename);

        try (OutputStream outputStream = new BufferedOutputStream(
                new FileOutputStream(tempFile.toFile()), BUFFER_SIZE)) {

            // Process each chunk
            for (StorageChunk chunk : chunks) {
                // Download chunk to temp directory
                GetObjectResponse response = null;
                try {
                    response = minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(chunk.getStoragePath())
                                    .build());
                } catch (ErrorResponseException e) {
                    throw new RuntimeException(e);
                } catch (InsufficientDataException e) {
                    throw new RuntimeException(e);
                } catch (InternalException e) {
                    throw new RuntimeException(e);
                } catch (InvalidKeyException e) {
                    throw new RuntimeException(e);
                } catch (InvalidResponseException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                } catch (ServerException e) {
                    throw new RuntimeException(e);
                } catch (XmlParserException e) {
                    throw new RuntimeException(e);
                }

                // Write chunk data to the merged file
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = response.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                // Close the response
                response.close();
            }
        }

        // Upload the merged file
        try (InputStream inputStream = Files.newInputStream(tempFile)) {
            try {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(inputStream, Files.size(tempFile), -1)
                                .build());
            } catch (ErrorResponseException e) {
                throw new RuntimeException(e);
            } catch (InsufficientDataException e) {
                throw new RuntimeException(e);
            } catch (InternalException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeyException e) {
                throw new RuntimeException(e);
            } catch (InvalidResponseException e) {
                throw new RuntimeException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (ServerException e) {
                throw new RuntimeException(e);
            } catch (XmlParserException e) {
                throw new RuntimeException(e);
            }
        }

        // Clean up the temporary file
        Files.deleteIfExists(tempFile);

        log.info("Merged {} chunks into file: {}", chunks.size(), objectName);
        return objectName;
    }

    @Override
    public String generatePreview(String storagePath, String fileType, String mimeType) throws IOException {
        // For now, just create a reference to the preview that would be generated
        String previewName = FilenameUtils.getBaseName(storagePath) + "_preview.jpg";
        String previewPath = "previews/" + previewName;

        // In a real implementation, would download the file, generate a preview, and upload it
        log.warn("Preview generation not fully implemented for MinIO storage");

        return previewPath;
    }

    @Override
    public String generateThumbnail(String storagePath, String fileType, String mimeType) throws IOException {
        // For image files, we might just resize the original image
        // For other files, we'd need to generate a thumbnail representation

        String thumbnailName = FilenameUtils.getBaseName(storagePath) + "_thumbnail.jpg";
        String thumbnailPath = "thumbnails/" + thumbnailName;

        // In a real implementation, would download the file, generate a thumbnail, and upload it
        log.warn("Thumbnail generation not fully implemented for MinIO storage");

        return thumbnailPath;
    }

    @Override
    public String convertFile(String storagePath, String targetFormat) throws IOException {
        // This would require external tools like LibreOffice or FFmpeg
        // For now, just create a reference to where the converted file would be

        String convertedName = FilenameUtils.getBaseName(storagePath) + "." + targetFormat;
        String convertedPath = "converted/" + convertedName;

        log.warn("File conversion not fully implemented for MinIO storage");

        return convertedPath;
    }

    @Override
    public String extractText(String storagePath, String mimeType) throws IOException {
        // This would require text extraction tools like Apache Tika
        // For now, just return a placeholder

        log.warn("Text extraction not fully implemented for MinIO storage");

        return "Text extraction not implemented in this version";
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
            // Download the file to calculate its hash
            Path tempFile = Files.createTempFile(tempDir, "hash-", "tmp");

            try (GetObjectResponse response = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storagePath)
                            .build());
                 OutputStream outputStream = Files.newOutputStream(tempFile)) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = response.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            // Calculate hash from the downloaded file
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(tempFile);
            byte[] hash = digest.digest(fileBytes);

            // Clean up
            Files.deleteIfExists(tempFile);

            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new StorageException("Error computing file hash", e);
        } catch (Exception e) {
            throw new StorageException("Error accessing file: " + storagePath, e);
        }
    }

    // Helper methods

    private String getObjectName(String directory, String filename) {
        return directory == null ? filename : directory + "/" + filename;
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