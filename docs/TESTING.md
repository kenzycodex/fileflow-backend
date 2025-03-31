# FileFlow Testing Guide

This document provides guidance on testing the FileFlow application, including the test structure, how to run tests, and best practices for contributing new tests.

## Table of Contents

- [Test Structure](#test-structure)
- [Running Tests](#running-tests)
- [Unit Tests](#unit-tests)
- [Controller Tests](#controller-tests)
- [Authentication Testing](#authentication-testing)
- [Security in Tests](#security-in-tests)
- [Environment Configuration in Tests](#environment-configuration-in-tests)
- [Troubleshooting Common Test Issues](#troubleshooting-common-test-issues)

## Test Structure

FileFlow tests are organized into several categories:

- **Unit Tests**: Test individual components in isolation
- **Controller Tests**: Test REST controllers with mock services
- **Configuration Tests**: Test configuration classes
- **Authentication Tests**: Test authentication flows including Firebase integration

### Test Packages

Tests are organized into the following package structure:

- `com.fileflow.controller`: Controller tests
- `com.fileflow.service`: Service unit tests
- `com.fileflow.repository`: Repository tests
- `com.fileflow.security`: Security component tests
- `com.fileflow.config`: Configuration tests
- `com.fileflow.test.helpers`: Test helper classes and utilities

## Running Tests

### Running All Tests

To run all tests:

```bash
./mvnw test
```

### Running Specific Test Classes

To run a specific test class:

```bash
./mvnw test -Dtest=FileControllerTest
```

### Running Tests with Specific Profile

```bash
./mvnw test -Dspring.profiles.active=test
```

## Unit Tests

Unit tests focus on testing individual components in isolation. FileFlow uses Mockito for mocking dependencies.

### Service Tests

Service tests verify business logic. Here's an example of a service test:

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SearchServiceImplTest {

    @Mock
    private FileRepository fileRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private SearchServiceImpl searchService;
    
    @BeforeEach
    public void setup() {
        // Setup common test data and mocks
    }
    
    @Test
    public void testSearchFiles() {
        // Test case for searching files
    }
}
```

### Key Unit Testing Principles

1. Mock all external dependencies
2. Test one specific behavior per test method
3. Use descriptive method names
4. Set up common test data in `@BeforeEach` methods
5. For void methods, use `doNothing().when()` pattern
6. Use `ReflectionTestUtils` to set private fields for testing

## Controller Tests

Controller tests verify API endpoints using MockMvc. These tests mock the service layer and test the HTTP layer.

### Test Setup

```java
@WebMvcTest(FileController.class)
@Import(TestSecurityConfig.class)
public class FileControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;
    
    @MockBean
    private SearchService searchService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }
    
    @Test
    @WithMockUser
    public void testGetFile() throws Exception {
        // Test case for GET endpoint
    }
}
```

### Testing Secure Endpoints

For endpoints that require authentication, use the `@WithMockUser` annotation:

```java
@Test
@WithMockUser
public void testCreateFolder() throws Exception {
    // Test method for authenticated endpoint
}
```

For admin-only endpoints:

```java
@Test
@WithMockUser(roles = {"ADMIN"})
public void testAdminEndpoint() throws Exception {
    // Test method for admin endpoint
}
```

## Authentication Testing

### Testing JWT Authentication

Test JWT authentication by mocking the `JwtTokenProvider`:

```java
@ExtendWith(MockitoExtension.class)
public class JwtTokenProviderTest {

    @Mock
    private JwtConfig jwtConfig;
    
    @Mock
    private JwtService jwtService;
    
    @InjectMocks
    private JwtTokenProvider tokenProvider;
    
    @BeforeEach
    public void setup() {
        when(jwtConfig.getSecret()).thenReturn("test-secret-key-long-enough-for-jwt-signature");
        when(jwtConfig.getExpiration()).thenReturn(3600000L);
    }
    
    @Test
    public void testGenerateToken() {
        // Test token generation
    }
}
```

### Testing Firebase Authentication

Testing Firebase authentication requires carefully mocking the Firebase SDK:

```java
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

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(firebaseAuthService, "firebaseEnabled", true);
    }

    @Test
    public void testAuthenticateWithFirebaseTokenVerificationFailed() {
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
                firebaseAuthService.authenticateWithFirebase("invalid-token");
            });
            
            // Verify the exception message
            assertTrue(exception.getMessage().contains("Invalid Firebase token"));
        }
    }
}
```

Important points for Firebase authentication testing:

1. Use `MockedStatic` to mock static Firebase methods
2. Mock the `FirebaseAuth` and `FirebaseToken` interfaces
3. For exceptions, create a mock of `FirebaseAuthException` rather than instantiating directly
4. Set private fields using `ReflectionTestUtils`
5. Test both success and failure scenarios

## Security in Tests

### TestSecurityConfig

FileFlow uses a special test security configuration to disable real authentication for tests:

```java
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.disable())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/**").permitAll());

        return http.build();
    }
}
```

Import this configuration in controller tests:

```java
@WebMvcTest(FolderController.class)
@Import(TestSecurityConfig.class)
public class FolderControllerTest {
    // Test methods
}
```

## Environment Configuration in Tests

When testing with environment variables:

1. Set environment variables programmatically:

```java
@BeforeEach
public void setup() {
    // Set environment variables for the test
    System.setProperty("FIREBASE_ENABLED", "false");
}

@AfterEach
public void cleanup() {
    // Clear environment variables after the test
    System.clearProperty("FIREBASE_ENABLED");
}
```

2. Use `ReflectionTestUtils` to set values directly:

```java
@BeforeEach
public void setup() {
    ReflectionTestUtils.setField(firebaseAuthService, "firebaseEnabled", false);
}
```

3. Create a test-specific `.env` file and place it in the test resources directory.

## Troubleshooting Common Test Issues

### MockBean Issues

If you're getting dependency injection errors, ensure all required dependencies are mocked:

```java
@MockBean
private SearchService searchService;
```

### Mocking Void Methods

For void methods, use the `doNothing()` pattern:

```java
// Incorrect
when(searchService.indexFile(any())).thenReturn(); // Compilation error

// Correct
doNothing().when(searchService).indexFile(any(File.class));
```

### Authentication in Tests

If your tests are failing with 403 Forbidden errors:

1. Add `@Import(TestSecurityConfig.class)` to your test class
2. Add `@WithMockUser` to test methods that require authentication
3. Configure MockMvc with security:

```java
mockMvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
```

### Unnecessary Stubbing Exceptions

If you get "UnnecessaryStubbingException", either:

1. Remove the unnecessary stubbing, or
2. Add `@MockitoSettings(strictness = Strictness.LENIENT)` to your test class

### Mocking SecurityContext

To mock the SecurityContext for service tests:

```java
@Mock
private SecurityContext securityContext;

@Mock
private Authentication authentication;

@BeforeEach
public void setup() {
    // Create user principal
    userPrincipal = UserPrincipal.builder()
            .id(1L)
            .username("testuser")
            .build();

    // Mock security context
    when(authentication.getPrincipal()).thenReturn(userPrincipal);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
}
```

### Firebase Authentication Testing Issues

Common issues when testing Firebase authentication:

1. **FirebaseAuthException constructor not found**:
    - Don't instantiate `FirebaseAuthException` directly, mock it instead:
   ```java
   Exception mockException = mock(FirebaseAuthException.class);
   when(mockException.getMessage()).thenReturn("Invalid token");
   ```

2. **Static methods not mocked properly**:
    - Use `MockedStatic` in a try-with-resources block:
   ```java
   try (MockedStatic<FirebaseAuth> firebaseAuthMock = mockStatic(FirebaseAuth.class)) {
       firebaseAuthMock.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
       // Test code here
   }
   ```

3. **Environment configuration issues**:
    - Use `ReflectionTestUtils` to set environment properties directly:
   ```java
   ReflectionTestUtils.setField(service, "firebaseEnabled", true);
   ```