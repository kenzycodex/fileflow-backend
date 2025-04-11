package com.fileflow.service.email;

import com.fileflow.model.User;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for sending emails
 */
public interface EmailService {

    /**
     * Send welcome email with verification link
     *
     * @param user User to send email to
     * @param verificationToken Email verification token
     * @return CompletableFuture<Boolean> indicating whether the email was sent successfully
     */
    CompletableFuture<Boolean> sendWelcomeEmail(User user, String verificationToken);

    /**
     * Send password reset email with reset link
     *
     * @param user User to send email to
     * @param token Password reset token
     * @return CompletableFuture<Boolean> indicating whether the email was sent successfully
     */
    CompletableFuture<Boolean> sendPasswordResetEmail(User user, String token);

    /**
     * Send account locked notification email
     *
     * @param user User to send email to
     * @param lockoutMinutes Duration of the account lockout in minutes
     * @return CompletableFuture<Boolean> indicating whether the email was sent successfully
     */
    CompletableFuture<Boolean> sendAccountLockedEmail(User user, long lockoutMinutes);

    /**
     * Send unusual login activity notification email
     *
     * @param user User to send email to
     * @param ipAddress IP address of the unusual login
     * @param location Location information (if available)
     * @param device Device information from user agent
     * @return CompletableFuture<Boolean> indicating whether the email was sent successfully
     */
    CompletableFuture<Boolean> sendUnusualActivityEmail(User user, String ipAddress, String location, String device);

    /**
     * Send notification about password change
     *
     * @param user User to send the email to
     * @return CompletableFuture<Boolean> indicating whether the email was sent successfully
     */
    CompletableFuture<Boolean> sendPasswordChangeNotification(User user);
}