package com.bidb.personetakip.service;

import com.bidb.personetakip.exception.IpCaptureException;
import com.bidb.personetakip.exception.IpValidationException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for IP address operations.
 * Handles IP extraction, validation, and formatting.
 * Requirements: 1.1, 1.2, 1.3, 1.4, 6.2
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
     * Extracts client IP address with detailed error handling.
     * 
     * @param request HTTP servlet request
     * @param throwOnFailure whether to throw exception on capture failure
     * @return Client IP address or default value if unknown
     * @throws IpCaptureException if capture fails and throwOnFailure is true
     * Requirements: 6.2 - Handle IP capture failures gracefully
     */
    String extractClientIpAddressWithErrorHandling(HttpServletRequest request, boolean throwOnFailure) throws IpCaptureException;
    
    /**
     * Validates IP address format for both IPv4 and IPv6.
     * 
     * @param ipAddress IP address to validate
     * @return true if valid IPv4 or IPv6 format, false otherwise
     * Requirements: 1.4 - Validate IP address format before storing
     */
    boolean isValidIpAddress(String ipAddress);
    
    /**
     * Validates IP address and throws detailed exception if invalid.
     * 
     * @param ipAddress IP address to validate
     * @throws IpValidationException if IP address is invalid
     * Requirements: 1.3 - IP validation error messages
     *               1.4 - Validate IP address format before storing
     */
    void validateIpAddressWithException(String ipAddress) throws IpValidationException;
    
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
    
    /**
     * Extracts and sanitizes client IP address with security validation.
     * 
     * @param request HTTP servlet request
     * @return Sanitized and security-validated IP address
     * @throws IpValidationException if IP address fails security validation
     * Requirements: 5.1 - Secure IP address storage constraints
     */
    String extractAndSanitizeIpAddress(HttpServletRequest request) throws IpValidationException;
}