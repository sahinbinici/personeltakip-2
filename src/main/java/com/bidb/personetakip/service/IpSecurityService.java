package com.bidb.personetakip.service;

import com.bidb.personetakip.exception.IpValidationException;
import com.bidb.personetakip.model.IpAddressLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for IP address security operations.
 * Handles secure storage, input sanitization, access monitoring, and retention policies.
 * Requirements: 5.1, 5.3
 */
public interface IpSecurityService {
    
    /**
     * Sanitizes IP address input to prevent injection attacks and ensure data integrity.
     * Requirements: 5.1
     * 
     * @param ipAddress Raw IP address input
     * @return Sanitized IP address safe for storage
     * @throws IpValidationException if IP address cannot be sanitized
     */
    String sanitizeIpAddress(String ipAddress) throws IpValidationException;
    
    /**
     * Validates IP address against security constraints before storage.
     * Requirements: 5.1
     * 
     * @param ipAddress IP address to validate
     * @throws IpValidationException if IP address violates security constraints
     */
    void validateSecureStorage(String ipAddress) throws IpValidationException;
    
    /**
     * Monitors IP data access and logs security-relevant events.
     * Requirements: 5.3
     * 
     * @param ipAddress IP address being accessed
     * @param userId User ID whose IP data is accessed
     * @param adminUserId Admin user performing the access
     * @param accessType Type of access (VIEW, MODIFY, DELETE)
     * @param sourceIp IP address of the admin performing the access
     */
    void monitorIpDataAccess(String ipAddress, Long userId, Long adminUserId, String accessType, String sourceIp);
    
    /**
     * Enforces IP data retention policy by cleaning up old records.
     * Requirements: 5.3
     * 
     * @return Number of records cleaned up
     */
    int enforceRetentionPolicy();
    
    /**
     * Checks if IP address storage meets security requirements.
     * Requirements: 5.1
     * 
     * @param ipAddress IP address to check
     * @return true if storage is secure, false otherwise
     */
    boolean isSecureStorage(String ipAddress);
    
    /**
     * Gets security audit logs for IP address operations.
     * Requirements: 5.3
     * 
     * @param userId User ID to filter logs (null for all users)
     * @param startDate Start date for log filtering
     * @param endDate End date for log filtering
     * @return List of audit logs
     */
    List<IpAddressLog> getSecurityAuditLogs(Long userId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Detects suspicious IP access patterns.
     * Requirements: 5.3
     * 
     * @param userId User ID to analyze
     * @param timeWindowHours Time window in hours to analyze
     * @return true if suspicious patterns detected
     */
    boolean detectSuspiciousAccess(Long userId, int timeWindowHours);
    
    /**
     * Validates IP address input length and format for security.
     * Requirements: 5.1
     * 
     * @param ipAddress IP address to validate
     * @return true if input is safe, false otherwise
     */
    boolean isSecureInput(String ipAddress);
    
    /**
     * Encrypts sensitive IP address data for secure storage.
     * Requirements: 5.1
     * 
     * @param ipAddress IP address to encrypt
     * @return Encrypted IP address
     */
    String encryptIpAddress(String ipAddress);
    
    /**
     * Decrypts IP address data for authorized access.
     * Requirements: 5.1
     * 
     * @param encryptedIpAddress Encrypted IP address
     * @return Decrypted IP address
     */
    String decryptIpAddress(String encryptedIpAddress);
}