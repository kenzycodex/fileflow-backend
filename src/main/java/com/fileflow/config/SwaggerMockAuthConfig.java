package com.fileflow.config;

import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerMockAuthConfig {

    @Bean
    public OpenApiCustomizer mockAuthCustomizer() {
        return openApi -> {
            // Add a mock authentication option
            openApi.getComponents().addSecuritySchemes("mockAuth",
                    new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("Mock Authentication Token for Testing")
            );
        };
    }

    // Provide a mock token for Swagger UI testing
    @Bean
    public String mockAuthToken() {
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0IiwibmFtZSI6IlN3YWdnZXIgVGVzdCBVc2VyIiwicm9sZSI6IlVTRVIifQ.mockTokenForSwaggerTesting";
    }
}