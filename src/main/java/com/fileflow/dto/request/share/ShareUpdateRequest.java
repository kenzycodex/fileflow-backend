package com.fileflow.dto.request.share;

import lombok.*;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareUpdateRequest {
    private Long recipientId;

    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String recipientEmail;

    private String permissions;

    private LocalDateTime expiryDate;

    @Getter
    private Boolean isPasswordProtected;

    private String password;
}