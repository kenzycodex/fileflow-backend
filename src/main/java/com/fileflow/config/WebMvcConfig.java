package com.fileflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

/**
 * Spring MVC configuration for API and Swagger UI
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Swagger UI resources
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS));

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS));

        // API Docs resources
        registry.addResourceHandler("/v3/api-docs/**")
                .addResourceLocations("classpath:/META-INF/resources/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS));
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Direct mapping for Swagger UI
        registry.addViewController("/swagger-ui/")
                .setViewName("redirect:/swagger-ui/index.html");

        // Add shortcut for API docs
        registry.addViewController("/api-docs")
                .setViewName("redirect:/swagger-ui/index.html");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Disposition")  // Added Content-Disposition for file downloads
                .maxAge(3600);
    }

    /**
     * Configure content negotiation to default to application/json
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .defaultContentType(MediaType.APPLICATION_JSON)
                .favorParameter(false)
                .ignoreAcceptHeader(false)
                .mediaType("json", MediaType.APPLICATION_JSON);
    }
}