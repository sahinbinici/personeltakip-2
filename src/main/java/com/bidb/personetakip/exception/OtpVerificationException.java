package com.bidb.personetakip.exception;

/**
 * Exception thrown when OTP verification fails
 */
public class OtpVerificationException extends RuntimeException {
    
    public OtpVerificationException(String message) {
        super(message);
    }
    
    public OtpVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
