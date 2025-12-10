package com.bidb.personetakip.model;

/**
 * Enumeration of possible IP address actions for audit logging.
 * Used to track different types of IP-related operations in the system.
 */
public enum IpAddressAction {
    /**
     * IP address data was viewed/accessed
     */
    VIEW,
    
    /**
     * IP address was assigned to a user
     */
    ASSIGN,
    
    /**
     * IP address assignment was removed from a user
     */
    REMOVE,
    
    /**
     * IP address mismatch was detected
     */
    MISMATCH,
    
    /**
     * IP address data was accessed for reporting or compliance
     */
    ACCESS
}