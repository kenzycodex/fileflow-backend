package com.fileflow.config;

import com.fileflow.service.config.EnvPropertyService;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class FirebaseConfig {

    private final ResourceLoader resourceLoader;
    private final EnvPropertyService envPropertyService;

    @PostConstruct
    public void initialize() {
        try {
            // Get Firebase configuration from environment
            boolean firebaseEnabled = envPropertyService.getBooleanProperty("FIREBASE_ENABLED", false);
            String configPath = envPropertyService.getProperty("FIREBASE_CONFIG_FILE", "classpath:firebase-service-account.json");

            if (!firebaseEnabled) {
                log.info("Firebase Authentication is disabled. Set FIREBASE_ENABLED=true to enable.");
                return;
            }

            log.info("Initializing Firebase with config file: {}", configPath);

            if (FirebaseApp.getApps().isEmpty()) {
                try {
                    Resource resource = resourceLoader.getResource(configPath);

                    if (!resource.exists()) {
                        log.error("Firebase configuration file not found: {}", configPath);
                        log.error("Please ensure the file exists at the specified location");
                        return;
                    }

                    try (InputStream serviceAccount = resource.getInputStream()) {
                        FirebaseOptions options = FirebaseOptions.builder()
                                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                                .build();

                        FirebaseApp.initializeApp(options);
                        log.info("Firebase has been initialized successfully");
                    }
                } catch (IllegalArgumentException e) {
                    log.error("Firebase initialization failed with invalid arguments: {}", e.getMessage());
                } catch (IOException e) {
                    log.error("Failed to read Firebase configuration file: {}", e.getMessage());
                }
            } else {
                log.info("Firebase is already initialized");
            }
        } catch (Exception e) {
            log.error("Unexpected error during Firebase initialization", e);
        }
    }
}