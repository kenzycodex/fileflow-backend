// File: src/main/java/com/fileflow/websocket/WebSocketMessageType.java
package com.fileflow.websocket;

/**
 * Message types for WebSocket communication
 */
public enum WebSocketMessageType {
    // Connection messages
    CONNECTION_ESTABLISHED,
    CONNECTION_ERROR,

    // Heartbeat messages
    PING,
    PONG,

    // Event subscriptions
    SUBSCRIBE_TO_FILE_EVENTS,
    SUBSCRIBE_TO_FOLDER_EVENTS,
    UNSUBSCRIBE_FROM_FILE_EVENTS,
    UNSUBSCRIBE_FROM_FOLDER_EVENTS,

    // Notification messages
    FILE_EVENT,
    FOLDER_EVENT,
    QUOTA_UPDATE,
    SYSTEM_NOTIFICATION
}