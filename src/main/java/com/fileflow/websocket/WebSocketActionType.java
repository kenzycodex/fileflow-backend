// File: src/main/java/com/fileflow/websocket/WebSocketActionType.java
package com.fileflow.websocket;

/**
 * Action types for WebSocket events
 */
public enum WebSocketActionType {
    CREATED,
    UPDATED,
    DELETED,
    MOVED,
    RENAMED,
    SHARED,
    UNSHARED,
    RESTORED,
    UPLOADED,
    DOWNLOADED,
    LOCKED,
    UNLOCKED,
    COMMENTED
}