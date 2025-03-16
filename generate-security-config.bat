@echo off
setlocal enabledelayedexpansion

echo ======================================================
echo Generating new SecurityConfig template
echo ======================================================

set "CONFIG_PATH=%~dp0src\main\java\com\fileflow\config\SecurityConfig.java.new"

echo Creating template at: %CONFIG_PATH%
echo.

(
echo package com.fileflow.config;
echo.
echo import com.fileflow.security.JwtAuthenticationEntryPoint;
echo import com.fileflow.security.JwtAuthenticationFilter;
echo import org.springframework.beans.factory.annotation.Autowired;
echo import org.springframework.context.annotation.Bean;
echo import org.springframework.context.annotation.Configuration;
echo import org.springframework.security.authentication.AuthenticationManager;
echo import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
echo import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
echo import org.springframework.security.config.annotation.web.builders.HttpSecurity;
echo import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
echo import org.springframework.security.config.http.SessionCreationPolicy;
echo import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
echo import org.springframework.security.crypto.password.PasswordEncoder;
echo import org.springframework.security.web.SecurityFilterChain;
echo import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
echo import org.springframework.web.cors.CorsConfiguration;
echo import org.springframework.web.cors.CorsConfigurationSource;
echo import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
echo.
echo import java.util.Arrays;
echo.
echo @Configuration
echo @EnableWebSecurity
echo @EnableMethodSecurity
echo public class SecurityConfig {
echo.
echo     @Autowired
echo     private JwtAuthenticationEntryPoint unauthorizedHandler;
echo.
echo     @Autowired
echo     private JwtAuthenticationFilter jwtAuthenticationFilter;
echo.
echo     @Bean
echo     public PasswordEncoder passwordEncoder() {
echo         return new BCryptPasswordEncoder();
echo     }
echo.
echo     @Bean
echo     public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
echo         return authenticationConfiguration.getAuthenticationManager();
echo     }
echo.
echo     @Bean
echo     public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
echo         http
echo             .cors(cors -> cors.configurationSource(corsConfigurationSource()))
echo             .csrf(csrf -> csrf.disable())
echo             .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
echo             .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
echo             .authorizeHttpRequests(authorize -> authorize
echo                 .requestMatchers("/api/auth/**", "/api/public/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
echo                 .anyRequest().authenticated()
echo             );
echo.
echo         // Add JWT filter
echo         http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
echo.
echo         return http.build();
echo     }
echo.
echo     @Bean
echo     public CorsConfigurationSource corsConfigurationSource() {
echo         CorsConfiguration configuration = new CorsConfiguration();
echo         configuration.setAllowedOrigins(Arrays.asList("*"));
echo         configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
echo         configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
echo         configuration.setExposedHeaders(Arrays.asList("x-auth-token"));
echo         UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
echo         source.registerCorsConfiguration("/**", configuration);
echo         return source;
echo     }
echo }
) > "%CONFIG_PATH%"

echo New SecurityConfig template created at:
echo %CONFIG_PATH%
echo.
echo Please review and modify this file as needed, then rename it to replace
echo your existing SecurityConfig.java file.
echo ======================================================

pause