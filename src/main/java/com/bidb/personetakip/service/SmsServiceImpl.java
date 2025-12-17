package com.bidb.personetakip.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of SMS service with VatanSMS API integration.
 * Includes retry and circuit breaker patterns for resilience.
 * 
 * VatanSMS API: https://api.vatansms.net/api/v1/otp
 */
@Service
public class SmsServiceImpl implements SmsService {
    
    private static final Logger logger = LoggerFactory.getLogger(SmsServiceImpl.class);
    private static final SecureRandom random = new SecureRandom();
    
    @Value("${sms.gateway.url:https://api.vatansms.net/api/v1/otp}")
    private String smsGatewayUrl;
    
    @Value("${sms.gateway.api-id:29d463733f56db81be9eb355}")
    private String apiId;
    
    @Value("${sms.gateway.api-key:79259ea325e14e8603ab7cf7}")
    private String apiKey;
    
    @Value("${sms.gateway.sender:G.ANTEP UNI}")
    private String senderId;
    
    @Value("${otp.length:6}")
    private int otpLength;
    
    private RestTemplate restTemplate;
    
    public SmsServiceImpl() {
        this(new RestTemplate());
    }

    public SmsServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Formats phone number for VatanSMS API.
     * VatanSMS expects Turkish mobile numbers without leading 0 and exactly 10 digits
     * Example: 05551234567 -> 5551234567
     * 
     * @param phoneNumber Raw phone number
     * @return Formatted phone number (10 digits, no leading 0)
     */
    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return phoneNumber;
        }
        
        // Remove all non-digit characters
        String cleaned = phoneNumber.replaceAll("\\D", "");
        
        // Handle different Turkish phone number formats
        if (cleaned.startsWith("90") && cleaned.length() == 12) {
            // International format: 905xxxxxxxxx -> 5xxxxxxxxx
            cleaned = cleaned.substring(2);
        } else if (cleaned.startsWith("0") && cleaned.length() == 11) {
            // Turkish format with leading 0: 05xxxxxxxxx -> 5xxxxxxxxx
            cleaned = cleaned.substring(1);
        } else if (cleaned.startsWith("5") && cleaned.length() == 10) {
            // Already correct format: 5xxxxxxxxx
            // No change needed
        } else {
            logger.warn("Unexpected phone number format: {}. Using as-is after cleaning.", phoneNumber);
        }
        
        // Validate final format
        if (!cleaned.matches("^5\\d{9}$")) {
            logger.warn("Phone number {} does not match expected Turkish mobile format (5xxxxxxxxx)", cleaned);
        }
        
        logger.debug("Formatted phone number: {} -> {}", phoneNumber, cleaned);
        return cleaned;
    }
    
    /**
     * Sends SMS with retry and circuit breaker protection using VatanSMS API.
     * Retries up to 3 times with exponential backoff.
     * Circuit breaker opens after 50% failure rate.
     */
    @Override
    @Retry(name = "smsGateway", fallbackMethod = "sendSmsFallback")
    @CircuitBreaker(name = "smsGateway", fallbackMethod = "sendSmsFallback")
    public void sendSms(String phoneNumber, String message) {
        // Format phone number (remove leading 0)
        String formattedPhone = formatPhoneNumber(phoneNumber);
        
        logger.info("Attempting to send SMS to: {} (formatted: {}) via VatanSMS", phoneNumber, formattedPhone);
        
        try {
            // VatanSMS API uses POST request with JSON body
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Build JSON request body
            Map<String, Object> body = new HashMap<>();
            body.put("api_id", apiId);
            body.put("api_key", apiKey);
            body.put("sender", senderId);
            body.put("message_type", "normal");
            body.put("message", message);
            body.put("phones", new String[]{formattedPhone});

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                smsGatewayUrl,
                HttpMethod.POST,
                request,
                String.class
            );

            logger.info("VatanSMS Response Code: {}, Body: {}", response.getStatusCode(), response.getBody());

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new SmsServiceException("SMS gateway returned non-success status: " + response.getStatusCode());
            }
            
            // Check if response contains error
            String responseBody = response.getBody();
            if (responseBody != null && (responseBody.contains("error") || responseBody.contains("hata"))) {
                logger.error("VatanSMS returned error: {}", responseBody);
                throw new SmsServiceException("SMS gateway returned error: " + responseBody);
            }
            
            logger.info("SMS sent successfully to: {} (formatted: {})", phoneNumber, formattedPhone);
        } catch (RestClientException e) {
            logger.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
            throw new SmsServiceException("Failed to send SMS", e);
        } catch (Exception e) {
            logger.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
            throw new SmsServiceException("Failed to send SMS", e);
        }
    }
    
    /**
     * Fallback method when SMS sending fails after all retries or circuit is open.
     */
    private void sendSmsFallback(String phoneNumber, String message, Exception e) {
        logger.error("SMS service unavailable. Failed to send SMS to {} after retries. Error: {}", 
                     phoneNumber, e.getMessage());
        throw new SmsServiceException("SMS service is currently unavailable. Please try again later.", e);
    }
    
    /**
     * Generates a 6-digit OTP code using SecureRandom.
     */
    @Override
    public String generateOtp() {
        int upperBound = (int) Math.pow(10, otpLength);
        int otp = random.nextInt(upperBound);
        
        // Format with leading zeros if necessary
        String otpString = String.format("%0" + otpLength + "d", otp);
        
        logger.debug("Generated OTP with length: {}", otpLength);
        return otpString;
    }
}
