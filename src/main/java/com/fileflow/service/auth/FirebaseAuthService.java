package com.fileflow.service.auth;

import com.fileflow.dto.response.auth.UserResponse;
import com.fileflow.exception.BadRequestException;
import com.fileflow.exception.UnauthorizedException;
import com.fileflow.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseAuthService {

    private final AuthService authService;

    @Value("${app.firebase.enabled:false}")
    private boolean firebaseEnabled;

    /**
     * Verify the Firebase ID token and authenticate or create a user
     * @param idToken Firebase ID token
     * @return JWT response with user details and tokens
     */
    public UserResponse authenticateWithFirebase(String idToken) {
        if (!firebaseEnabled) {
            throw new BadRequestException("Firebase authentication is not enabled");
        }

        try {
            // Verify the Firebase ID token
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();

            // Validate email is provided
            if (email == null || email.isEmpty()) {
                log.error("Firebase authentication failed: no email provided in token");
                throw new BadRequestException("Firebase authentication requires an email");
            }

            String name = decodedToken.getName();
            Map<String, Object> claims = decodedToken.getClaims();

            log.info("Successfully verified Firebase ID token for user: {}", email);

            // Get provider ID (google.com, github.com, apple.com, microsoft.com)
            String provider = decodedToken.getIssuer();

            // Handle authentication with our system
            return handleFirebaseUser(uid, email, name, provider, claims);

        } catch (FirebaseAuthException e) {
            log.error("Firebase ID token verification failed: {}", e.getMessage());
            throw new UnauthorizedException("Invalid Firebase token: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Firebase authentication error: {}", e.getMessage());
            throw new BadRequestException("Invalid Firebase token format");
        }
    }

    /**
     * Handle Firebase user authentication - either sign in existing user or create new user
     */
    private UserResponse handleFirebaseUser(String firebaseUid, String email, String name, String provider, Map<String, Object> claims) {
        // Check if user with this email already exists in our system
        Optional<User> existingUser = authService.findUserByEmail(email);

        if (existingUser.isPresent()) {
            // User exists - link Firebase UID if not already linked
            User user = existingUser.get();

            // If user doesn't have Firebase UID set, update it
            if (user.getFirebaseUid() == null || !user.getFirebaseUid().equals(firebaseUid)) {
                authService.updateFirebaseUid(user.getId(), firebaseUid, provider);
            }

            // Return authenticated user
            return authService.authenticateFirebaseUser(user.getId());
        } else {
            // User doesn't exist - create new user with Firebase info
            String firstName = "";
            String lastName = "";

            // Parse name into first and last name
            if (name != null && !name.isEmpty()) {
                String[] nameParts = name.split(" ");
                firstName = nameParts[0];
                if (nameParts.length > 1) {
                    lastName = nameParts[nameParts.length - 1];
                }
            }

            // Generate username from email
            String username = generateUsernameFromEmail(email);

            // Get profile picture if available
            String profileImageUrl = null;
            if (claims.containsKey("picture")) {
                profileImageUrl = (String) claims.get("picture");
            }

            // Create the user
            return authService.createFirebaseUser(
                    firebaseUid,
                    email,
                    username,
                    firstName,
                    lastName,
                    profileImageUrl,
                    provider);
        }
    }

    /**
     * Generate a username from email
     */
    private String generateUsernameFromEmail(String email) {
        if (email == null || email.isEmpty() || !email.contains("@")) {
            return "user" + System.currentTimeMillis(); // Fallback username
        }

        String baseName = email.split("@")[0];
        // Remove special characters
        baseName = baseName.replaceAll("[^a-zA-Z0-9]", "");

        // Ensure username is not empty
        if (baseName.isEmpty()) {
            baseName = "user" + System.currentTimeMillis();
        }

        return baseName;
    }
}