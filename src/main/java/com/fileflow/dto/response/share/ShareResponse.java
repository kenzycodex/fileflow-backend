package com.fileflow.dto.response.share;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShareResponse {
    private Long id;
    private Long ownerId;
    private String ownerName;
    private Long itemId;
    private String itemName;
    private String itemType;
    private Long recipientId;
    private String recipientName;
    private String recipientEmail;
    private String shareLink;
    private String accessUrl;
    private String permissions;
    private LocalDateTime expiryDate;
    private boolean passwordProtected;
    private LocalDateTime createdAt;
    private int viewCount;
}