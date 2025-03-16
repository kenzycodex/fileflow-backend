package com.fileflow.dto.request.sync;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncItemRequest {
    private Long id;

    @NotBlank(message = "Action type is required")
    private String actionType;

    @NotNull(message = "Item ID is required")
    private Long itemId;

    @NotBlank(message = "Item type is required")
    private String itemType;

    private String dataPayload;
}
