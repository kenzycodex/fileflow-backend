package com.fileflow.service.storage;

import com.fileflow.model.StorageChunk;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Enhanced storage service interface with additional features
 */
public interface EnhancedStorageService extends StorageService {

    /**
     * Merge chunks into a single file
     *
     * @param chunks list of chunks to merge
     * @param filename filename for the merged file
     * @param directory directory to store the merged file
     * @return storage path of the merged file
     * @throws IOException if error occurs during merging
     */
    String mergeChunks(List<StorageChunk> chunks, String filename, String directory) throws IOException;

    /**
     * Process file for preview generation
     *
     * @param storagePath path to the stored file
     * @param fileType type of file
     * @param mimeType MIME type of file
     * @return path to the preview file
     * @throws IOException if error occurs during processing
     */
    String generatePreview(String storagePath, String fileType, String mimeType) throws IOException;

    /**
     * Generate a thumbnail for a file
     *
     * @param storagePath path to the stored file
     * @param fileType type of file
     * @param mimeType MIME type of file
     * @return path to the thumbnail
     * @throws IOException if error occurs during thumbnail generation
     */
    String generateThumbnail(String storagePath, String fileType, String mimeType) throws IOException;

    /**
     * Convert a file to a different format
     *
     * @param storagePath path to the stored file
     * @param targetFormat target format extension (pdf, jpg, etc)
     * @return path to the converted file
     * @throws IOException if error occurs during conversion
     */
    String convertFile(String storagePath, String targetFormat) throws IOException;

    /**
     * Extract text content from a file (for search indexing)
     *
     * @param storagePath path to the stored file
     * @param mimeType MIME type of file
     * @return extracted text
     * @throws IOException if error occurs during text extraction
     */
    String extractText(String storagePath, String mimeType) throws IOException;

    /**
     * Get a pre-signed URL for direct upload to storage
     *
     * @param filename filename for the upload
     * @param contentType content type of the file
     * @param expiryTimeInMinutes expiry time in minutes
     * @return pre-signed upload URL
     */
    String generateUploadUrl(String filename, String contentType, int expiryTimeInMinutes);

    /**
     * Compute the hash of a file
     *
     * @param file the file to hash
     * @return hash of the file
     * @throws IOException if error occurs during hash computation
     */
    String computeHash(MultipartFile file) throws IOException;

    /**
     * Compute the hash of a stored file
     *
     * @param storagePath path to the stored file
     * @return hash of the file
     * @throws IOException if error occurs during hash computation
     */
    String computeHash(String storagePath) throws IOException;
}