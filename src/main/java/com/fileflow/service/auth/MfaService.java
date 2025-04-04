package com.fileflow.service.auth;

import com.fileflow.config.AppConfig;
import com.fileflow.exception.BadRequestException;
import com.fileflow.model.User;
import com.fileflow.model.UserMfa;
import com.fileflow.repository.UserMfaRepository;
import com.fileflow.repository.UserRepository;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling Multi-Factor Authentication (MFA)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MfaService {

    private final UserRepository userRepository;
    private final UserMfaRepository userMfaRepository;
    private final AppConfig appConfig;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String MFA_CODE_PREFIX = "mfa:code:";
    private static final int DEFAULT_CODE_VALID_SECONDS = 60;

    /**
     * Generate a new MFA secret for a user
     */
    @Transactional
    public String generateMfaSecret(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Generate a new secret
        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        String secret = secretGenerator.generate();

        // Save to database
        UserMfa userMfa = userMfaRepository.findByUserId(userId)
                .orElse(UserMfa.builder().user(user).build());

        userMfa.setSecret(secret);
        userMfa.setEnabled(false);
        userMfa.setCreatedAt(LocalDateTime.now());

        userMfaRepository.save(userMfa);

        return secret;
    }

    /**
     * Generate a QR code for MFA setup
     */
    public String generateQrCodeImageUri(String secret, String username) {
        QrData data = new QrData.Builder()
                .label(username)
                .secret(secret)
                .issuer(appConfig.getMfa().getIssuer())
                .algorithm(HashingAlgorithm.SHA1)
                .digits(appConfig.getMfa().getCodeLength())
                .period(appConfig.getMfa().getCodeExpirySeconds())
                .build();

        QrGenerator qrGenerator = new ZxingPngQrGenerator();
        byte[] imageData;
        try {
            imageData = qrGenerator.generate(data);
        } catch (QrGenerationException e) {
            log.error("Error generating QR code", e);
            throw new BadRequestException("Could not generate QR code");
        }

        return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageData);
    }

    /**
     * Verify a TOTP code against a secret
     */
    public boolean verifyCode(String secret, String code) {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

        return verifier.isValidCode(secret, code);
    }

    /**
     * Enable MFA for a user after verification
     */
    @Transactional
    public void enableMfa(Long userId, String verificationCode) {
        UserMfa userMfa = userMfaRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("MFA not set up for this user"));

        if (!verifyCode(userMfa.getSecret(), verificationCode)) {
            throw new BadRequestException("Invalid verification code");
        }

        userMfa.setEnabled(true);
        userMfa.setVerifiedAt(LocalDateTime.now());
        userMfaRepository.save(userMfa);

        log.info("MFA enabled for user ID: {}", userId);
    }

    /**
     * Disable MFA for a user
     */
    @Transactional
    public void disableMfa(Long userId) {
        UserMfa userMfa = userMfaRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("MFA not set up for this user"));

        userMfa.setEnabled(false);
        userMfaRepository.save(userMfa);

        log.info("MFA disabled for user ID: {}", userId);
    }

    /**
     * Check if MFA is enabled for a user
     */
    public boolean isMfaEnabled(Long userId) {
        return userMfaRepository.findByUserId(userId)
                .map(UserMfa::isEnabled)
                .orElse(false);
    }

    /**
     * Generate a temporary verification code for email-based MFA
     */
    public String generateEmailVerificationCode(Long userId) {
        // Generate a 6-digit code
        int code = 100000 + (int)(Math.random() * 900000);
        String verificationCode = String.valueOf(code);

        // Store in Redis with expiration
        String key = MFA_CODE_PREFIX + userId;
        redisTemplate.opsForValue().set(key, verificationCode, DEFAULT_CODE_VALID_SECONDS, TimeUnit.SECONDS);

        return verificationCode;
    }

    /**
     * Verify an email verification code
     */
    public boolean verifyEmailCode(Long userId, String code) {
        String key = MFA_CODE_PREFIX + userId;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode != null && storedCode.equals(code)) {
            // Delete the code once used
            redisTemplate.delete(key);
            return true;
        }

        return false;
    }
}