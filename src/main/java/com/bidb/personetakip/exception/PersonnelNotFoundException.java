package com.bidb.personetakip.exception;

/**
 * Exception thrown when personnel is not found in external database
 */
public class PersonnelNotFoundException extends RuntimeException {
    
    public PersonnelNotFoundException(String message) {
        super(message);
    }
    
    public PersonnelNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
