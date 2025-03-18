package com.fileflow.dto.request.share;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareCreateRequest {
    @NotNull(message = "Item ID is required")
    private Long itemId;

    @NotBlank(message = "Item type is required")
    private String itemType;

    private Long recipientId;

    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String recipientEmail;

    @NotBlank(message = "Permissions are required")
    private String permissions;

    private LocalDateTime expiryDate;

    private boolean passwordProtected;

    @Size(min = 4, max = 50, message = "Password must be between 4 and 50 characters")
    private String password;
}