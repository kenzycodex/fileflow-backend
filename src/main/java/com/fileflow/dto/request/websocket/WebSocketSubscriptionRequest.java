package com.fileflow.dto.request.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * Request for WebSocket subscription
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketSubscriptionRequest {

    /**
     * ID of the item to subscribe to (file ID or folder ID)
     */
    @NotNull
    private Long itemId;

    /**
     * Additional subscription options
     * Examples: "includeChildItems", "notifyOnDownload", etc.
     */
    private String[] options;
}