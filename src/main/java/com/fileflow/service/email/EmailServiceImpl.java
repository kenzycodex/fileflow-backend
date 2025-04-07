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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
     * Send a welcome email to a new user
     */
    @Override
    @Async
    public CompletableFuture<Boolean> sendWelcomeEmail(User user, String verificationToken) {
        String subject = "Welcome to FileFlow - Verify Your Email";

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("username", user.getFirstName());
        // Fix: Updated to use /auth/verify-email path instead of just /verify-email
        templateModel.put("verificationLink", baseUrl + "/auth/verify-email?token=" + verificationToken);
        // Adding baseUrl and verificationToken separately for template flexibility
        templateModel.put("baseUrl", baseUrl);
        templateModel.put("verificationToken", verificationToken);

        return sendHtmlEmail(user.getEmail(), subject, "welcome", templateModel);
    }

    /**
     * Send a password reset email
     */
    @Override
    @Async
    public CompletableFuture<Boolean> sendPasswordResetEmail(User user, String token) {
        String subject = "FileFlow - Reset Your Password";

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("username", user.getFirstName());
        templateModel.put("resetLink", baseUrl + "/auth/reset-password?token=" + token);
        templateModel.put("resetUrl", baseUrl + "/auth/reset-password?token=" + token);
        // Adding baseUrl and token separately for template flexibility
        templateModel.put("baseUrl", baseUrl);
        templateModel.put("token", token);

        return sendHtmlEmail(user.getEmail(), subject, "password-reset", templateModel);
    }

    /**
     * Send an account locked notification
     */
    @Override
    @Async
    public CompletableFuture<Boolean> sendAccountLockedEmail(User user, long lockoutMinutes) {
        String subject = "FileFlow - Account Security Alert";

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("username", user.getFirstName());
        templateModel.put("lockoutMinutes", lockoutMinutes);

        return sendHtmlEmail(user.getEmail(), subject, "account-locked", templateModel);
    }

    /**
     * Send a notification for unusual login activity
     */
    @Override
    @Async
    public CompletableFuture<Boolean> sendUnusualActivityEmail(User user, String ipAddress, String location, String device) {
        String subject = "FileFlow - Security Alert: Unusual Login Detected";

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("username", user.getFirstName());
        templateModel.put("ipAddress", ipAddress);
        templateModel.put("location", location);
        templateModel.put("device", device);
        templateModel.put("time", java.time.LocalDateTime.now().toString());

        return sendHtmlEmail(user.getEmail(), subject, "unusual-activity", templateModel);
    }

    /**
     * Send an email to a user
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
        }
    }
}