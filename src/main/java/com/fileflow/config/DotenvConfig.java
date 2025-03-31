package com.fileflow.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class DotenvConfig {

    private Dotenv dotenv;

    @PostConstruct
    public void init() {
        try {
            // Try to load from current directory first
            Path dotenvPath = Paths.get(".env");

            if (Files.exists(dotenvPath)) {
                log.info("Loading .env from current directory: {}", dotenvPath.toAbsolutePath());
                dotenv = Dotenv.configure()
                        .directory(".")
                        .ignoreIfMissing()
                        .load();
            } else {
                // Try to load from root directory
                Path rootDotenvPath = Paths.get("/app/.env");
                if (Files.exists(rootDotenvPath)) {
                    log.info("Loading .env from /app directory");
                    dotenv = Dotenv.configure()
                            .directory("/app")
                            .ignoreIfMissing()
                            .load();
                } else {
                    // Try to load from classpath
                    try {
                        File classpathEnv = new ClassPathResource(".env").getFile();
                        if (classpathEnv.exists()) {
                            log.info("Loading .env from classpath");
                            dotenv = Dotenv.configure()
                                    .directory(classpathEnv.getParent())
                                    .ignoreIfMissing()
                                    .load();
                        } else {
                            log.warn(".env file not found in any location, using system environment variables");
                            dotenv = Dotenv.configure()
                                    .ignoreIfMissing()
                                    .load();
                        }
                    } catch (Exception e) {
                        log.warn("Error loading .env from classpath: {}", e.getMessage());
                        dotenv = Dotenv.configure()
                                .ignoreIfMissing()
                                .load();
                    }
                }
            }

            // Log that environment variables were loaded
            log.info("Environment variables loaded successfully");

        } catch (Exception e) {
            log.error("Error initializing .env configuration", e);
            // Continue without dotenv - will use system environment variables
        }
    }

    /**
     * Get value from environment variables
     *
     * @param key Environment variable key
     * @return The value or null if not found
     */
    public String get(String key) {
        if (dotenv != null) {
            return dotenv.get(key);
        }
        return System.getenv(key);
    }

    /**
     * Get value from environment variables with a default value
     *
     * @param key Environment variable key
     * @param defaultValue Default value if not found
     * @return The value or defaultValue if not found
     */
    public String get(String key, String defaultValue) {
        if (dotenv != null) {
            return dotenv.get(key, defaultValue);
        }
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
}