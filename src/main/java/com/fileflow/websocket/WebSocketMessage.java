// File: src/main/java/com/fileflow/websocket/WebSocketMessage.java
package com.fileflow.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * WebSocket message structure
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private WebSocketMessageType type;
    private Map<String, Object> data;
}