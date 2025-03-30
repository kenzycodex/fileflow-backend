package com.fileflow.bridge.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Central registry for all Java bridges that can be exposed to JavaScript
 */
@Component
public class BridgeRegistry {

    private static final Logger logger = LoggerFactory.getLogger(BridgeRegistry.class);

    @Autowired
    private ApplicationContext applicationContext;

    private final Map<String, Object> bridges = new HashMap<>();

    @PostConstruct
    public void init() {
        logger.info("Initializing BridgeRegistry");

        // Auto-register all bridge implementations
        Map<String, BridgeInterface> impls = applicationContext.getBeansOfType(BridgeInterface.class);

        impls.forEach((name, bridge) -> {
            String bridgeName = bridge.getBridgeName();
            bridges.put(bridgeName, bridge);
            logger.info("Registered bridge: {} (implementation: {})", bridgeName, name);
        });

        logger.info("BridgeRegistry initialized with {} bridges", bridges.size());
    }

    /**
     * Get a bridge by name
     */
    public Object getBridge(String name) {
        return bridges.get(name);
    }

    /**
     * Check if a bridge exists
     */
    public boolean hasBridge(String name) {
        return bridges.containsKey(name);
    }

    /**
     * Get all registered bridges
     */
    public Map<String, Object> getAllBridges() {
        return new HashMap<>(bridges);
    }

    /**
     * Get names of all registered bridges
     */
    public String[] getBridgeNames() {
        return bridges.keySet().toArray(new String[0]);
    }
}