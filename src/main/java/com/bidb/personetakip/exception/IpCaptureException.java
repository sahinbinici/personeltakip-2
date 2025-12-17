package com.bidb.personetakip.exception;

/**
 * Exception thrown when IP address capture fails during entry/exit operations
 * Requirements: 6.2
 */
public class IpCaptureException extends RuntimeException {
    
    private final String captureContext;
    private final boolean shouldBlockOperation;
    
    public IpCaptureException(String message) {
        super(message);
        this.captureContext = null;
        this.shouldBlockOperation = false;
    }
    
    public IpCaptureException(String message, String captureContext) {
        super(message);
        this.captureContext = captureContext;
        this.shouldBlockOperation = false;
    }
    
    public IpCaptureException(String message, String captureContext, boolean shouldBlockOperation) {
        super(message);
        this.captureContext = captureContext;
        this.shouldBlockOperation = shouldBlockOperation;
    }
    
    public IpCaptureException(String message, Throwable cause) {
        super(message, cause);
        this.captureContext = null;
        this.shouldBlockOperation = false;
    }
    
    public IpCaptureException(String message, String captureContext, Throwable cause) {
        super(message, cause);
        this.captureContext = captureContext;
        this.shouldBlockOperation = false;
    }
    
    public String getCaptureContext() {
        return captureContext;
    }
    
    public boolean shouldBlockOperation() {
        return shouldBlockOperation;
    }
}