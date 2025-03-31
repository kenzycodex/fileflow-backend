# Authentication Guide

This guide explains how to set up and configure the authentication system in FileFlow, including both traditional JWT-based authentication and social authentication via Firebase.

## Authentication Methods

FileFlow supports multiple authentication methods:

1. **Traditional Authentication** - Username/password with JWT tokens
2. **Social Authentication** - Login with Google, GitHub, Microsoft, and Apple via Firebase

## JWT Authentication

The default authentication mechanism uses JWT (JSON Web Tokens) for secure, stateless authentication.

### How It Works

1. User provides username/email and password
2. Server validates credentials and issues:
    - Access token (short-lived)
    - Refresh token (long-lived)
3. Access token is used for API authorization
4. Refresh token can be used to obtain a new access token when needed

### Configuration

JWT authentication is configured through these environment variables:

```properties
# JWT Settings
JWT_SECRET=your_secure_secret_key
JWT_EXPIRATION=86400000        # Access token validity in ms (24h)
JWT_REFRESH_EXPIRATION=604800000   # Refresh token validity in ms (7 days)
```

## Firebase Social Authentication

FileFlow integrates with Firebase Authentication to enable social login options.

### Available Providers

- Google
- GitHub
- Microsoft
- Apple

### Setup Process

#### 1. Create a Firebase Project

1. Go to the [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or select an existing one
3. Navigate to the Authentication section
4. Enable the desired authentication providers

#### 2. Configure Authentication Providers

For each provider you want to enable:

##### Google
- Usually works with minimal configuration in Firebase

##### GitHub
1. Register a new OAuth application in [GitHub Developer Settings](https://github.com/settings/developers)
2. Set callback URL to: `https://your-project-id.firebaseapp.com/__/auth/handler`
3. Copy Client ID and Client Secret to Firebase console

##### Microsoft
1. Register an application in [Microsoft Azure Portal](https://portal.azure.com/#blade/Microsoft_AAD_RegisteredApps/ApplicationsListBlade)
2. Set redirect URI to: `https://your-project-id.firebaseapp.com/__/auth/handler`
3. Copy Application ID and Secret to Firebase console

##### Apple
1. Set up Sign in with Apple in your [Apple Developer Account](https://developer.apple.com/)
2. Configure services for your App ID
3. Follow Firebase documentation to complete the setup

#### 3. Generate a Firebase Admin SDK Config

1. In Firebase Console, go to Project Settings > Service accounts
2. Select "Firebase Admin SDK"
3. Click "Generate new private key"
4. Save the downloaded JSON file securely

#### 4. Configure FileFlow to Use Firebase

1. Place the Firebase service account JSON file:
    - For development: `src/main/resources/firebase-service-account.json`
    - For production: `/app/config/firebase-service-account.json`

2. Update your `.env` file:
   ```
   FIREBASE_ENABLED=true
   FIREBASE_CONFIG_FILE=/path/to/firebase-service-account.json
   ```

3. Ensure Firebase dependencies are in your `pom.xml`:
   ```xml
   <dependency>
       <groupId>com.google.firebase</groupId>
       <artifactId>firebase-admin</artifactId>
       <version>9.2.0</version>
   </dependency>
   ```

### Client Integration

For frontend applications, use the Firebase JavaScript SDK:

```javascript
// Initialize Firebase
import { initializeApp } from "firebase/app";
import { getAuth, GoogleAuthProvider, signInWithPopup } from "firebase/auth";

const firebaseConfig = {
  apiKey: "YOUR_API_KEY",
  authDomain: "YOUR_PROJECT_ID.firebaseapp.com",
  projectId: "YOUR_PROJECT_ID",
  // ...other config
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);

// Sign in with Google example
async function signInWithGoogle() {
  const provider = new GoogleAuthProvider();
  try {
    const result = await signInWithPopup(auth, provider);
    const idToken = await result.user.getIdToken();
    
    // Send token to your backend
    const response = await fetch('/api/v1/auth/social/firebase', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ idToken }),
    });
    
    const data = await response.json();
    
    // Store JWT tokens from your backend
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    
    return data;
  } catch (error) {
    console.error('Error signing in with Google:', error);
    throw error;
  }
}
```

## Authentication Flow

### Traditional Authentication

1. User calls `/api/v1/auth/signin` with credentials
2. Server validates and returns JWT tokens
3. Client includes access token in Authorization header for subsequent requests
4. When token expires, client requests new one via `/api/v1/auth/refresh`

### Social Authentication

1. User authenticates with provider via Firebase in the browser
2. Client obtains Firebase ID token
3. Client sends token to backend: `/api/v1/auth/social/firebase`
4. Backend verifies token with Firebase Admin SDK
5. Backend creates/updates user account and issues JWT tokens
6. Authentication proceeds same as traditional flow

## Logout

FileFlow implements secure logout that invalidates tokens on both client and server side:

```javascript
async function logout() {
  try {
    // Get refresh token
    const refreshToken = localStorage.getItem('refreshToken');
    
    // Call backend logout endpoint
    await fetch('/api/v1/auth/logout', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ refreshToken }),
    });
    
    // Clear local storage
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    
    // If using Firebase, also sign out there
    const auth = getAuth();
    await auth.signOut();
    
  } catch (error) {
    console.error('Error during logout:', error);
  }
}
```

## Security Considerations

### Token Storage

- Never store tokens in cookies without proper security measures
- For web applications, use localStorage or sessionStorage
- Consider token encryption for added security

### CSRF Protection

FileFlow includes CSRF protection for sensitive operations. Ensure your frontend handles CSRF tokens correctly when required.

### Password Policies

Password requirements are configurable:

```properties
PASSWORD_MIN_LENGTH=8
PASSWORD_REQUIRE_DIGITS=true
PASSWORD_REQUIRE_LOWERCASE=true
PASSWORD_REQUIRE_UPPERCASE=true
PASSWORD_REQUIRE_SPECIAL=true
```

## Troubleshooting

### Firebase Authentication Issues

1. **Invalid Firebase token**:
    - Check token expiration (Firebase tokens expire after 1 hour)
    - Verify clock sync between systems

2. **CORS issues with Firebase authentication**:
    - Add your domains to the Firebase Console > Authentication > Settings > Authorized domains

3. **Missing user data after social login**:
    - Some providers may not share all profile information
    - Request additional scopes when configuring the providers

4. **Firebase admin SDK initialization errors**:
    - Verify service account file path and permissions
    - Check file format and completeness

### JWT Authentication Issues

1. **Token expired errors**:
    - Implement proper token refresh mechanism
    - Check system clocks are synchronized

2. **Invalid signature errors**:
    - Ensure JWT_SECRET is consistent across all server instances
    - Verify token hasn't been tampered with