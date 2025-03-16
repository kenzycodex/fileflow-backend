package com.fileflow.validation.validator;

import com.fileflow.validation.annotation.ValidEmail;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Email validator implementation for ValidEmail annotation
 */
public class EmailValidator implements ConstraintValidator<ValidEmail, String> {

    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    private static final Pattern pattern = Pattern.compile(EMAIL_PATTERN);

    @Override
    public void initialize(ValidEmail constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        return email != null && pattern.matcher(email).matches();
    }
}
