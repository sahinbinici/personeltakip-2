package com.bidb.personetakip.service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for IP address operations.
 * Handles IP extraction, validation, and formatting.
 * Requirements: 1.1, 1.2, 1.3, 1.4
 */
public interface IpAddressService {
    
    /**
     * Extracts client IP address from HTTP request.
     * Handles proxy and load balancer scenarios to get real client IP.
     * 
     * @param request HTTP servlet request
     * @return Client IP address or default value if unknown
     * Requirements: 1.1 - Capture and store client IP address
     *               1.2 - Handle proxy and load balancer scenarios
     *               1.3 - Store default value for unknown IP
     */
    String extractClientIpAddress(HttpServletRequest request);
    
    /**
     * Validates IP address format for both IPv4 and IPv6.
     * 
     * @param ipAddress IP address to validate
     * @return true if valid IPv4 or IPv6 format, false otherwise
     * Requirements: 1.4 - Validate IP address format before storing
     */
    boolean isValidIpAddress(String ipAddress);
    
    /**
     * Returns the default value used for unknown IP addresses.
     * 
     * @return Default IP address value
     * Requirements: 1.3 - Store default value indicating unknown IP
     */
    String getUnknownIpDefault();
    
    /**
     * Formats IP address for display purposes.
     * Ensures consistent formatting for both IPv4 and IPv6.
     * 
     * @param ipAddress IP address to format
     * @return Formatted IP address string
     */
    String formatIpAddress(String ipAddress);
}