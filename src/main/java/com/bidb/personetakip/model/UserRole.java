package com.bidb.personetakip.model;

/**
 * Enum representing user roles in the system.
 * Supports hierarchical admin roles with department-based permissions.
 */
public enum UserRole {
    /**
     * Normal user (personnel) - can register, login, and view daily QR codes
     */
    NORMAL_USER,
    
    /**
     * Department Admin - management capabilities for specific department only
     */
    DEPARTMENT_ADMIN,
    
    /**
     * Admin - management and reporting capabilities for all departments
     */
    ADMIN,
    
    /**
     * Super Admin - full system configuration rights
     */
    SUPER_ADMIN
}
