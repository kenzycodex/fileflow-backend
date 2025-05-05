package com.fileflow.validation.validator;

import com.fileflow.validation.annotation.ValidFilename;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Validator for filenames
 */
public class FilenameValidator implements ConstraintValidator<ValidFilename, String> {

    // Pattern for invalid characters in filenames
    private static final Pattern INVALID_FILENAME_CHARS = Pattern.compile("[/\\\\:*?\"<>|]");

    @Override
    public boolean isValid(String filename, ConstraintValidatorContext context) {
        if (filename == null) {
            // Null values are handled by @NotNull if required
            return true;
        }

        // Check if filename contains invalid characters
        return !INVALID_FILENAME_CHARS.matcher(filename).find();
    }
}