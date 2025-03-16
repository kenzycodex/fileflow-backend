package com.fileflow.service.version;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.file.FileVersionResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface VersioningService {
    /**
     * Create new version of file
     *
     * @param fileId file ID
     * @param file new file content
     * @param comment optional comment
     * @return created version information
     * @throws IOException if error occurs during file processing
     */
    FileVersionResponse createVersion(Long fileId, MultipartFile file, String comment) throws IOException;

    /**
     * Get all versions of file
     *
     * @param fileId file ID
     * @return list of versions
     */
    List<FileVersionResponse> getVersions(Long fileId);

    /**
     * Get specific version of file
     *
     * @param versionId version ID
     * @return version information
     */
    FileVersionResponse getVersion(Long versionId);

    /**
     * Restore file to specific version
     *
     * @param fileId file ID
     * @param versionId version ID to restore
     * @return API response with result
     */
    ApiResponse restoreVersion(Long fileId, Long versionId);

    /**
     * Delete specific version
     *
     * @param versionId version ID to delete
     * @return API response with result
     */
    ApiResponse deleteVersion(Long versionId);

    /**
     * Cleanup old versions based on retention policy
     *
     * @param maxVersionsPerFile maximum versions to keep per file
     * @return number of deleted versions
     */
    int cleanupOldVersions(int maxVersionsPerFile);
}