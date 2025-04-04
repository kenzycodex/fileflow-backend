package com.fileflow.service.auth;

import com.fileflow.config.AppConfig;
import com.fileflow.exception.BadRequestException;
import com.fileflow.model.User;
import com.fileflow.model.UserMfa;
import com.fileflow.repository.UserMfaRepository;
import com.fileflow.repository.UserRepository;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MfaServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMfaRepository userMfaRepository;

    @Mock
    private AppConfig appConfig;

    @Mock
    private AppConfig.MfaConfig mfaConfig;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private MfaService mfaService;

    private User testUser;
    private UserMfa testUserMfa;
    private static final Long USER_ID = 1L;
    private static final String SECRET = "ABCDEFGHIJKLMNOP";
    private static final String VERIFICATION_CODE = "123456";

    @BeforeEach
    public void setup() {
        // Setup test user
        testUser = User.builder()
                .id(USER_ID)
                .username("testuser")
                .email("test@example.com")
                .build();

        // Setup test user MFA
        testUserMfa = UserMfa.builder()
                .id(1L)
                .user(testUser)
                .secret(SECRET)
                .enabled(false)
                .createdAt(LocalDateTime.now())
                .build();

        // Setup AppConfig.MfaConfig
        when(appConfig.getMfa()).thenReturn(mfaConfig);
        when(mfaConfig.getIssuer()).thenReturn("FileFlow");
        when(mfaConfig.getCodeLength()).thenReturn(6);
        when(mfaConfig.getCodeExpirySeconds()).thenReturn(30);

        // Setup Redis template
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Mock the verification for testing
        CodeVerifier codeVerifier = mock(CodeVerifier.class);
        when(codeVerifier.isValidCode(eq(SECRET), eq("123456"))).thenReturn(true);
        when(codeVerifier.isValidCode(eq(SECRET), eq("999999"))).thenReturn(false);

        // Use ReflectionTestUtils to override the verification method since we can't easily mock it
        ReflectionTestUtils.setField(mfaService, "codeVerifier", codeVerifier);
    }

    @Test
    public void testGenerateMfaSecret_newUser() {
        // Setup mocks
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(userMfaRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userMfaRepository.save(any(UserMfa.class))).thenReturn(testUserMfa);

        // Call the service method
        String secret = mfaService.generateMfaSecret(USER_ID);

        // Verify the result
        assertNotNull(secret);
        verify(userMfaRepository).save(any(UserMfa.class));
    }

    @Test
    public void testGenerateMfaSecret_existingUser() {
        // Setup mocks
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(userMfaRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testUserMfa));
        when(userMfaRepository.save(any(UserMfa.class))).thenReturn(testUserMfa);

        // Call the service method
        String secret = mfaService.generateMfaSecret(USER_ID);

        // Verify the result
        assertNotNull(secret);
        verify(userMfaRepository).save(any(UserMfa.class));
    }

    @Test
    public void testGenerateMfaSecret_userNotFound() {
        // Setup mocks
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // Verify exception
        assertThrows(BadRequestException.class, () -> {
            mfaService.generateMfaSecret(USER_ID);
        });

        // Verify repository calls
        verify(userMfaRepository, never()).save(any(UserMfa.class));
    }

    @Test
    public void testGenerateQrCodeImageUri() {
        // Call the service method
        String qrCodeUri = mfaService.generateQrCodeImageUri(SECRET, "testuser");

        // Verify the result
        assertNotNull(qrCodeUri);
        assertTrue(qrCodeUri.startsWith("data:image/png;base64,"));
    }

    @Test
    public void testVerifyCode_valid() {
        // Call the service method
        boolean result = mfaService.verifyCode(SECRET, "123456");

        // Verify the result
        assertTrue(result);
    }

    @Test
    public void testVerifyCode_invalid() {
        // Call the service method
        boolean result = mfaService.verifyCode(SECRET, "999999");

        // Verify the result
        assertFalse(result);
    }

    @Test
    public void testEnableMfa_validCode() {
        // Setup mocks
        when(userMfaRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testUserMfa));
        when(userMfaRepository.save(any(UserMfa.class))).thenReturn(testUserMfa);

        // Call the service method
        mfaService.enableMfa(USER_ID, "123456");

        // Verify the result
        assertTrue(testUserMfa.isEnabled());
        assertNotNull(testUserMfa.getVerifiedAt());
        verify(userMfaRepository).save(testUserMfa);
    }

    @Test
    public void testEnableMfa_invalidCode() {
        // Setup mocks
        when(userMfaRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testUserMfa));

        // Verify exception
        assertThrows(BadRequestException.class, () -> {
            mfaService.enableMfa(USER_ID, "999999");
        });

        // Verify repository calls
        verify(userMfaRepository, never()).save(any(UserMfa.class));
    }

    @Test
    public void testEnableMfa_mfaNotSetUp() {
        // Setup mocks
        when(userMfaRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // Verify exception
        assertThrows(BadRequestException.class, () -> {
            mfaService.enableMfa(USER_ID, "123456");
        });

        // Verify repository calls
        verify(userMfaRepository, never()).save(any(UserMfa.class));
    }

    @Test
    public void testDisableMfa() {
        // Setup mocks
        when(userMfaRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testUserMfa));
        when(userMfaRepository.save(any(UserMfa.class))).thenReturn(testUserMfa);

        // Call the service method
        mfaService.disableMfa(USER_ID);

        // Verify the result
        assertFalse(testUserMfa.isEnabled());
        verify(userMfaRepository).save(testUserMfa);
    }

    @Test
    public void testDisableMfa_mfaNotSetUp() {
        // Setup mocks
        when(userMfaRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // Verify exception
        assertThrows(BadRequestException.class, () -> {
            mfaService.disableMfa(USER_ID);
        });

        // Verify repository calls
        verify(userMfaRepository, never()).save(any(UserMfa.class));
    }

    @Test
    public void testIsMfaEnabled_true() {
        // Setup mocks
        testUserMfa.setEnabled(true);
        when(userMfaRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testUserMfa));

        // Call the service method
        boolean result = mfaService.isMfaEnabled(USER_ID);

        // Verify the result
        assertTrue(result);
    }

    @Test
    public void testIsMfaEnabled_false() {
        // Setup mocks
        testUserMfa.setEnabled(false);
        when(userMfaRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testUserMfa));

        // Call the service method
        boolean result = mfaService.isMfaEnabled(USER_ID);

        // Verify the result
        assertFalse(result);
    }

    @Test
    public void testIsMfaEnabled_notSetUp() {
        // Setup mocks
        when(userMfaRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // Call the service method
        boolean result = mfaService.isMfaEnabled(USER_ID);

        // Verify the result
        assertFalse(result);
    }

    @Test
    public void testGenerateEmailVerificationCode() {
        // Setup mocks
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        // Call the service method
        String code = mfaService.generateEmailVerificationCode(USER_ID);

        // Verify the result
        assertNotNull(code);
        assertEquals(6, code.length());
        verify(valueOperations).set(eq("mfa:code:" + USER_ID), eq(code), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void testVerifyEmailCode_valid() {
        // Setup mocks
        when(valueOperations.get("mfa:code:" + USER_ID)).thenReturn(VERIFICATION_CODE);
        doNothing().when(redisTemplate).delete("mfa:code:" + USER_ID);

        // Call the service method
        boolean result = mfaService.verifyEmailCode(USER_ID, VERIFICATION_CODE);

        // Verify the result
        assertTrue(result);
        verify(redisTemplate).delete("mfa:code:" + USER_ID);
    }

    @Test
    public void testVerifyEmailCode_invalid() {
        // Setup mocks
        when(valueOperations.get("mfa:code:" + USER_ID)).thenReturn(VERIFICATION_CODE);

        // Call the service method
        boolean result = mfaService.verifyEmailCode(USER_ID, "999999");

        // Verify the result
        assertFalse(result);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    public void testVerifyEmailCode_expired() {
        // Setup mocks
        when(valueOperations.get("mfa:code:" + USER_ID)).thenReturn(null);

        // Call the service method
        boolean result = mfaService.verifyEmailCode(USER_ID, VERIFICATION_CODE);

        // Verify the result
        assertFalse(result);
        verify(redisTemplate, never()).delete(anyString());
    }
}