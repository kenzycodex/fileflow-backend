package com.fileflow.service.share;

import com.fileflow.dto.request.share.ShareCreateRequest;
import com.fileflow.dto.request.share.ShareUpdateRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.common.PagedResponse;
import com.fileflow.dto.response.share.ShareResponse;
import com.fileflow.exception.BadRequestException;
import com.fileflow.exception.ForbiddenException;
import com.fileflow.exception.ResourceNotFoundException;
import com.fileflow.model.File;
import com.fileflow.model.Folder;
import com.fileflow.model.Share;
import com.fileflow.model.User;
import com.fileflow.repository.FileRepository;
import com.fileflow.repository.FolderRepository;
import com.fileflow.repository.ShareRepository;
import com.fileflow.repository.UserRepository;
import com.fileflow.security.UserPrincipal;
import com.fileflow.service.activity.ActivityService;
import com.fileflow.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShareServiceImpl implements ShareService {

    private final ShareRepository shareRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityService activityService;

    @Override
    @Transactional
    public ShareResponse createShare(ShareCreateRequest shareCreateRequest) {
        User currentUser = getCurrentUser();

        // Validate item exists and belongs to the user
        validateItemOwnership(currentUser.getId(), shareCreateRequest.getItemId(), shareCreateRequest.getItemType());

        // Check if already shared (update instead of create)
        Share existingShare = shareRepository.findByOwnerAndItemIdAndItemType(
                        currentUser, shareCreateRequest.getItemId(),
                        Share.ItemType.valueOf(shareCreateRequest.getItemType()))
                .orElse(null);

        if (existingShare != null) {
            // Update existing share
            updateExistingShare(existingShare, shareCreateRequest);
            Share updatedShare = shareRepository.save(existingShare);

            // Log activity
            activityService.logActivity(
                    currentUser.getId(),
                    Constants.ACTIVITY_UPDATE_SHARE,
                    shareCreateRequest.getItemType(),
                    shareCreateRequest.getItemId(),
                    "Updated share for " + shareCreateRequest.getItemType().toLowerCase() + ": " + getItemName(
                            shareCreateRequest.getItemId(), shareCreateRequest.getItemType())
            );

            return mapShareToShareResponse(updatedShare);
        }

        // Create new share
        Share share = buildShare(currentUser, shareCreateRequest);
        Share savedShare = shareRepository.save(share);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_SHARE,
                shareCreateRequest.getItemType(),
                shareCreateRequest.getItemId(),
                "Shared " + shareCreateRequest.getItemType().toLowerCase() + ": " + getItemName(
                        shareCreateRequest.getItemId(), shareCreateRequest.getItemType())
        );

        return mapShareToShareResponse(savedShare);
    }

    @Override
    public ShareResponse getShare(Long shareId) {
        User currentUser = getCurrentUser();

        Share share = shareRepository.findById(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("Share", "id", shareId));

        // Check if user is owner or recipient
        if (!share.getOwner().getId().equals(currentUser.getId()) &&
                (share.getRecipient() == null || !share.getRecipient().getId().equals(currentUser.getId()))) {
            throw new ForbiddenException("You do not have permission to access this share");
        }

        return mapShareToShareResponse(share);
    }

    @Override
    @Transactional
    public ShareResponse getShareByLink(String shareLink, String password) {
        Share share = shareRepository.findByShareLink(shareLink)
                .orElseThrow(() -> new ResourceNotFoundException("Share", "link", shareLink));

        // Check if expired
        if (share.getExpiryDate() != null && share.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This share link has expired");
        }

        // Check password if protected
        if (share.isPasswordProtected()) {
            if (password == null || password.isEmpty()) {
                throw new ForbiddenException("This share is password protected");
            }

            if (!passwordEncoder.matches(password, share.getPasswordHash())) {
                throw new ForbiddenException("Invalid password");
            }
        }

        // Increment view count
        share.setViewCount(share.getViewCount() + 1);
        Share updatedShare = shareRepository.save(share);

        return mapShareToShareResponse(updatedShare);
    }

    @Override
    @Transactional
    public ShareResponse updateShare(Long shareId, ShareUpdateRequest shareUpdateRequest) {
        User currentUser = getCurrentUser();

        Share share = shareRepository.findById(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("Share", "id", shareId));

        // Check if user is owner
        if (!share.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to update this share");
        }

        // Update recipient if provided
        if (shareUpdateRequest.getRecipientId() != null) {
            User recipient = userRepository.findById(shareUpdateRequest.getRecipientId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", shareUpdateRequest.getRecipientId()));

            share.setRecipient(recipient);
            share.setRecipientEmail(recipient.getEmail());
        } else if (shareUpdateRequest.getRecipientEmail() != null && !shareUpdateRequest.getRecipientEmail().isEmpty()) {
            // Update recipient email if provided
            share.setRecipient(null);
            share.setRecipientEmail(shareUpdateRequest.getRecipientEmail());
        }

        // Update permissions if provided
        if (shareUpdateRequest.getPermissions() != null) {
            share.setPermissions(Share.Permission.valueOf(shareUpdateRequest.getPermissions()));
        }

        // Update expiry date if provided
        if (shareUpdateRequest.getExpiryDate() != null) {
            share.setExpiryDate(shareUpdateRequest.getExpiryDate());
        }

        // Update password protection if provided
        if (shareUpdateRequest.isPasswordProtected() != null) {
            share.setPasswordProtected(shareUpdateRequest.isPasswordProtected());

            // Update password if provided and protection is enabled
            if (share.isPasswordProtected() && shareUpdateRequest.getPassword() != null &&
                    !shareUpdateRequest.getPassword().isEmpty()) {
                share.setPasswordHash(passwordEncoder.encode(shareUpdateRequest.getPassword()));
            } else if (!share.isPasswordProtected()) {
                share.setPasswordHash(null);
            }
        }

        Share updatedShare = shareRepository.save(share);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_UPDATE_SHARE,
                updatedShare.getItemType().name(),
                updatedShare.getItemId(),
                "Updated share for " + updatedShare.getItemType().name().toLowerCase() + ": " +
                        getItemName(updatedShare.getItemId(), updatedShare.getItemType().name())
        );

        return mapShareToShareResponse(updatedShare);
    }

    @Override
    @Transactional
    public ApiResponse deleteShare(Long shareId) {
        User currentUser = getCurrentUser();

        Share share = shareRepository.findById(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("Share", "id", shareId));

        // Check if user is owner
        if (!share.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to delete this share");
        }

        // Get item info for logging
        String itemType = share.getItemType().name();
        Long itemId = share.getItemId();
        String itemName = getItemName(itemId, itemType);

        // Delete share
        shareRepository.delete(share);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_DELETE_SHARE,
                itemType,
                itemId,
                "Removed share for " + itemType.toLowerCase() + ": " + itemName
        );

        return ApiResponse.builder()
                .success(true)
                .message("Share deleted successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public PagedResponse<ShareResponse> getOutgoingShares(int page, int size) {
        User currentUser = getCurrentUser();
        validatePageNumberAndSize(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Share> shares = shareRepository.findByOwner(currentUser, pageable);

        if (shares.getNumberOfElements() == 0) {
            return new PagedResponse<>(Collections.emptyList(), shares.getNumber(),
                    shares.getSize(), shares.getTotalElements(), shares.getTotalPages(), shares.isLast());
        }

        List<ShareResponse> shareResponses = shares.map(this::mapShareToShareResponse).getContent();

        return new PagedResponse<>(shareResponses, shares.getNumber(),
                shares.getSize(), shares.getTotalElements(), shares.getTotalPages(), shares.isLast());
    }

    @Override
    public PagedResponse<ShareResponse> getIncomingShares(int page, int size) {
        User currentUser = getCurrentUser();
        validatePageNumberAndSize(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Share> shares = shareRepository.findByRecipient(currentUser, pageable);

        if (shares.getNumberOfElements() == 0) {
            return new PagedResponse<>(Collections.emptyList(), shares.getNumber(),
                    shares.getSize(), shares.getTotalElements(), shares.getTotalPages(), shares.isLast());
        }

        List<ShareResponse> shareResponses = shares.map(this::mapShareToShareResponse).getContent();

        return new PagedResponse<>(shareResponses, shares.getNumber(),
                shares.getSize(), shares.getTotalElements(), shares.getTotalPages(), shares.isLast());
    }

    @Override
    public ApiResponse validateSharePassword(Long shareId, String password) {
        Share share = shareRepository.findById(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("Share", "id", shareId));

        if (!share.isPasswordProtected()) {
            return ApiResponse.builder()
                    .success(true)
                    .message("Share is not password protected")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        boolean isValid = passwordEncoder.matches(password, share.getPasswordHash());

        return ApiResponse.builder()
                .success(isValid)
                .message(isValid ? "Password is valid" : "Invalid password")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public void incrementViewCount(Long shareId) {
        Share share = shareRepository.findById(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("Share", "id", shareId));

        share.setViewCount(share.getViewCount() + 1);
        shareRepository.save(share);
    }

    @Override
    public boolean isSharedWithUser(Long itemId, Share.ItemType itemType, Long userId) {
        List<Share> shares = shareRepository.findByItemIdAndItemType(itemId, itemType);

        return shares.stream()
                .anyMatch(share -> share.getRecipient() != null &&
                        share.getRecipient().getId().equals(userId));
    }

    @Override
    public Share.Permission getUserPermission(Long itemId, Share.ItemType itemType, Long userId) {
        List<Share> shares = shareRepository.findByItemIdAndItemType(itemId, itemType);

        return shares.stream()
                .filter(share -> share.getRecipient() != null &&
                        share.getRecipient().getId().equals(userId))
                .map(Share::getPermissions)
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional
    public int deleteExpiredShares() {
        List<Share> expiredShares = shareRepository.findByExpiryDateBefore(LocalDateTime.now());

        if (!expiredShares.isEmpty()) {
            shareRepository.deleteAll(expiredShares);
            log.info("Deleted {} expired shares", expiredShares.size());
            return expiredShares.size();
        }

        return 0;
    }

    // Helper methods

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void validatePageNumberAndSize(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page number cannot be less than zero.");
        }

        if (size < 1) {
            throw new BadRequestException("Page size must not be less than one.");
        }

        if (size > 100) {
            throw new BadRequestException("Page size must not be greater than 100.");
        }
    }

    private void validateItemOwnership(Long userId, Long itemId, String itemType) {
        if (Share.ItemType.FILE.name().equals(itemType)) {
            File file = fileRepository.findById(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException("File", "id", itemId));

            if (!file.getUser().getId().equals(userId)) {
                throw new ForbiddenException("You do not have permission to share this file");
            }

            if (file.isDeleted()) {
                throw new BadRequestException("Cannot share a deleted file");
            }
        } else if (Share.ItemType.FOLDER.name().equals(itemType)) {
            Folder folder = folderRepository.findById(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", itemId));

            if (!folder.getUser().getId().equals(userId)) {
                throw new ForbiddenException("You do not have permission to share this folder");
            }

            if (folder.isDeleted()) {
                throw new BadRequestException("Cannot share a deleted folder");
            }
        } else {
            throw new BadRequestException("Invalid item type: " + itemType);
        }
    }

    private void updateExistingShare(Share share, ShareCreateRequest request) {
        // Update recipient
        if (request.getRecipientId() != null) {
            User recipient = userRepository.findById(request.getRecipientId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getRecipientId()));

            share.setRecipient(recipient);
            share.setRecipientEmail(recipient.getEmail());
        } else if (request.getRecipientEmail() != null && !request.getRecipientEmail().isEmpty()) {
            share.setRecipient(null);
            share.setRecipientEmail(request.getRecipientEmail());
        }

        // Update permissions
        share.setPermissions(Share.Permission.valueOf(request.getPermissions()));

        // Update expiry date
        share.setExpiryDate(request.getExpiryDate());

        // Update password protection
        share.setPasswordProtected(request.isPasswordProtected());

        if (share.isPasswordProtected() && request.getPassword() != null && !request.getPassword().isEmpty()) {
            share.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        } else if (!share.isPasswordProtected()) {
            share.setPasswordHash(null);
        }
    }

    private Share buildShare(User owner, ShareCreateRequest request) {
        // Generate unique share link
        String shareLink = UUID.randomUUID().toString();

        // Determine recipient
        User recipient = null;
        String recipientEmail = request.getRecipientEmail();

        if (request.getRecipientId() != null) {
            recipient = userRepository.findById(request.getRecipientId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getRecipientId()));

            recipientEmail = recipient.getEmail();
        }

        return Share.builder()
                .owner(owner)
                .itemId(request.getItemId())
                .itemType(Share.ItemType.valueOf(request.getItemType()))
                .recipient(recipient)
                .recipientEmail(recipientEmail)
                .shareLink(shareLink)
                .permissions(Share.Permission.valueOf(request.getPermissions()))
                .expiryDate(request.getExpiryDate())
                .isPasswordProtected(request.isPasswordProtected())
                .passwordHash(request.isPasswordProtected() && request.getPassword() != null &&
                        !request.getPassword().isEmpty() ?
                        passwordEncoder.encode(request.getPassword()) : null)
                .createdAt(LocalDateTime.now())
                .viewCount(0)
                .build();
    }

    private String getItemName(Long itemId, String itemType) {
        if (Share.ItemType.FILE.name().equals(itemType)) {
            File file = fileRepository.findById(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException("File", "id", itemId));

            return file.getFilename();
        } else if (Share.ItemType.FOLDER.name().equals(itemType)) {
            Folder folder = folderRepository.findById(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", itemId));

            return folder.getFolderName();
        }

        return "Unknown item";
    }

    private ShareResponse mapShareToShareResponse(Share share) {
        String itemName = getItemName(share.getItemId(), share.getItemType().name());

        // Generate access URL
        String accessUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/shares/links/")
                .path(share.getShareLink())
                .toUriString();

        return ShareResponse.builder()
                .id(share.getId())
                .ownerId(share.getOwner().getId())
                .ownerName(share.getOwner().getUsername())
                .itemId(share.getItemId())
                .itemName(itemName)
                .itemType(share.getItemType().name())
                .recipientId(share.getRecipient() != null ? share.getRecipient().getId() : null)
                .recipientName(share.getRecipient() != null ? share.getRecipient().getUsername() : null)
                .recipientEmail(share.getRecipientEmail())
                .shareLink(share.getShareLink())
                .accessUrl(accessUrl)
                .permissions(share.getPermissions().name())
                .expiryDate(share.getExpiryDate())
                .passwordProtected(share.isPasswordProtected())
                .createdAt(share.getCreatedAt())
                .viewCount(share.getViewCount())
                .build();
    }
}