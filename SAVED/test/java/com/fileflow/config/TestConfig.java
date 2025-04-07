package com.fileflow.config;

import com.fileflow.config.test.FixedEnvironment;
import com.fileflow.security.CustomUserDetailsService;
import com.fileflow.security.JwtAuthenticationEntryPoint;
import com.fileflow.security.JwtAuthenticationFilter;
import com.fileflow.security.JwtTokenProvider;
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
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.Mockito.when;

/**
 * Test configuration class to provide all necessary beans for testing
 * This configuration ensures all dependencies are correctly initialized for tests
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public DotenvConfig dotenvConfig() {
        return new DotenvConfig();
    }

    @Bean
    @Primary
    public Environment environment() {
        // Use a special fixed environment implementation to avoid NullPointerExceptions
        return new FixedEnvironment();
    }

    @Bean
    @Primary
    public EnvPropertyService envPropertyService(DotenvConfig dotenvConfig, Environment environment) {
        return new EnvPropertyService(dotenvConfig, environment);
    }

    @Bean
    @Primary
    public AppConfig appConfig() {
        AppConfig config = new AppConfig();

        // Initialize security config with default values
        AppConfig.SecurityConfig securityConfig = new AppConfig.SecurityConfig();
        AppConfig.PasswordStrength passwordStrength = new AppConfig.PasswordStrength();
        securityConfig.setPasswordStrength(passwordStrength);
        config.setSecurity(securityConfig);

        // Initialize email config
        AppConfig.EmailConfig emailConfig = new AppConfig.EmailConfig();
        config.setEmail(emailConfig);

        // Initialize MFA config
        AppConfig.MfaConfig mfaConfig = new AppConfig.MfaConfig();
        config.setMfa(mfaConfig);

        // Initialize rate limiting config
        AppConfig.RateLimitingConfig rateLimitingConfig = new AppConfig.RateLimitingConfig();
        config.setRateLimiting(rateLimitingConfig);

        return config;
    }

    @Bean
    @Primary
    public JwtConfig jwtConfig() {
        JwtConfig config = new JwtConfig();
        config.setSecret("testsecretkeythatisatleast256bitslong0123456789abcdef");
        config.setExpiration(300000L); // 5 minutes
        config.setRefreshExpiration(600000L); // 10 minutes
        config.setHeader("Authorization");
        config.setPrefix("Bearer");
        return config;
    }

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    @Primary
    public EmailService emailService() {
        return Mockito.mock(EmailService.class);
    }

    @Bean
    @Primary
    public SecurityUtils securityUtils() {
        SecurityUtils securityUtils = Mockito.mock(SecurityUtils.class);
        Mockito.when(securityUtils.getClientIpAddress(Mockito.any(HttpServletRequest.class)))
                .thenReturn("127.0.0.1");
        Mockito.when(securityUtils.getUserAgent(Mockito.any(HttpServletRequest.class)))
                .thenReturn("Mozilla/5.0 Test User Agent");
        return securityUtils;
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
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> redisTemplate = Mockito.mock(RedisTemplate.class);

        // Mock value operations
        ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Mock set operations
        SetOperations<String, String> setOperations = Mockito.mock(SetOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        return redisTemplate;
    }

    @Bean
    @Primary
    public JwtTokenProvider jwtTokenProvider() {
        return Mockito.mock(JwtTokenProvider.class);
    }

    @Bean
    @Primary
    public JwtService jwtService() {
        return Mockito.mock(JwtService.class);
    }

    @Bean
    @Primary
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return Mockito.mock(JwtAuthenticationFilter.class);
    }
}