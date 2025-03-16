package com.fileflow.util;

import java.util.regex.Pattern;

public class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{3,50}$");

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$");

    private ValidationUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Validate email format
     *
     * @param email the email to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate username format
     *
     * @param username the username to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * Validate password strength
     *
     * @param password the password to validate
     * @return true if valid, false otherwise
     */
    public static boolean isStrongPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Validate that a string is not null or empty
     *
     * @param str the string to validate
     * @return true if not null or empty, false otherwise
     */
    public static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }
}