package com.fileflow.service.share;

import com.fileflow.dto.request.share.ShareCreateRequest;
import com.fileflow.dto.request.share.ShareUpdateRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.common.PagedResponse;
import com.fileflow.dto.response.share.ShareResponse;
import com.fileflow.model.Share;

public interface ShareService {
    /**
     * Create a new share
     *
     * @param shareCreateRequest request containing share details
     * @return the created share response
     */
    ShareResponse createShare(ShareCreateRequest shareCreateRequest);

    /**
     * Get share by ID
     *
     * @param shareId share ID
     * @return the share response
     */
    ShareResponse getShare(Long shareId);

    /**
     * Get share by link
     *
     * @param shareLink share link
     * @param password optional password for protected shares
     * @return the share response
     */
    ShareResponse getShareByLink(String shareLink, String password);

    /**
     * Update share
     *
     * @param shareId share ID
     * @param shareUpdateRequest request containing updated share details
     * @return the updated share response
     */
    ShareResponse updateShare(Long shareId, ShareUpdateRequest shareUpdateRequest);

    /**
     * Delete share
     *
     * @param shareId share ID
     * @return response with deletion information
     */
    ApiResponse deleteShare(Long shareId);

    /**
     * Get shares created by current user
     *
     * @param page page number
     * @param size page size
     * @return paged list of shares
     */
    PagedResponse<ShareResponse> getOutgoingShares(int page, int size);

    /**
     * Get shares shared with current user
     *
     * @param page page number
     * @param size page size
     * @return paged list of shares
     */
    PagedResponse<ShareResponse> getIncomingShares(int page, int size);

    /**
     * Validate share password
     *
     * @param shareId share ID
     * @param password password to validate
     * @return response with validation result
     */
    ApiResponse validateSharePassword(Long shareId, String password);

    /**
     * Increment share view count
     *
     * @param shareId share ID
     */
    void incrementViewCount(Long shareId);

    /**
     * Check if item is shared with specific user
     *
     * @param itemId item ID
     * @param itemType item type (FILE/FOLDER)
     * @param userId user ID
     * @return true if shared with user, false otherwise
     */
    boolean isSharedWithUser(Long itemId, Share.ItemType itemType, Long userId);

    /**
     * Check user's permission for shared item
     *
     * @param itemId item ID
     * @param itemType item type (FILE/FOLDER)
     * @param userId user ID
     * @return the permission (VIEW/EDIT/COMMENT) or null if not shared
     */
    Share.Permission getUserPermission(Long itemId, Share.ItemType itemType, Long userId);

    /**
     * Delete expired shares
     *
     * @return number of deleted shares
     */
    int deleteExpiredShares();
}