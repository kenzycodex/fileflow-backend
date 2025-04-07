package com.fileflow.service.config;

import com.fileflow.config.DotenvConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EnvPropertyServiceTest {

    @Mock
    private DotenvConfig dotenvConfig;

    @Mock
    private Environment environment;

    @InjectMocks
    private EnvPropertyService envPropertyService;

    @BeforeEach
    public void setup() {
        // Set up the system property for testing
        System.setProperty("TEST_ENV_SYSTEM_PROPERTY", "system_value");
    }

    @Test
    public void testGetProperty_fromSystemEnv() {
        // With the system property set, it should return from there first
        String result = envPropertyService.getProperty("TEST_ENV_SYSTEM_PROPERTY", "default");
        assertEquals("system_value", result);

        // Verify it didn't try other sources
        verify(dotenvConfig, never()).get(anyString(), anyString());
        verify(environment, never()).getProperty(anyString());
    }

    @Test
    public void testGetProperty_fromDotenv() {
        // Setup mock
        when(dotenvConfig.get("TEST_ENV_DOTENV", null)).thenReturn("dotenv_value");

        // Call service method
        String result = envPropertyService.getProperty("TEST_ENV_DOTENV", "default");

        // Verify result
        assertEquals("dotenv_value", result);
        verify(dotenvConfig).get("TEST_ENV_DOTENV", null);
        verify(environment, never()).getProperty("TEST_ENV_DOTENV");
    }

    @Test
    public void testGetProperty_fromSpringEnvironment() {
        // Setup mocks
        when(dotenvConfig.get("TEST_ENV_SPRING", null)).thenReturn(null);
        when(environment.getProperty("TEST_ENV_SPRING")).thenReturn("spring_value");

        // Call service method
        String result = envPropertyService.getProperty("TEST_ENV_SPRING", "default");

        // Verify result
        assertEquals("spring_value", result);
        verify(dotenvConfig).get("TEST_ENV_SPRING", null);
        verify(environment).getProperty("TEST_ENV_SPRING");
    }

    @Test
    public void testGetProperty_defaultValue() {
        // Setup mocks
        when(dotenvConfig.get("TEST_ENV_DEFAULT", null)).thenReturn(null);
        when(environment.getProperty("TEST_ENV_DEFAULT")).thenReturn(null);

        // Call service method
        String result = envPropertyService.getProperty("TEST_ENV_DEFAULT", "default_value");

        // Verify result
        assertEquals("default_value", result);
        verify(dotenvConfig).get("TEST_ENV_DEFAULT", null);
        verify(environment).getProperty("TEST_ENV_DEFAULT");
    }

    @Test
    public void testGetProperty_nullDefault() {
        // Setup mocks
        when(dotenvConfig.get("TEST_ENV_NULL", null)).thenReturn(null);
        when(environment.getProperty("TEST_ENV_NULL")).thenReturn(null);

        // Call service method
        String result = envPropertyService.getProperty("TEST_ENV_NULL");

        // Verify result
        assertNull(result);
        verify(dotenvConfig).get("TEST_ENV_NULL", null);
        verify(environment).getProperty("TEST_ENV_NULL");
    }

    @Test
    public void testGetBooleanProperty_true() {
        // Setup mocks
        when(dotenvConfig.get("TEST_BOOLEAN_TRUE", null)).thenReturn("true");

        // Call service method
        boolean result = envPropertyService.getBooleanProperty("TEST_BOOLEAN_TRUE", false);

        // Verify result
        assertTrue(result);
    }

    @Test
    public void testGetBooleanProperty_false() {
        // Setup mocks
        when(dotenvConfig.get("TEST_BOOLEAN_FALSE", null)).thenReturn("false");

        // Call service method
        boolean result = envPropertyService.getBooleanProperty("TEST_BOOLEAN_FALSE", true);

        // Verify result
        assertFalse(result);
    }

    @Test
    public void testGetBooleanProperty_invalidValue() {
        // Setup mocks
        when(dotenvConfig.get("TEST_BOOLEAN_INVALID", null)).thenReturn("not_a_boolean");

        // Call service method
        boolean result = envPropertyService.getBooleanProperty("TEST_BOOLEAN_INVALID", true);

        // Verify result - should use default for invalid values
        assertFalse(result);
    }

    @Test
    public void testGetIntProperty_validValue() {
        // Setup mocks
        when(dotenvConfig.get("TEST_INT_VALID", null)).thenReturn("42");

        // Call service method
        int result = envPropertyService.getIntProperty("TEST_INT_VALID", 0);

        // Verify result
        assertEquals(42, result);
    }

    @Test
    public void testGetIntProperty_invalidValue() {
        // Setup mocks
        when(dotenvConfig.get("TEST_INT_INVALID", null)).thenReturn("not_an_integer");

        // Call service method
        int result = envPropertyService.getIntProperty("TEST_INT_INVALID", 99);

        // Verify result - should use default for invalid values
        assertEquals(99, result);
    }

    @Test
    public void testGetLongProperty_validValue() {
        // Setup mocks
        when(dotenvConfig.get("TEST_LONG_VALID", null)).thenReturn("9223372036854775807");

        // Call service method
        long result = envPropertyService.getLongProperty("TEST_LONG_VALID", 0);

        // Verify result
        assertEquals(Long.MAX_VALUE, result);
    }

    @Test
    public void testGetLongProperty_invalidValue() {
        // Setup mocks
        when(dotenvConfig.get("TEST_LONG_INVALID", null)).thenReturn("not_a_long");

        // Call service method
        long result = envPropertyService.getLongProperty("TEST_LONG_INVALID", 999L);

        // Verify result - should use default for invalid values
        assertEquals(999L, result);
    }
}