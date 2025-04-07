package com.fileflow.test.helpers;

import com.google.firebase.ErrorCode;
import com.google.firebase.auth.FirebaseAuthException;
import org.mockito.Mockito;

/**
 * Helper class for creating mock FirebaseAuthException instances in tests.
 * Since FirebaseAuthException has a complex constructor that's not easily accessible in tests,
 * this utility creates mocked instances with the desired behavior.
 */
public class MockFirebaseAuthException {

    /**
     * Creates a mock FirebaseAuthException with the specified error code and message.
     *
     * @param errorCode the Firebase Auth error code
     * @param message the error message
     * @return a mocked FirebaseAuthException
     */
    public static FirebaseAuthException create(String errorCode, String message) {
        FirebaseAuthException mockException = Mockito.mock(FirebaseAuthException.class);
        Mockito.when(mockException.getErrorCode()).thenReturn(ErrorCode.valueOf(errorCode));
        Mockito.when(mockException.getMessage()).thenReturn(message);
        return mockException;
    }

    /**
     * Common error codes used by Firebase Authentication.
     * These can be used to create realistic mock exceptions.
     */
    public static class ErrorCodes {
        public static final String INVALID_ID_TOKEN = "invalid-id-token";
        public static final String ID_TOKEN_EXPIRED = "id-token-expired";
        public static final String USER_DISABLED = "user-disabled";
        public static final String USER_NOT_FOUND = "user-not-found";
        public static final String INVALID_CLAIMS = "invalid-claims";
        public static final String SESSION_EXPIRED = "session-expired";
    }
}