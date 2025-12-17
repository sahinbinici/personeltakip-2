package com.bidb.personetakip.exception;

/**
 * Exception thrown when IP privacy configuration operations fail
 * Requirements: 5.2, 5.5, 6.5
 */
public class IpPrivacyConfigurationException extends RuntimeException {
    
    private final String configurationKey;
    private final String configurationValue;
    private final String configurationOperation;
    
    public IpPrivacyConfigurationException(String message) {
        super(message);
        this.configurationKey = null;
        this.configurationValue = null;
        this.configurationOperation = null;
    }
    
    public IpPrivacyConfigurationException(String message, String configurationKey, String configurationValue) {
        super(message);
        this.configurationKey = configurationKey;
        this.configurationValue = configurationValue;
        this.configurationOperation = null;
    }
    
    public IpPrivacyConfigurationException(String message, String configurationKey, String configurationValue, String configurationOperation) {
        super(message);
        this.configurationKey = configurationKey;
        this.configurationValue = configurationValue;
        this.configurationOperation = configurationOperation;
    }
    
    public IpPrivacyConfigurationException(String message, Throwable cause) {
        super(message, cause);
        this.configurationKey = null;
        this.configurationValue = null;
        this.configurationOperation = null;
    }
    
    public String getConfigurationKey() {
        return configurationKey;
    }
    
    public String getConfigurationValue() {
        return configurationValue;
    }
    
    public String getConfigurationOperation() {
        return configurationOperation;
    }
}