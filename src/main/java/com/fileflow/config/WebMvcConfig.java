package com.fileflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Static resources
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");

        // Add explicit handlers for Swagger UI resources
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/");

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Direct mapping for Swagger UI
        registry.addViewController("/swagger-ui/")
                .setViewName("redirect:/swagger-ui/index.html");

        // Simply redirect root to Swagger UI for now
        registry.addViewController("/")
                .setViewName("redirect:/swagger-ui/index.html");

        // Don't add a catchall mapping that could cause infinite forwarding loops
    }
}