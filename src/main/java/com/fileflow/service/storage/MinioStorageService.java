package com.fileflow.service.storage;

import com.fileflow.config.StorageConfig;
import com.fileflow.exception.StorageException;
import com.fileflow.util.FileUtils;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;

    @Value("${app.minio.bucket}")
    private String bucketName;

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
                            return Paths.get(itemResult.get().objectName());
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

    private String getObjectName(String directory, String filename) {
        return directory == null ? filename : directory + "/" + filename;
    }
}