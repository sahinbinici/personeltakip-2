package com.bidb.personetakip.model;

/**
 * Enumeration for excuse status values.
 * Requirements: 3.4, 3.5
 */
public enum ExcuseStatus {
    /**
     * Excuse has been submitted and is awaiting review.
     */
    PENDING("Beklemede"),
    
    /**
     * Excuse has been approved by an administrator.
     */
    APPROVED("Onaylandı"),
    
    /**
     * Excuse has been rejected by an administrator.
     */
    REJECTED("Reddedildi");
    
    private final String displayName;
    
    ExcuseStatus(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Gets the Turkish display name for the status.
     * 
     * @return Display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Converts string to ExcuseStatus enum.
     * 
     * @param status Status string
     * @return ExcuseStatus enum
     * @throws IllegalArgumentException if status is invalid
     */
    public static ExcuseStatus fromString(String status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        
        try {
            return ExcuseStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid excuse status: " + status);
        }
    }
    
    /**
     * Checks if the status represents a final state (approved or rejected).
     * 
     * @return true if final state
     */
    public boolean isFinal() {
        return this == APPROVED || this == REJECTED;
    }
    
    /**
     * Checks if the status is pending.
     * 
     * @return true if pending
     */
    public boolean isPending() {
        return this == PENDING;
    }
}