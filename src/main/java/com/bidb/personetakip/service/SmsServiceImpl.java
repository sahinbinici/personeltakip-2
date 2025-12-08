package com.bidb.personetakip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
    
    private final ObjectMapper objectMapper;
    
    public SmsServiceImpl() {
        this.objectMapper = new ObjectMapper();
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
        logger.debug("Attempting to send SMS to: {} via VatanSMS", phoneNumber);
        
        try {
            URL url = new URL(smsGatewayUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            // Prepare request body according to VatanSMS API format
            Map<String, Object> params = new HashMap<>();
            params.put("api_id", apiId);
            params.put("api_key", apiKey);
            params.put("sender", senderId);
            params.put("message_type", "normal");
            params.put("message", message);
            params.put("phones", new String[]{phoneNumber});
            
            String jsonInputString = objectMapper.writeValueAsString(params);
            
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            int responseCode = conn.getResponseCode();
            String responseMessage = conn.getResponseMessage();
            
            logger.info("VatanSMS Response Code: {}, Message: {}", responseCode, responseMessage);
            
            if (responseCode >= 200 && responseCode < 300) {
                logger.info("SMS sent successfully to: {}", phoneNumber);
            } else {
                throw new SmsServiceException("SMS gateway returned non-success status: " + responseCode + " - " + responseMessage);
            }
            
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
