package com.bidb.personetakip.exception;

/**
 * Exception thrown when IP assignment operations fail
 * Requirements: 3.1, 3.2, 3.5
 */
public class IpAssignmentException extends RuntimeException {
    
    private final String assignmentOperation;
    private final String userId;
    private final String invalidAssignment;
    
    public IpAssignmentException(String message) {
        super(message);
        this.assignmentOperation = null;
        this.userId = null;
        this.invalidAssignment = null;
    }
    
    public IpAssignmentException(String message, String assignmentOperation, String userId) {
        super(message);
        this.assignmentOperation = assignmentOperation;
        this.userId = userId;
        this.invalidAssignment = null;
    }
    
    public IpAssignmentException(String message, String assignmentOperation, String userId, String invalidAssignment) {
        super(message);
        this.assignmentOperation = assignmentOperation;
        this.userId = userId;
        this.invalidAssignment = invalidAssignment;
    }
    
    public IpAssignmentException(String message, Throwable cause) {
        super(message, cause);
        this.assignmentOperation = null;
        this.userId = null;
        this.invalidAssignment = null;
    }
    
    public String getAssignmentOperation() {
        return assignmentOperation;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getInvalidAssignment() {
        return invalidAssignment;
    }
}