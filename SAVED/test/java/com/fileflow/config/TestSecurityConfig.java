package com.fileflow.config;

import com.fileflow.security.CustomUserDetailsService;
import com.fileflow.security.JwtAuthenticationEntryPoint;
import com.fileflow.security.JwtAuthenticationFilter;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

/**
 * Test security configuration that disables security for tests
 * and provides mock beans for security components.
 *
 * Note: This configuration avoids defining jwtAuthenticationFilter
 * to prevent bean definition conflicts with TestConfig.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/**").permitAll());

        return http.build();
    }

    @Bean
    @Primary
    public StringHttpMessageConverter stringHttpMessageConverter() {
        StringHttpMessageConverter converter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        converter.setSupportedMediaTypes(
                Arrays.asList(MediaType.TEXT_PLAIN, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON));
        return converter;
    }

    @Bean
    @Primary
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(
                Collections.singletonList(MediaType.APPLICATION_JSON));
        return converter;
    }

    // Note: jwtAuthenticationFilter bean is deliberately removed from here
    // to avoid conflicts with TestConfig

    @Bean
    @Primary
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return Mockito.mock(JwtAuthenticationEntryPoint.class);
    }

    @Bean
    @Primary
    public CustomUserDetailsService customUserDetailsService() {
        return Mockito.mock(CustomUserDetailsService.class);
    }

    @Bean
    @Primary
    public AuthenticationManager authenticationManager() {
        return Mockito.mock(AuthenticationManager.class);
    }
}