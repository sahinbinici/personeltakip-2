package com.bidb.personetakip.service;

/**
 * Service interface for SMS operations including OTP generation and SMS sending.
 */
public interface SmsService {
    
    /**
     * Sends an SMS message to the specified phone number via external gateway.
     *
     * @param phoneNumber The recipient's phone number
     * @param message The message content to send
     * @throws SmsServiceException if SMS sending fails
     */
    void sendSms(String phoneNumber, String message);
    
    /**
     * Generates a 6-digit OTP code.
     *
     * @return A 6-digit OTP code as a String
     */
    String generateOtp();
}
