package com.fileflow.service.storage;

import com.fileflow.config.StorageConfig;
import com.fileflow.exception.StorageException;
import com.fileflow.util.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

@Service
@Profile("!prod")
@RequiredArgsConstructor
@Slf4j
public class LocalStorageService implements StorageService {

    private final Path rootLocation;

    public LocalStorageService(StorageConfig storageConfig) {
        this.rootLocation = storageConfig.fileStorageLocation();
    }

    @PostConstruct
    @Override
    public void init() {
        try {
            FileUtils.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
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
        Path targetLocation = getTargetLocation(directory, sanitizedFilename);

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Return the path relative to the root location
        return getRelativePath(directory, sanitizedFilename);
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
            return true;
        } catch (IOException e) {
            log.error("Error moving file from {} to {}", source, destination, e);
            return false;
        }
    }

    private Path getTargetLocation(String directory, String filename) throws IOException {
        Path dirPath = directory == null ? rootLocation : rootLocation.resolve(directory);

        // Create directories if they don't exist
        FileUtils.createDirectories(dirPath);

        return dirPath.resolve(filename);
    }

    private String getRelativePath(String directory, String filename) {
        return directory == null ? filename : Paths.get(directory, filename).toString();
    }
}