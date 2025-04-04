package com.fileflow.service.email;

import com.fileflow.model.User;

import java.util.concurrent.CompletableFuture;

/**
 * Service for sending emails to users
 */
public interface EmailService {

    /**
     * Send a welcome email to a new user with email verification link
     *
     * @param user User to send email to
     * @param verificationToken Email verification token
     * @return CompletableFuture with boolean success result
     */
    CompletableFuture<Boolean> sendWelcomeEmail(User user, String verificationToken);

    /**
     * Send a password reset email
     *
     * @param user User to send email to
     * @param token Password reset token
     * @return CompletableFuture with boolean success result
     */
    CompletableFuture<Boolean> sendPasswordResetEmail(User user, String token);

    /**
     * Send an account locked notification
     *
     * @param user User to send email to
     * @param lockoutMinutes Length of lockout in minutes
     * @return CompletableFuture with boolean success result
     */
    CompletableFuture<Boolean> sendAccountLockedEmail(User user, long lockoutMinutes);

    /**
     * Send a notification for unusual login activity
     *
     * @param user User to send email to
     * @param ipAddress IP address of the unusual login
     * @param location Location of the login if available
     * @param device Device information if available
     * @return CompletableFuture with boolean success result
     */
    CompletableFuture<Boolean> sendUnusualActivityEmail(User user, String ipAddress, String location, String device);
}