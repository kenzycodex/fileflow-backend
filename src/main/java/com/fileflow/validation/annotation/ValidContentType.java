package com.fileflow.validation.annotation;

import com.fileflow.validation.validator.ContentTypeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation annotation for file content types
 */
@Documented
@Constraint(validatedBy = ContentTypeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidContentType {
    String message() default "Invalid content type";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /**
     * Array of allowed content types (e.g., "image/jpeg", "application/pdf")
     */
    String[] allowed() default {};

    /**
     * Allow all image types
     */
    boolean allowImages() default false;

    /**
     * Allow all document types
     */
    boolean allowDocuments() default false;

    /**
     * Allow all video types
     */
    boolean allowVideos() default false;

    /**
     * Allow all audio types
     */
    boolean allowAudio() default false;
}