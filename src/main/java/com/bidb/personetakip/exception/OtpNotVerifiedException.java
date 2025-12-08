package com.bidb.personetakip.exception;

/**
 * Exception thrown when attempting to complete registration without verified OTP
 */
public class OtpNotVerifiedException extends RuntimeException {
    
    public OtpNotVerifiedException(String message) {
        super(message);
    }
    
    public OtpNotVerifiedException(String message, Throwable cause) {
        super(message, cause);
    }
}
