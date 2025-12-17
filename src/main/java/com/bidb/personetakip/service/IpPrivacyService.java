package com.bidb.personetakip.service;

import com.bidb.personetakip.exception.IpPrivacyConfigurationException;

/**
 * Service interface for IP privacy and anonymization operations.
 * Handles IP address masking, anonymization, and privacy-compliant display.
 * Requirements: 5.2, 5.3, 5.5, 6.5
 */
public interface IpPrivacyService {
    
    /**
     * Displays IP address according to configured privacy settings.
     * May mask or anonymize the IP address based on privacy configuration.
     * 
     * @param ipAddress Original IP address
     * @param respectPrivacySettings Whether to apply privacy settings
     * @return Privacy-compliant IP address display
     * Requirements: 5.2 - Respect user privacy settings if configured
     */
    String displayIpAddress(String ipAddress, boolean respectPrivacySettings);
    
    /**
     * Displays IP address with detailed error handling.
     * 
     * @param ipAddress Original IP address
     * @param respectPrivacySettings Whether to apply privacy settings
     * @param throwOnError Whether to throw exception on configuration errors
     * @return Privacy-compliant IP address display
     * @throws IpPrivacyConfigurationException if privacy configuration is invalid and throwOnError is true
     * Requirements: 5.2 - Respect user privacy settings if configured
     *               6.5 - IP privacy configuration error handling
     */
    String displayIpAddressWithErrorHandling(String ipAddress, boolean respectPrivacySettings, boolean throwOnError) throws IpPrivacyConfigurationException;
    
    /**
     * Anonymizes IP address by masking parts of it.
     * For IPv4: masks last octet (e.g., 192.168.1.xxx)
     * For IPv6: masks last 64 bits (e.g., 2001:db8::xxxx:xxxx:xxxx:xxxx)
     * 
     * @param ipAddress IP address to anonymize
     * @return Anonymized IP address
     * Requirements: 5.5 - Provide option to anonymize or mask IP addresses
     */
    String anonymizeIpAddress(String ipAddress);
    
    /**
     * Logs IP address access for audit purposes.
     * Records who accessed IP address information and when.
     * 
     * @param ipAddress IP address that was accessed
     * @param userId User ID who accessed the IP (null if system access)
     * @param adminUserId Admin user ID who performed the access (null if not admin action)
     * @param action Type of access (VIEW, EXPORT, FILTER, etc.)
     * Requirements: 5.3 - Log IP address access and modifications for audit purposes
     */
    void logIpAddressAccess(String ipAddress, Long userId, Long adminUserId, String action);
    
    /**
     * Logs IP address modification for audit purposes.
     * Records changes to IP address assignments or data.
     * 
     * @param oldIpAddress Previous IP address value (null if new assignment)
     * @param newIpAddress New IP address value (null if removal)
     * @param userId User ID whose IP was modified
     * @param adminUserId Admin user ID who performed the modification
     * @param action Type of modification (ASSIGN, REMOVE, UPDATE)
     * Requirements: 5.3 - Log IP address access and modifications for audit purposes
     */
    void logIpAddressModification(String oldIpAddress, String newIpAddress, Long userId, Long adminUserId, String action);
    
    /**
     * Checks if privacy settings are enabled for IP address display.
     * 
     * @return true if privacy settings should be applied, false otherwise
     * Requirements: 5.2 - Respect user privacy settings if configured
     */
    boolean isPrivacyModeEnabled();
    
    /**
     * Gets the configured anonymization level.
     * 
     * @return Anonymization level (NONE, PARTIAL, FULL)
     * Requirements: 5.5 - Provide option to anonymize or mask IP addresses
     */
    AnonymizationLevel getAnonymizationLevel();
    
    /**
     * Enum representing different levels of IP anonymization
     */
    enum AnonymizationLevel {
        NONE,       // No anonymization - show full IP
        PARTIAL,    // Partial anonymization - mask some parts
        FULL        // Full anonymization - show only general location info
    }
}