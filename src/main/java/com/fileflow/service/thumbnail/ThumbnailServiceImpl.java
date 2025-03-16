package com.fileflow.service.thumbnail;

import com.fileflow.exception.ResourceNotFoundException;
import com.fileflow.model.File;
import com.fileflow.repository.FileRepository;
import com.fileflow.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThumbnailServiceImpl implements ThumbnailService {

    private final FileRepository fileRepository;
    private final StorageService storageService;

    private static final List<String> SUPPORTED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/tiff"
    );

    private static final int THUMBNAIL_WIDTH = 200;
    private static final int THUMBNAIL_HEIGHT = 200;

    @Override
    @Async
    public boolean generateThumbnail(Long fileId) {
        try {
            File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

            // Check if file is an image
            if (!isSupportedImageType(file.getMimeType())) {
                return false;
            }

            // Load file content
            var resource = storageService.loadAsResource(file.getStoragePath());
            try (InputStream inputStream = resource.getInputStream()) {
                BufferedImage originalImage = ImageIO.read(inputStream);

                if (originalImage == null) {
                    log.warn("Failed to read image for file ID: {}", fileId);
                    return false;
                }

                // Generate thumbnail
                BufferedImage thumbnail = createThumbnail(originalImage);

                // Save thumbnail
                String thumbnailPath = saveThumbnail(thumbnail, fileId);

                // Update file with thumbnail path
                file.setThumbnailPath(thumbnailPath);
                fileRepository.save(file);

                return true;
            }
        } catch (Exception e) {
            log.error("Error generating thumbnail for file ID: {}", fileId, e);
            return false;
        }
    }

    @Override
    public String generateThumbnail(MultipartFile file, Long fileId) throws IOException {
        // Check if file is an image
        if (!isSupportedImageType(file.getContentType())) {
            return null;
        }

        try {
            BufferedImage originalImage = ImageIO.read(file.getInputStream());

            if (originalImage == null) {
                return null;
            }

            // Generate thumbnail
            BufferedImage thumbnail = createThumbnail(originalImage);

            // Save thumbnail
            return saveThumbnail(thumbnail, fileId);
        } catch (Exception e) {
            log.error("Error generating thumbnail for uploaded file", e);
            return null;
        }
    }

    @Override
    public String getThumbnailUrl(Long fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        if (file.getThumbnailPath() == null) {
            return null;
        }

        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/files/thumbnails/")
                .path(fileId.toString())
                .toUriString();
    }

    @Override
    public boolean hasThumbnail(Long fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        return file.getThumbnailPath() != null;
    }

    @Override
    public boolean deleteThumbnail(Long fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        if (file.getThumbnailPath() == null) {
            return false;
        }

        try {
            // Delete thumbnail from storage
            storageService.delete(file.getThumbnailPath());

            // Update file record
            file.setThumbnailPath(null);
            fileRepository.save(file);

            return true;
        } catch (Exception e) {
            log.error("Error deleting thumbnail for file ID: {}", fileId, e);
            return false;
        }
    }

    @Override
    @Async
    public int batchGenerateThumbnails(int batchSize) {
        // Find files without thumbnails
        List<File> files = fileRepository.findFilesWithoutThumbnails(
                PageRequest.of(0, batchSize));

        int count = 0;

        for (File file : files) {
            if (generateThumbnail(file.getId())) {
                count++;
            }
        }

        log.info("Batch generated {} thumbnails", count);
        return count;
    }

    // Helper methods

    private boolean isSupportedImageType(String mimeType) {
        return SUPPORTED_IMAGE_TYPES.contains(mimeType);
    }

    private BufferedImage createThumbnail(BufferedImage original) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        // Calculate dimensions while maintaining aspect ratio
        double ratio = (double) THUMBNAIL_WIDTH / (double) originalWidth;
        int scaledHeight = (int) (originalHeight * ratio);

        int finalHeight = scaledHeight;
        if (scaledHeight > THUMBNAIL_HEIGHT) {
            ratio = (double) THUMBNAIL_HEIGHT / (double) originalHeight;
            int scaledWidth = (int) (originalWidth * ratio);
            finalHeight = THUMBNAIL_HEIGHT;
            originalWidth = scaledWidth;
        } else {
            originalWidth = THUMBNAIL_WIDTH;
            originalHeight = scaledHeight;
        }

        // Create thumbnail image
        BufferedImage thumbnail = new BufferedImage(
                THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = thumbnail.createGraphics();

        // Fill with white background for images with transparency
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);

        // Center the image
        int x = (THUMBNAIL_WIDTH - originalWidth) / 2;
        int y = (THUMBNAIL_HEIGHT - finalHeight) / 2;

        // Draw the scaled image
        g.drawImage(original, x, y, originalWidth, finalHeight, null);
        g.dispose();

        return thumbnail;
    }

    private String saveThumbnail(BufferedImage thumbnail, Long fileId) throws IOException {
        // Convert BufferedImage to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, "jpg", baos);
        byte[] imageData = baos.toByteArray();

        // Create unique filename
        String filename = "thumbnail-" + fileId + "-" + UUID.randomUUID().toString() + ".jpg";

        // Determine storage directory (user-specific)
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        String storageDir = "users/" + file.getUser().getId() + "/thumbnails";

        // Store thumbnail
        InputStream inputStream = new ByteArrayInputStream(imageData);
        Path tempFilePath = Path.of(filename);

        // Use the storage service to store the file
        return storageService.store(inputStream, imageData.length, filename, storageDir);
    }
}