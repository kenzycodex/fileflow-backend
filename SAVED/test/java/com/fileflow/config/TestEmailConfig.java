package com.fileflow.config;

import com.fileflow.service.email.EmailService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Test configuration for email components
 */
@TestConfiguration
@Profile("test")
public class TestEmailConfig {

    @Bean
    @Primary
    public JavaMailSender testJavaMailSender() {
        // Create a non-functional mail sender for tests
        return new JavaMailSenderImpl();
    }

    @Bean
    @Primary
    public SpringTemplateEngine testTemplateEngine() {
        // Return a mock template engine
        return Mockito.mock(SpringTemplateEngine.class);
    }

    @Bean
    @Primary
    public EmailService emailService() {
        // Return a mock email service
        return Mockito.mock(EmailService.class);
    }
}