package com.bidb.personetakip.model;

/**
 * Enum representing user roles in the system.
 * Currently supports NORMAL_USER, with ADMIN and SUPER_ADMIN prepared for future implementation.
 */
public enum UserRole {
    /**
     * Normal user (personnel) - can register, login, and view daily QR codes
     */
    NORMAL_USER,
    
    /**
     * Admin - management and reporting capabilities (future implementation)
     */
    ADMIN,
    
    /**
     * Super Admin - full system configuration rights (future implementation)
     */
    SUPER_ADMIN
}
