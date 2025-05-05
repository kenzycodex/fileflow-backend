package com.fileflow.validation.annotation;

import com.fileflow.validation.validator.FilenameValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation annotation for filenames
 */
@Documented
@Constraint(validatedBy = FilenameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFilename {
    String message() default "Invalid filename. Filenames cannot contain: /\\:*?\"<>|";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}