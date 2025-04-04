package com.fileflow.validation.validator;

import com.fileflow.config.AppConfig;
import com.fileflow.validation.annotation.StrongPassword;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Validator for strong password requirements
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private int minLength;
    private boolean requireUppercase;
    private boolean requireLowercase;
    private boolean requireDigits;
    private boolean requireSpecialChar;

    @Autowired
    private AppConfig appConfig;

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        // Use annotation values as defaults but override with app config if available
        this.minLength = constraintAnnotation.minLength();
        this.requireUppercase = constraintAnnotation.requireUppercase();
        this.requireLowercase = constraintAnnotation.requireLowercase();
        this.requireDigits = constraintAnnotation.requireDigits();
        this.requireSpecialChar = constraintAnnotation.requireSpecialChar();

        // Override with application config if available
        if (appConfig != null) {
            AppConfig.PasswordStrength config = appConfig.getSecurity().getPasswordStrength();
            this.minLength = config.getMinLength();
            this.requireUppercase = config.isRequireUppercase();
            this.requireLowercase = config.isRequireLowercase();
            this.requireDigits = config.isRequireDigits();
            this.requireSpecialChar = config.isRequireSpecial();
        }
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }

        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasLowercase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

        StringBuilder messageBuilder = new StringBuilder();
        boolean isValid = true;

        // Check length
        if (password.length() < minLength) {
            messageBuilder.append("Password must be at least ").append(minLength).append(" characters long. ");
            isValid = false;
        }

        // Check uppercase
        if (requireUppercase && !hasUppercase) {
            messageBuilder.append("Password must contain at least one uppercase letter. ");
            isValid = false;
        }

        // Check lowercase
        if (requireLowercase && !hasLowercase) {
            messageBuilder.append("Password must contain at least one lowercase letter. ");
            isValid = false;
        }

        // Check digits
        if (requireDigits && !hasDigit) {
            messageBuilder.append("Password must contain at least one digit. ");
            isValid = false;
        }

        // Check special characters
        if (requireSpecialChar && !hasSpecialChar) {
            messageBuilder.append("Password must contain at least one special character. ");
            isValid = false;
        }

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(messageBuilder.toString().trim())
                    .addConstraintViolation();
        }

        return isValid;
    }
}