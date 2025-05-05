package com.fileflow.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply rate limiting to a controller method
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Type of rate limit to apply
     */
    LimitType type() default LimitType.API;

    /**
     * Key resolver to use (default is IP address)
     */
    KeyResolver keyResolver() default KeyResolver.IP;

    /**
     * Rate limit types
     */
    enum LimitType {
        API,        // Standard API rate limit
        LOGIN,      // Login attempt rate limit
        SIGNUP,     // Signup attempt rate limit
        PASSWORD,   // Password reset rate limit
        TOKEN       // Token validation rate limit
    }

    /**
     * Key resolver types
     */
    enum KeyResolver {
        IP,         // Use client IP address
        USER,       // Use authenticated user ID
        CUSTOM      // Use custom key resolver (requires implementation)
    }
}