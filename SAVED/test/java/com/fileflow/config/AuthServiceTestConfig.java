package com.fileflow.config;

import com.fileflow.service.auth.JwtService;
import com.fileflow.service.config.EnvPropertyService;
import com.fileflow.service.email.EmailService;
import com.fileflow.service.security.RateLimiterService;
import com.fileflow.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Test configuration specifically for AuthService tests
 * Provides all necessary mocks for AuthService tests
 */
@TestConfiguration
public class AuthServiceTestConfig {

    @Bean
    @Primary
    public DotenvConfig dotenvConfig() {
        return new DotenvConfig();
    }

    @Bean
    @Primary
    public EnvPropertyService envPropertyService() {
        return new EnvPropertyService(dotenvConfig(), null);
    }

    @Bean
    @Primary
    public AppConfig appConfig() {
        AppConfig config = new AppConfig();

        // Setup Security Config
        AppConfig.SecurityConfig securityConfig = new AppConfig.SecurityConfig();
        config.setSecurity(securityConfig);

        // Setup Password Strength
        AppConfig.PasswordStrength passwordStrength = new AppConfig.PasswordStrength();
        securityConfig.setPasswordStrength(passwordStrength);

        // Setup Email Config
        AppConfig.EmailConfig emailConfig = new AppConfig.EmailConfig();
        config.setEmail(emailConfig);

        // Setup MFA Config
        AppConfig.MfaConfig mfaConfig = new AppConfig.MfaConfig();
        config.setMfa(mfaConfig);

        // Setup Rate Limiting Config
        AppConfig.RateLimitingConfig rateLimitingConfig = new AppConfig.RateLimitingConfig();
        config.setRateLimiting(rateLimitingConfig);

        return config;
    }

    @Bean
    @Primary
    public EmailService emailService() {
        return Mockito.mock(EmailService.class);
    }

    @Bean
    @Primary
    public SecurityUtils securityUtils() {
        SecurityUtils mockUtils = Mockito.mock(SecurityUtils.class);
        Mockito.when(mockUtils.getClientIpAddress(Mockito.any(HttpServletRequest.class)))
                .thenReturn("127.0.0.1");
        return mockUtils;
    }

    @Bean
    @Primary
    public RateLimiterService rateLimiterService() {
        return Mockito.mock(RateLimiterService.class);
    }

    @Bean
    @Primary
    public HttpServletRequest httpServletRequest() {
        return Mockito.mock(HttpServletRequest.class);
    }

    @Bean
    @Primary
    public RedisTemplate<String, String> redisTemplate() {
        return Mockito.mock(RedisTemplate.class);
    }

    @Bean
    @Primary
    public JwtService jwtService() {
        return Mockito.mock(JwtService.class);
    }

    @Bean
    @Primary
    public JwtConfig jwtConfig() {
        JwtConfig config = new JwtConfig();
        config.setSecret("testsecretkeythatisatleast256bitslong0123456789abcdef");
        config.setExpiration(300000L);
        config.setRefreshExpiration(600000L);
        config.setHeader("Authorization");
        config.setPrefix("Bearer");
        return config;
    }
}