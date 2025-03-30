package com.fileflow.bridge.core;

/**
 * Base interface for all bridges that can be exposed to JavaScript
 */
public interface BridgeInterface {
    /**
     * Get the name of this bridge (used in JavaScript)
     * This name will be used as prefix for bridge in JavaScript
     * e.g., "file" becomes "fileBridge"
     */
    String getBridgeName();
}