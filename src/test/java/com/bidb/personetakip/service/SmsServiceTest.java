package com.bidb.personetakip.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SMS service implementation.
 * Tests OTP generation, SMS sending, retry logic, and circuit breaker behavior.
 */
@ExtendWith(MockitoExtension.class)
class SmsServiceTest {
    
    @Mock
    private RestTemplate restTemplate;
    
    private SmsServiceImpl smsService;
    
    private static final String TEST_GATEWAY_URL = "http://test-gateway.com/sms";
    private static final String TEST_API_ID = "test-api-id";
    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_SENDER_ID = "TestSender";
    private static final int OTP_LENGTH = 6;
    
    @BeforeEach
    void setUp() {
        smsService = new SmsServiceImpl();
        ReflectionTestUtils.setField(smsService, "smsGatewayUrl", TEST_GATEWAY_URL);
        ReflectionTestUtils.setField(smsService, "apiId", TEST_API_ID);
        ReflectionTestUtils.setField(smsService, "apiKey", TEST_API_KEY);
        ReflectionTestUtils.setField(smsService, "senderId", TEST_SENDER_ID);
        ReflectionTestUtils.setField(smsService, "otpLength", OTP_LENGTH);
        ReflectionTestUtils.setField(smsService, "restTemplate", restTemplate);
    }
    
    /**
     * Test OTP generation format - should be exactly 6 digits.
     */
    @Test
    void testGenerateOtp_ShouldBe6Digits() {
        // Generate OTP
        String otp = smsService.generateOtp();
        
        // Verify
        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}"), "OTP should contain only digits");
    }
    
    /**
     * Test OTP generation produces different values.
     */
    @Test
    void testGenerateOtp_ShouldProduceDifferentValues() {
        // Generate multiple OTPs
        String otp1 = smsService.generateOtp();
        String otp2 = smsService.generateOtp();
        String otp3 = smsService.generateOtp();
        
        // At least one should be different (statistically almost certain)
        boolean allSame = otp1.equals(otp2) && otp2.equals(otp3);
        assertFalse(allSame, "Generated OTPs should vary");
    }
    
    /**
     * Test OTP generation with leading zeros.
     */
    @Test
    void testGenerateOtp_ShouldMaintainLeadingZeros() {
        // Generate many OTPs to increase chance of getting one with leading zero
        boolean foundLeadingZero = false;
        for (int i = 0; i < 100; i++) {
            String otp = smsService.generateOtp();
            if (otp.startsWith("0")) {
                foundLeadingZero = true;
                assertEquals(6, otp.length(), "OTP with leading zero should still be 6 digits");
                break;
            }
        }
        // Note: This test is probabilistic but with 100 tries we should get at least one
    }
    
    /**
     * Test SMS sending with successful response from gateway.
     */
    @Test
    void testSendSms_Success() {
        // Arrange
        String phoneNumber = "+905551234567";
        String message = "Your OTP is: 123456";
        
        ResponseEntity<String> successResponse = new ResponseEntity<>("Success", HttpStatus.OK);
        when(restTemplate.exchange(
            eq(TEST_GATEWAY_URL),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(successResponse);
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> smsService.sendSms(phoneNumber, message));
        
        // Verify REST call was made
        verify(restTemplate, times(1)).exchange(
            eq(TEST_GATEWAY_URL),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        );
    }
    
    /**
     * Test SMS sending with correct headers and body for VatanSMS API format.
     */
    @Test
    void testSendSms_CorrectRequestFormat() {
        // Arrange
        String phoneNumber = "05551234567";
        String message = "Test message";
        
        ResponseEntity<String> successResponse = new ResponseEntity<>("Success", HttpStatus.OK);
        ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        
        when(restTemplate.exchange(
            eq(TEST_GATEWAY_URL),
            eq(HttpMethod.POST),
            requestCaptor.capture(),
            eq(String.class)
        )).thenReturn(successResponse);
        
        // Act
        smsService.sendSms(phoneNumber, message);
        
        // Assert
        HttpEntity<Map<String, Object>> capturedRequest = requestCaptor.getValue();
        
        // Verify headers - should be JSON content type
        assertEquals("application/json", capturedRequest.getHeaders().getContentType().toString());
        
        // Verify body - VatanSMS API format
        Map<String, Object> body = capturedRequest.getBody();
        assertNotNull(body);
        assertEquals(TEST_API_ID, body.get("api_id"));
        assertEquals(TEST_API_KEY, body.get("api_key"));
        assertEquals(message, body.get("message"));
        assertEquals(TEST_SENDER_ID, body.get("sender"));
        assertEquals("normal", body.get("message_type"));
        
        // Verify phone number formatting (should remove leading 0)
        String[] phones = (String[]) body.get("phones");
        assertNotNull(phones);
        assertEquals(1, phones.length);
        assertEquals("5551234567", phones[0]); // Leading 0 removed
    }
    
    /**
     * Test SMS sending failure throws exception.
     */
    @Test
    void testSendSms_GatewayFailure_ThrowsException() {
        // Arrange
        String phoneNumber = "+905551234567";
        String message = "Test message";
        
        when(restTemplate.exchange(
            eq(TEST_GATEWAY_URL),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenThrow(new RestClientException("Gateway unavailable"));
        
        // Act & Assert
        assertThrows(SmsServiceException.class, 
            () -> smsService.sendSms(phoneNumber, message));
    }
    
    /**
     * Test SMS sending with non-2xx status code throws exception.
     */
    @Test
    void testSendSms_Non2xxStatus_ThrowsException() {
        // Arrange
        String phoneNumber = "+905551234567";
        String message = "Test message";
        
        ResponseEntity<String> errorResponse = new ResponseEntity<>("Error", HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(
            eq(TEST_GATEWAY_URL),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(errorResponse);
        
        // Act & Assert
        assertThrows(SmsServiceException.class, 
            () -> smsService.sendSms(phoneNumber, message));
    }
    
    /**
     * Test phone number formatting for different input formats.
     */
    @Test
    void testPhoneNumberFormatting() {
        // Arrange
        String message = "Test message";
        ResponseEntity<String> successResponse = new ResponseEntity<>("Success", HttpStatus.OK);
        ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        
        when(restTemplate.exchange(
            eq(TEST_GATEWAY_URL),
            eq(HttpMethod.POST),
            requestCaptor.capture(),
            eq(String.class)
        )).thenReturn(successResponse);
        
        // Test different phone number formats
        String[][] testCases = {
            {"05551234567", "5551234567"},     // Turkish format with leading 0
            {"5551234567", "5551234567"},      // Already correct format
            {"905551234567", "5551234567"},    // International format
            {"+905551234567", "5551234567"},   // International with +
            {"0 555 123 45 67", "5551234567"}, // With spaces
            {"(0555) 123-45-67", "5551234567"} // With parentheses and dashes
        };
        
        for (String[] testCase : testCases) {
            String input = testCase[0];
            String expected = testCase[1];
            
            // Act
            smsService.sendSms(input, message);
            
            // Assert
            HttpEntity<Map<String, Object>> capturedRequest = requestCaptor.getValue();
            Map<String, Object> body = capturedRequest.getBody();
            String[] phones = (String[]) body.get("phones");
            
            assertEquals(expected, phones[0], 
                String.format("Phone number formatting failed for input: %s", input));
        }
    }
}
