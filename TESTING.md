# FileFlow Testing Guide

This document provides guidance on testing the FileFlow application, including the test structure, how to run tests, and best practices for contributing new tests.

## Table of Contents

- [Test Structure](#test-structure)
- [Running Tests](#running-tests)
- [Unit Tests](#unit-tests)
- [Controller Tests](#controller-tests)
- [Security in Tests](#security-in-tests)
- [Troubleshooting Common Test Issues](#troubleshooting-common-test-issues)

## Test Structure

FileFlow tests are organized into several categories:

- **Unit Tests**: Test individual components in isolation
- **Controller Tests**: Test REST controllers with mock services
- **Configuration Tests**: Test configuration classes

### Test Packages

Tests are organized into the following package structure:

- `com.fileflow.controller`: Controller tests
- `com.fileflow.service`: Service unit tests
- `com.fileflow.repository`: Repository tests
- `com.fileflow.security`: Security component tests
- `com.fileflow.config`: Configuration tests

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