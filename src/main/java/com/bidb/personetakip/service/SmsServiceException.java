package com.bidb.personetakip.service;

/**
 * Exception thrown when SMS service operations fail.
 */
public class SmsServiceException extends RuntimeException {
    
    public SmsServiceException(String message) {
        super(message);
    }
    
    public SmsServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
