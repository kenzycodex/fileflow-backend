package com.fileflow.dto.request.sync;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushTokenUpdateRequest {
    @NotBlank(message = "Push token is required")
    @Size(max = 255, message = "Push token cannot exceed 255 characters")
    private String pushToken;
}
