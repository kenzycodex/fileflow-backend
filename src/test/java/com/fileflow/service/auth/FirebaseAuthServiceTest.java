package com.fileflow.service.auth;

import com.fileflow.dto.response.auth.UserResponse;
import com.fileflow.exception.BadRequestException;
import com.fileflow.exception.UnauthorizedException;
import com.fileflow.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FirebaseAuthServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private FirebaseToken firebaseToken;

    @Mock
    private FirebaseAuth firebaseAuth;

    @InjectMocks
    private FirebaseAuthService firebaseAuthService;

    private final String validToken = "valid-firebase-token";
    private final String uid = "firebase-user-id";
    private final String email = "test@example.com";
    private final String name = "Test User";
    private final String provider = "google.com";

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(firebaseAuthService, "firebaseEnabled", true);

        // Make mocks lenient for different test scenarios
        lenient().when(authService.createFirebaseUser(
                        anyString(), anyString(), anyString(),
                        anyString(), anyString(), anyString(), anyString()))
                .thenReturn(UserResponse.builder()
                        .id(1L)
                        .email(email)
                        .username("testuser")
                        .build());
    }

    @Test
    public void testAuthenticateWithFirebaseDisabled() {
        // Set Firebase disabled
        ReflectionTestUtils.setField(firebaseAuthService, "firebaseEnabled", false);

        // Test that exception is thrown when Firebase is disabled
        assertThrows(BadRequestException.class, () -> {
            firebaseAuthService.authenticateWithFirebase(validToken);
        });
    }

    @Test
    public void testAuthenticateWithFirebaseTokenVerificationFailed() throws FirebaseAuthException {
        try (MockedStatic<FirebaseAuth> firebaseAuthMock = mockStatic(FirebaseAuth.class)) {
            // Mock FirebaseAuth
            firebaseAuthMock.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);

            // Create a simple mocked exception
            Exception mockException = mock(FirebaseAuthException.class);
            when(mockException.getMessage()).thenReturn("Invalid token");

            // Make verifyIdToken throw the exception
            when(firebaseAuth.verifyIdToken(anyString())).thenThrow(mockException);

            // Test that UnauthorizedException is thrown for invalid token
            UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
                firebaseAuthService.authenticateWithFirebase(validToken);
            });

            // Verify the exception message
            assertTrue(exception.getMessage().contains("Invalid Firebase token"));
        }
    }

    @Test
    public void testAuthenticateWithFirebaseExistingUser() {
        try (MockedStatic<FirebaseAuth> firebaseAuthMock = mockStatic(FirebaseAuth.class)) {
            // Mock FirebaseAuth and token
            firebaseAuthMock.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            lenient().when(firebaseAuth.verifyIdToken(validToken)).thenReturn(firebaseToken);
            lenient().when(firebaseToken.getUid()).thenReturn(uid);
            lenient().when(firebaseToken.getEmail()).thenReturn(email);
            lenient().when(firebaseToken.getName()).thenReturn(name);
            lenient().when(firebaseToken.getIssuer()).thenReturn(provider);

            Map<String, Object> claims = new HashMap<>();
            claims.put("picture", "https://example.com/profile.jpg");
            lenient().when(firebaseToken.getClaims()).thenReturn(claims);

            // Mock existing user
            User existingUser = User.builder()
                    .id(1L)
                    .email(email)
                    .username("testuser")
                    .build();
            when(authService.findUserByEmail(email)).thenReturn(Optional.of(existingUser));

            UserResponse expectedResponse = UserResponse.builder()
                    .id(1L)
                    .email(email)
                    .username("testuser")
                    .build();
            when(authService.authenticateFirebaseUser(1L)).thenReturn(expectedResponse);

            // Test authentication with existing user
            UserResponse response = firebaseAuthService.authenticateWithFirebase(validToken);

            // Verify
            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals(email, response.getEmail());

            // Verify the user was updated with Firebase UID if needed
            verify(authService).updateFirebaseUid(eq(1L), eq(uid), eq(provider));
            verify(authService).authenticateFirebaseUser(1L);
        } catch (FirebaseAuthException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testAuthenticateWithFirebaseNewUser() throws FirebaseAuthException {
        try (MockedStatic<FirebaseAuth> firebaseAuthMock = mockStatic(FirebaseAuth.class)) {
            // Mock FirebaseAuth and token
            firebaseAuthMock.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            lenient().when(firebaseAuth.verifyIdToken(validToken)).thenReturn(firebaseToken);
            lenient().when(firebaseToken.getUid()).thenReturn(uid);
            lenient().when(firebaseToken.getEmail()).thenReturn(email);
            lenient().when(firebaseToken.getName()).thenReturn(name);
            lenient().when(firebaseToken.getIssuer()).thenReturn(provider);

            Map<String, Object> claims = new HashMap<>();
            claims.put("picture", "https://example.com/profile.jpg");
            lenient().when(firebaseToken.getClaims()).thenReturn(claims);

            // Mock new user (user doesn't exist)
            when(authService.findUserByEmail(email)).thenReturn(Optional.empty());

            UserResponse expectedResponse = UserResponse.builder()
                    .id(1L)
                    .email(email)
                    .username("test")
                    .firstName("Test")
                    .lastName("User")
                    .build();

            when(authService.createFirebaseUser(
                    eq(uid),
                    eq(email),
                    anyString(),
                    eq("Test"),
                    eq("User"),
                    anyString(),
                    eq(provider))).thenReturn(expectedResponse);

            // Test authentication with new user
            UserResponse response = firebaseAuthService.authenticateWithFirebase(validToken);

            // Verify
            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals(email, response.getEmail());

            // Verify a new user was created
            verify(authService, never()).updateFirebaseUid(any(), any(), any());
            verify(authService, never()).authenticateFirebaseUser(any());
            verify(authService).createFirebaseUser(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Test
    public void testAuthenticateWithNullEmail() {
        try (MockedStatic<FirebaseAuth> firebaseAuthMock = mockStatic(FirebaseAuth.class)) {
            // Mock FirebaseAuth and token with null email
            firebaseAuthMock.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            lenient().when(firebaseAuth.verifyIdToken(validToken)).thenReturn(firebaseToken);
            lenient().when(firebaseToken.getUid()).thenReturn(uid);
            // Return null for email
            lenient().when(firebaseToken.getEmail()).thenReturn(null);
            lenient().when(firebaseToken.getName()).thenReturn(name);
            lenient().when(firebaseToken.getIssuer()).thenReturn(provider);

            Map<String, Object> claims = new HashMap<>();
            claims.put("picture", "https://example.com/profile.jpg");
            lenient().when(firebaseToken.getClaims()).thenReturn(claims);

            // Test that BadRequestException is thrown for null email
            BadRequestException exception = assertThrows(BadRequestException.class, () -> {
                firebaseAuthService.authenticateWithFirebase(validToken);
            });

            // Verify the exception message
            assertEquals("Firebase authentication requires an email", exception.getMessage());
        } catch (FirebaseAuthException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testAuthenticateWithIllegalArgumentException() {
        try (MockedStatic<FirebaseAuth> firebaseAuthMock = mockStatic(FirebaseAuth.class)) {
            // Mock FirebaseAuth to throw IllegalArgumentException
            firebaseAuthMock.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            when(firebaseAuth.verifyIdToken(anyString())).thenThrow(new IllegalArgumentException("Invalid token format"));

            // Test that BadRequestException is thrown for invalid format
            BadRequestException exception = assertThrows(BadRequestException.class, () -> {
                firebaseAuthService.authenticateWithFirebase(validToken);
            });

            // Verify the exception message
            assertEquals("Invalid Firebase token format", exception.getMessage());
        } catch (FirebaseAuthException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGenerateUsernameFromInvalidEmail() {
        try (MockedStatic<FirebaseAuth> firebaseAuthMock = mockStatic(FirebaseAuth.class)) {
            // Mock FirebaseAuth and token with invalid email format
            firebaseAuthMock.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            lenient().when(firebaseAuth.verifyIdToken(validToken)).thenReturn(firebaseToken);
            lenient().when(firebaseToken.getUid()).thenReturn(uid);
            lenient().when(firebaseToken.getEmail()).thenReturn("invalid-email-format"); // Invalid format
            lenient().when(firebaseToken.getName()).thenReturn(name);
            lenient().when(firebaseToken.getIssuer()).thenReturn(provider);

            Map<String, Object> claims = new HashMap<>();
            lenient().when(firebaseToken.getClaims()).thenReturn(claims);

            // Mock that we're creating a new user
            when(authService.findUserByEmail(anyString())).thenReturn(Optional.empty());

            // Mock the user creation with more specific parameters
            UserResponse expectedResponse = UserResponse.builder()
                    .id(1L)
                    .email("invalid-email-format")
                    .username("invalidemailformat")
                    .build();

            // Use more specific argument matching
            when(authService.createFirebaseUser(
                    eq(uid),
                    eq("invalid-email-format"),
                    anyString(),
                    eq("Test"),
                    eq("User"),
                    isNull(),
                    eq(provider)))
                    .thenReturn(expectedResponse);

            // Test the method calls our fallback username generation
            UserResponse response = firebaseAuthService.authenticateWithFirebase(validToken);

            // Cannot easily verify the exact username, but we can verify the user was created
            assertNotNull(response);

            // Use more specific verification
            verify(authService).createFirebaseUser(
                    eq(uid),
                    eq("invalid-email-format"),
                    anyString(),
                    eq("Test"),
                    eq("User"),
                    isNull(),
                    eq(provider));
        } catch (FirebaseAuthException e) {
            throw new RuntimeException(e);
        }
    }
}