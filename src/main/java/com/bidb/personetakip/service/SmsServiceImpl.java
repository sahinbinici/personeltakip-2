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
 * Implementation of SMS service with external gateway integration.
 * Includes retry and circuit breaker patterns for resilience.
 */
@Service
public class SmsServiceImpl implements SmsService {
    
    private static final Logger logger = LoggerFactory.getLogger(SmsServiceImpl.class);
    private static final SecureRandom random = new SecureRandom();
    
    @Value("${sms.gateway.url}")
    private String smsGatewayUrl;
    
    @Value("${sms.gateway.api-key}")
    private String apiKey;
    
    @Value("${sms.gateway.sender}")
    private String senderId;
    
    @Value("${otp.length}")
    private int otpLength;
    
    private final RestTemplate restTemplate;
    
    public SmsServiceImpl() {
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Sends SMS with retry and circuit breaker protection.
     * Retries up to 3 times with exponential backoff.
     * Circuit breaker opens after 50% failure rate.
     */
    @Override
    @Retry(name = "smsGateway", fallbackMethod = "sendSmsFallback")
    @CircuitBreaker(name = "smsGateway", fallbackMethod = "sendSmsFallback")
    public void sendSms(String phoneNumber, String message) {
        logger.debug("Attempting to send SMS to: {}", phoneNumber);
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", apiKey);
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("to", phoneNumber);
            requestBody.put("message", message);
            requestBody.put("sender", senderId);
            
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                smsGatewayUrl,
                HttpMethod.POST,
                request,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("SMS sent successfully to: {}", phoneNumber);
            } else {
                throw new SmsServiceException("SMS gateway returned non-success status: " + response.getStatusCode());
            }
            
        } catch (RestClientException e) {
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
