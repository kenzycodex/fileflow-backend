package com.fileflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties
@EnableAsync
@EnableScheduling
public class FileFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileFlowApplication.class, args);
    }
}