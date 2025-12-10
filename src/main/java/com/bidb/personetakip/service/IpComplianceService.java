package com.bidb.personetakip.service;

import com.bidb.personetakip.model.EntryExitRecord;
import com.bidb.personetakip.model.User;

import java.util.List;

/**
 * Service interface for IP compliance operations.
 * Handles IP assignment comparison, mismatch detection, and compliance reporting.
 * Requirements: 3.4, 4.1, 4.2, 4.3, 4.4
 */
public interface IpComplianceService {
    
    /**
     * Checks if an IP address matches any of the user's assigned IP addresses.
     * 
     * @param actualIp The actual IP address used for entry/exit
     * @param assignedIpAddresses Comma-separated list of assigned IP addresses
     * @return true if IP matches any assigned IP, false otherwise
     * Requirements: 4.1 - IP mismatch highlighting
     *               4.2 - IP mismatch warning indicators
     */
    boolean isIpAddressMatch(String actualIp, String assignedIpAddresses);
    
    /**
     * Parses multiple IP addresses from a comma or semicolon separated string.
     * 
     * @param assignedIpAddresses Comma or semicolon separated IP addresses
     * @return List of individual IP addresses, empty list if null or empty input
     * Requirements: 3.4 - Multiple IP address support
     */
    List<String> parseAssignedIpAddresses(String assignedIpAddresses);
    
    /**
     * Determines if an entry/exit record has an IP mismatch.
     * 
     * @param record The entry/exit record
     * @param user The user who performed the entry/exit
     * @return true if there's an IP mismatch, false otherwise
     * Requirements: 4.1 - IP mismatch highlighting
     *               4.2 - IP mismatch warning indicators
     */
    boolean hasIpMismatch(EntryExitRecord record, User user);
    
    /**
     * Gets IP compliance status for a record.
     * 
     * @param record The entry/exit record
     * @param user The user who performed the entry/exit
     * @return IP compliance status (MATCH, MISMATCH, NO_ASSIGNMENT, UNKNOWN_IP)
     * Requirements: 4.3 - IP compliance statistics in reports
     */
    IpComplianceStatus getIpComplianceStatus(EntryExitRecord record, User user);
    
    /**
     * Validates that all IP addresses in the assignment string are valid.
     * 
     * @param assignedIpAddresses Comma or semicolon separated IP addresses
     * @return true if all IP addresses are valid, false otherwise
     * Requirements: 3.4 - Multiple IP address support
     */
    boolean validateAssignedIpAddresses(String assignedIpAddresses);
    
    /**
     * Enum representing IP compliance status
     */
    enum IpComplianceStatus {
        MATCH,          // IP matches assigned IP
        MISMATCH,       // IP doesn't match assigned IP
        NO_ASSIGNMENT,  // User has no assigned IP addresses
        UNKNOWN_IP      // Entry/exit IP is unknown
    }
}