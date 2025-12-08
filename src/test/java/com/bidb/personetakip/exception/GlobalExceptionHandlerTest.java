package com.bidb.personetakip.exception;

import com.bidb.personetakip.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GlobalExceptionHandler
 * Tests each exception type returns correct HTTP status, error response format, and error message content
 * Requirements: 1.3, 4.4, 7.3, 8.3
 */
class GlobalExceptionHandlerTest {
    
    private GlobalExceptionHandler exceptionHandler;
    
    @Mock
    private HttpServletRequest request;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        exceptionHandler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/test");
    }
    
    /**
     * Test ValidationException returns HTTP 400 status
     */
    @Test
    void testValidationExceptionReturnsHttp400() {
        // Given
        ValidationException exception = new ValidationException("Invalid input");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(exception, request);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getStatusCode().value());
    }
    
    /**
     * Test ValidationException returns correct error response format
     */
    @Test
    void testValidationExceptionErrorResponseFormat() {
        // Given
        ValidationException exception = new ValidationException("Invalid input");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(exception, request);
        ErrorResponse errorResponse = response.getBody();
        
        // Then
        assertNotNull(errorResponse);
        assertNotNull(errorResponse.timestamp());
        assertEquals(400, errorResponse.status());
        assertEquals("Bad Request", errorResponse.error());
        assertEquals("Invalid input", errorResponse.message());
        assertEquals("/api/test", errorResponse.path());
    }
    
    /**
     * Test ValidationException returns correct error message content
     */
    @Test
    void testValidationExceptionErrorMessage() {
        // Given
        String expectedMessage = "QR code has already been used twice";
        ValidationException exception = new ValidationException(expectedMessage);
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(exception, request);
        
        // Then
        assertNotNull(response.getBody());
        assertEquals(expectedMessage, response.getBody().message());
    }
    
    /**
     * Test AuthenticationException returns HTTP 401 status
     */
    @Test
    void testAuthenticationExceptionReturnsHttp401() {
        // Given
        AuthenticationException exception = new AuthenticationException("Invalid credentials");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAuthenticationException(exception, request);
        
        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getStatusCode().value());
    }
    
    /**
     * Test AuthenticationException returns correct error response format
     */
    @Test
    void testAuthenticationExceptionErrorResponseFormat() {
        // Given
        AuthenticationException exception = new AuthenticationException("Invalid credentials");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAuthenticationException(exception, request);
        ErrorResponse errorResponse = response.getBody();
        
        // Then
        assertNotNull(errorResponse);
        assertNotNull(errorResponse.timestamp());
        assertEquals(401, errorResponse.status());
        assertEquals("Unauthorized", errorResponse.error());
        assertEquals("Invalid credentials", errorResponse.message());
        assertEquals("/api/test", errorResponse.path());
    }
    
    /**
     * Test AuthenticationException returns correct error message content
     */
    @Test
    void testAuthenticationExceptionErrorMessage() {
        // Given
        String expectedMessage = "Invalid TC ID or password";
        AuthenticationException exception = new AuthenticationException(expectedMessage);
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAuthenticationException(exception, request);
        
        // Then
        assertNotNull(response.getBody());
        assertEquals(expectedMessage, response.getBody().message());
    }
    
    /**
     * Test ResourceNotFoundException returns HTTP 404 status
     */
    @Test
    void testResourceNotFoundExceptionReturnsHttp404() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("User not found");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFoundException(exception, request);
        
        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getStatusCode().value());
    }
    
    /**
     * Test ResourceNotFoundException returns correct error response format
     */
    @Test
    void testResourceNotFoundExceptionErrorResponseFormat() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("User not found");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFoundException(exception, request);
        ErrorResponse errorResponse = response.getBody();
        
        // Then
        assertNotNull(errorResponse);
        assertNotNull(errorResponse.timestamp());
        assertEquals(404, errorResponse.status());
        assertEquals("Not Found", errorResponse.error());
        assertEquals("User not found", errorResponse.message());
        assertEquals("/api/test", errorResponse.path());
    }
    
    /**
     * Test ResourceNotFoundException returns correct error message content
     */
    @Test
    void testResourceNotFoundExceptionErrorMessage() {
        // Given
        String expectedMessage = "QR code not found";
        ResourceNotFoundException exception = new ResourceNotFoundException(expectedMessage);
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFoundException(exception, request);
        
        // Then
        assertNotNull(response.getBody());
        assertEquals(expectedMessage, response.getBody().message());
    }
    
    /**
     * Test ExternalServiceException returns HTTP 503 status
     */
    @Test
    void testExternalServiceExceptionReturnsHttp503() {
        // Given
        ExternalServiceException exception = new ExternalServiceException("SMS gateway unavailable");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleExternalServiceException(exception, request);
        
        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(503, response.getStatusCode().value());
    }
    
    /**
     * Test ExternalServiceException returns correct error response format
     */
    @Test
    void testExternalServiceExceptionErrorResponseFormat() {
        // Given
        ExternalServiceException exception = new ExternalServiceException("SMS gateway unavailable");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleExternalServiceException(exception, request);
        ErrorResponse errorResponse = response.getBody();
        
        // Then
        assertNotNull(errorResponse);
        assertNotNull(errorResponse.timestamp());
        assertEquals(503, errorResponse.status());
        assertEquals("Service Unavailable", errorResponse.error());
        assertEquals("SMS gateway unavailable", errorResponse.message());
        assertEquals("/api/test", errorResponse.path());
    }
    
    /**
     * Test ExternalServiceException returns correct error message content
     */
    @Test
    void testExternalServiceExceptionErrorMessage() {
        // Given
        String expectedMessage = "External database connection failed";
        ExternalServiceException exception = new ExternalServiceException(expectedMessage);
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleExternalServiceException(exception, request);
        
        // Then
        assertNotNull(response.getBody());
        assertEquals(expectedMessage, response.getBody().message());
    }
    
    /**
     * Test general Exception returns HTTP 500 status
     */
    @Test
    void testGeneralExceptionReturnsHttp500() {
        // Given
        Exception exception = new RuntimeException("Unexpected error");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGeneralException(exception, request);
        
        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getStatusCode().value());
    }
    
    /**
     * Test general Exception returns correct error response format
     */
    @Test
    void testGeneralExceptionErrorResponseFormat() {
        // Given
        Exception exception = new RuntimeException("Unexpected error");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGeneralException(exception, request);
        ErrorResponse errorResponse = response.getBody();
        
        // Then
        assertNotNull(errorResponse);
        assertNotNull(errorResponse.timestamp());
        assertEquals(500, errorResponse.status());
        assertEquals("Internal Server Error", errorResponse.error());
        assertEquals("An unexpected error occurred. Please try again later.", errorResponse.message());
        assertEquals("/api/test", errorResponse.path());
    }
    
    /**
     * Test general Exception returns generic error message (not exposing internal details)
     */
    @Test
    void testGeneralExceptionErrorMessageIsGeneric() {
        // Given
        Exception exception = new RuntimeException("Internal database connection pool exhausted");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGeneralException(exception, request);
        
        // Then
        assertNotNull(response.getBody());
        // Should return generic message, not the actual exception message
        assertEquals("An unexpected error occurred. Please try again later.", response.getBody().message());
        assertNotEquals(exception.getMessage(), response.getBody().message());
    }
    
    /**
     * Test error response path matches request URI
     */
    @Test
    void testErrorResponsePathMatchesRequestUri() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/mobil/giris-cikis-kaydet");
        ValidationException exception = new ValidationException("Invalid GPS coordinates");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(exception, request);
        
        // Then
        assertNotNull(response.getBody());
        assertEquals("/api/mobil/giris-cikis-kaydet", response.getBody().path());
    }
    
    /**
     * Test error response timestamp is recent
     */
    @Test
    void testErrorResponseTimestampIsRecent() {
        // Given
        ValidationException exception = new ValidationException("Test error");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(exception, request);
        
        // Then
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().timestamp());
        // Timestamp should be within the last second
        assertTrue(response.getBody().timestamp().isAfter(
            java.time.LocalDateTime.now().minusSeconds(1)
        ));
    }
}
