package com.bidb.personetakip.service;

import com.bidb.personetakip.exception.IpAssignmentException;
import com.bidb.personetakip.exception.IpCaptureException;
import com.bidb.personetakip.exception.IpPrivacyConfigurationException;
import com.bidb.personetakip.exception.IpValidationException;
import com.bidb.personetakip.util.IpErrorMessageUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for IP error handling functionality.
 * Requirements: 1.3, 6.2
 */
@SpringBootTest
@ActiveProfiles("test")
class IpErrorHandlingTest {
    
    @Test
    void testIpValidationExceptionCreation() {
        // Test basic exception creation
        IpValidationException ex1 = new IpValidationException("Test message");
        assertEquals("Test message", ex1.getMessage());
        assertNull(ex1.getInvalidIpAddress());
        assertNull(ex1.getValidationReason());
        
        // Test detailed exception creation
        IpValidationException ex2 = new IpValidationException("Invalid IP", "192.168.1", "incomplete address");
        assertEquals("Invalid IP", ex2.getMessage());
        assertEquals("192.168.1", ex2.getInvalidIpAddress());
        assertEquals("incomplete address", ex2.getValidationReason());
    }
    
    @Test
    void testIpCaptureExceptionCreation() {
        // Test basic exception creation
        IpCaptureException ex1 = new IpCaptureException("Capture failed");
        assertEquals("Capture failed", ex1.getMessage());
        assertNull(ex1.getCaptureContext());
        assertFalse(ex1.shouldBlockOperation());
        
        // Test detailed exception creation
        IpCaptureException ex2 = new IpCaptureException("Timeout", "timeout_exceeded", true);
        assertEquals("Timeout", ex2.getMessage());
        assertEquals("timeout_exceeded", ex2.getCaptureContext());
        assertTrue(ex2.shouldBlockOperation());
    }
    
    @Test
    void testIpAssignmentExceptionCreation() {
        // Test basic exception creation
        IpAssignmentException ex1 = new IpAssignmentException("Assignment failed");
        assertEquals("Assignment failed", ex1.getMessage());
        assertNull(ex1.getAssignmentOperation());
        assertNull(ex1.getUserId());
        assertNull(ex1.getInvalidAssignment());
        
        // Test detailed exception creation
        IpAssignmentException ex2 = new IpAssignmentException("Invalid format", "validate", "123", "192.168.1");
        assertEquals("Invalid format", ex2.getMessage());
        assertEquals("validate", ex2.getAssignmentOperation());
        assertEquals("123", ex2.getUserId());
        assertEquals("192.168.1", ex2.getInvalidAssignment());
    }
    
    @Test
    void testIpPrivacyConfigurationExceptionCreation() {
        // Test basic exception creation
        IpPrivacyConfigurationException ex1 = new IpPrivacyConfigurationException("Config error");
        assertEquals("Config error", ex1.getMessage());
        assertNull(ex1.getConfigurationKey());
        assertNull(ex1.getConfigurationValue());
        assertNull(ex1.getConfigurationOperation());
        
        // Test detailed exception creation
        IpPrivacyConfigurationException ex2 = new IpPrivacyConfigurationException(
            "Invalid method", "anonymization.method", "INVALID", "validate");
        assertEquals("Invalid method", ex2.getMessage());
        assertEquals("anonymization.method", ex2.getConfigurationKey());
        assertEquals("INVALID", ex2.getConfigurationValue());
        assertEquals("validate", ex2.getConfigurationOperation());
    }
    
    @Test
    void testIpErrorMessageUtilValidation() {
        // Test IP validation error messages
        IpValidationException validationEx = new IpValidationException(
            "Invalid IP", "192.168.1", "incomplete address");
        String message = IpErrorMessageUtil.getIpValidationErrorMessage(validationEx);
        assertTrue(message.contains("192.168.1"));
        assertTrue(message.contains("geçersiz"));
        
        // Test IP capture error messages
        IpCaptureException captureEx = new IpCaptureException("Timeout", "timeout_exceeded");
        String captureMessage = IpErrorMessageUtil.getIpCaptureErrorMessage(captureEx);
        assertTrue(captureMessage.contains("zaman aşımı"));
        
        // Test IP assignment error messages
        IpAssignmentException assignmentEx = new IpAssignmentException(
            "Invalid format", "validate", "123", "192.168.1");
        String assignmentMessage = IpErrorMessageUtil.getIpAssignmentErrorMessage(assignmentEx);
        assertTrue(assignmentMessage.contains("Geçersiz IP"));
        
        // Test IP privacy configuration error messages
        IpPrivacyConfigurationException privacyEx = new IpPrivacyConfigurationException(
            "Invalid method", "anonymization.method", "INVALID", "validate");
        String privacyMessage = IpErrorMessageUtil.getIpPrivacyConfigurationErrorMessage(privacyEx);
        assertTrue(privacyMessage.contains("anonimleştirme"));
    }
    
    @Test
    void testGenericErrorMessages() {
        assertEquals("IP adresi yakalama işlemi başarısız oldu.", 
            IpErrorMessageUtil.getGenericIpErrorMessage("capture"));
        assertEquals("IP adresi doğrulama işlemi başarısız oldu.", 
            IpErrorMessageUtil.getGenericIpErrorMessage("validate"));
        assertEquals("IP adresi atama işlemi başarısız oldu.", 
            IpErrorMessageUtil.getGenericIpErrorMessage("assign"));
        assertEquals("IP işlemi başarısız oldu.", 
            IpErrorMessageUtil.getGenericIpErrorMessage("unknown"));
    }
}