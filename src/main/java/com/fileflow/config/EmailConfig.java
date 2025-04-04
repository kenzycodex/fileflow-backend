package com.fileflow.config;

import com.fileflow.service.config.EnvPropertyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.util.Properties;

/**
 * Configuration for email sending
 */
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class EmailConfig {

    private final AppConfig appConfig;
    private final EnvPropertyService envPropertyService;

    @Value("${spring.mail.host:localhost}")
    private String host;

    @Value("${spring.mail.port:25}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${spring.mail.properties.mail.smtp.auth:false}")
    private String smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}")
    private String starttlsEnable;

    @Value("${spring.mail.properties.mail.smtp.from:noreply@fileflow.com}")
    private String from;

    @Value("${app.email.enabled:false}")
    private boolean enabled;

    /**
     * Configure JavaMailSender
     */
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // If email is disabled, return with minimal configuration
        if (!enabled) {
            return mailSender;
        }

        // Configure mail server
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        // Configure mail properties
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", smtpAuth);
        props.put("mail.smtp.starttls.enable", starttlsEnable);
        props.put("mail.debug", envPropertyService.getBooleanProperty("MAIL_DEBUG", false));
        props.put("mail.smtp.from", from);

        return mailSender;
    }

    /**
     * For development, you can use a tool like MailHog or MailDev
     * to catch and view emails without actually sending them
     */
    @Bean
    @Profile("dev")
    @Primary
    public JavaMailSender devMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        boolean mailEnabled = envPropertyService.getBooleanProperty("EMAIL_ENABLED", false);

        if (!mailEnabled) {
            return mailSender;
        }

        // Configure for local SMTP server like MailHog (http://github.com/mailhog/MailHog)
        mailSender.setHost(envPropertyService.getProperty("MAIL_HOST", "localhost"));
        mailSender.setPort(envPropertyService.getIntProperty("MAIL_PORT", 1025)); // MailHog default port

        // No authentication for dev mail servers typically
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "false");
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.debug", "true");
        props.put("mail.smtp.from", envPropertyService.getProperty("MAIL_FROM", "dev@fileflow.com"));

        return mailSender;
    }

    /**
     * Configure Thymeleaf template resolver for HTML emails
     */
    @Bean
    public ITemplateResolver emailTemplateResolver() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);
        return templateResolver;
    }

    /**
     * Configure Thymeleaf template engine
     */
    @Bean
    @Primary
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(emailTemplateResolver());
        return templateEngine;
    }
}