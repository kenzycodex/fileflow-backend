package com.fileflow.config;

import com.fileflow.interceptor.RateLimitInterceptor;
import com.fileflow.service.config.EnvPropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * Spring MVC configuration for API and Swagger UI
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private EnvPropertyService envPropertyService;
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

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
    public void addInterceptors(InterceptorRegistry registry) {
        // Add rate limit interceptor for API endpoints
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/v1/public/**", "/api/v1/auth/token/refresh");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Get allowed origins from environment or config
        String allowedOriginsStr = envPropertyService.getProperty("ALLOWED_ORIGINS",
                "http://localhost:3000,http://localhost:4173");

        String[] allowedOrigins = allowedOriginsStr.split(",");

        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Disposition")
                .allowCredentials(true)
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