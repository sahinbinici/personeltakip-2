package com.bidb.personetakip.exception;

/**
 * Exception thrown when IP address validation fails
 * Requirements: 1.3, 1.4
 */
public class IpValidationException extends ValidationException {
    
    private final String invalidIpAddress;
    private final String validationReason;
    
    public IpValidationException(String message) {
        super(message);
        this.invalidIpAddress = null;
        this.validationReason = null;
    }
    
    public IpValidationException(String message, String invalidIpAddress, String validationReason) {
        super(message);
        this.invalidIpAddress = invalidIpAddress;
        this.validationReason = validationReason;
    }
    
    public IpValidationException(String message, Throwable cause) {
        super(message, cause);
        this.invalidIpAddress = null;
        this.validationReason = null;
    }
    
    public String getInvalidIpAddress() {
        return invalidIpAddress;
    }
    
    public String getValidationReason() {
        return validationReason;
    }
}