package com.fileflow.service.email;

import com.fileflow.config.AppConfig;
import com.fileflow.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Email service implementation for sending various email notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine;
    private final AppConfig appConfig;

    @Value("${spring.mail.properties.mail.smtp.from}")
    private String fromEmail;

    @Value("${app.email.sender-name:FileFlow}")
    private String senderName;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * Send a welcome email to a new user with email verification link
     *
     * @param user User to send the email to
     * @param verificationToken Email verification token
     * @return CompletableFuture<Boolean> indicating whether the email was sent successfully
     */
    @Override
    @Async
    public CompletableFuture<Boolean> sendWelcomeEmail(User user, String verificationToken) {
        String subject = "Welcome to FileFlow - Verify Your Email";

        // Ensure baseUrl doesn't have trailing slash
        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        // Construct proper verification link with correct path
        String verificationLink = cleanBaseUrl + "/auth/verify-email?token=" + verificationToken;

        // Log the link for debugging
        log.info("Generated verification link: {}", verificationLink);

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("username", user.getFirstName());
        templateModel.put("verificationLink", verificationLink);
        templateModel.put("baseUrl", cleanBaseUrl);
        templateModel.put("verificationToken", verificationToken);

        return sendHtmlEmail(user.getEmail(), subject, "welcome", templateModel);
    }

    /**
     * Send a password reset email with reset link
     *
     * @param user User to send the email to
     * @param token Password reset token
     * @return CompletableFuture<Boolean> indicating whether the email was sent successfully
     */
    @Override
    @Async
    public CompletableFuture<Boolean> sendPasswordResetEmail(User user, String token) {
        String subject = "FileFlow - Reset Your Password";

        // Ensure baseUrl doesn't have trailing slash
        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        // Properly construct the reset link
        String resetLink = cleanBaseUrl + "/auth/reset-password?token=" + token;
        log.info("Sending password reset email to: {} with reset link: {}", user.getEmail(), resetLink);

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("username", user.getFirstName() != null ? user.getFirstName() : "User");
        templateModel.put("resetLink", resetLink);
        templateModel.put("baseUrl", cleanBaseUrl);
        templateModel.put("token", token);

        return sendHtmlEmail(user.getEmail(), subject, "password-reset", templateModel);
    }

    /**
     * Send account locked notification email
     *
     * @param user User to send the email to
     * @param lockoutMinutes Duration of the lockout in minutes
     * @return CompletableFuture<Boolean> indicating whether the email was sent successfully
     */
    @Override
    @Async
    public CompletableFuture<Boolean> sendAccountLockedEmail(User user, long lockoutMinutes) {
        String subject = "FileFlow - Account Security Alert";

        // Ensure baseUrl doesn't have trailing slash
        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("username", user.getFirstName() != null ? user.getFirstName() : "User");
        templateModel.put("lockoutMinutes", lockoutMinutes);
        templateModel.put("baseUrl", cleanBaseUrl);
        templateModel.put("supportEmail", "support@fileflow.com");

        log.info("Sending account locked email to: {}", user.getEmail());
        return sendHtmlEmail(user.getEmail(), subject, "account-locked", templateModel);
    }

    /**
     * Send notification for unusual login activity
     *
     * @param user User to send the email to
     * @param ipAddress IP address from which the unusual activity was detected
     * @param location Geographic location of the IP address (if available)
     * @param device Device information from the user agent
     * @return CompletableFuture<Boolean> indicating whether the email was sent successfully
     */
    @Override
    @Async
    public CompletableFuture<Boolean> sendUnusualActivityEmail(User user, String ipAddress, String location, String device) {
        String subject = "FileFlow - Security Alert: Unusual Login Detected";

        // Ensure baseUrl doesn't have trailing slash
        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("username", user.getFirstName() != null ? user.getFirstName() : "User");
        templateModel.put("ipAddress", ipAddress);
        templateModel.put("location", location != null ? location : "Unknown location");
        templateModel.put("device", device != null ? device : "Unknown device");
        templateModel.put("time", java.time.LocalDateTime.now().toString());
        templateModel.put("baseUrl", cleanBaseUrl);
        templateModel.put("supportEmail", "support@fileflow.com");

        log.info("Sending unusual activity email to: {}", user.getEmail());
        return sendHtmlEmail(user.getEmail(), subject, "unusual-activity", templateModel);
    }

    /**
     * Send notification about password change
     *
     * @param user User to send the email to
     * @return CompletableFuture<Boolean> indicating whether the email was sent successfully
     */
    @Override
    @Async
    public CompletableFuture<Boolean> sendPasswordChangeNotification(User user) {
        String subject = "FileFlow - Your Password Has Been Changed";

        // Ensure baseUrl doesn't have trailing slash
        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("username", user.getFirstName() != null ? user.getFirstName() : "User");
        templateModel.put("baseUrl", cleanBaseUrl);
        templateModel.put("supportEmail", "support@fileflow.com");
        templateModel.put("changeTime", LocalDateTime.now().toString());
        templateModel.put("ipAddress", user.getLastLoginIp() != null ? user.getLastLoginIp() : "Unknown");

        log.info("Sending password change notification to: {}", user.getEmail());
        return sendHtmlEmail(user.getEmail(), subject, "password-changed", templateModel);
    }

    /**
     * Helper method to send HTML email using a template
     *
     * @param to Email recipient
     * @param subject Email subject
     * @param templateName Name of the Thymeleaf template to use
     * @param templateModel Model data to pass to the template
     * @return CompletableFuture<Boolean> indicating whether the email was sent successfully
     */
    @Async
    public CompletableFuture<Boolean> sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> templateModel) {
        if (!emailEnabled) {
            log.info("Email sending disabled. Would send email to: {}, subject: {}, template: {}", to, subject, templateName);
            log.debug("Template model: {}", templateModel);
            return CompletableFuture.completedFuture(true);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, senderName);
            helper.setTo(to);
            helper.setSubject(subject);

            // Process template
            Context context = new Context();
            templateModel.forEach(context::setVariable);
            String htmlContent = templateEngine.process("emails/" + templateName, context);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to: {}, subject: {}", to, subject);
            return CompletableFuture.completedFuture(true);

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send email to: {}, subject: {}", to, subject, e);
            return CompletableFuture.completedFuture(false);
        } catch (Exception e) {
            log.error("Unexpected error sending email to: {}, subject: {}", to, subject, e);
            return CompletableFuture.completedFuture(false);
        }
    }
}