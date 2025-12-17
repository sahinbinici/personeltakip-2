package com.bidb.personetakip.service;

import com.bidb.personetakip.exception.IpCaptureException;
import com.bidb.personetakip.exception.IpValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Implementation of IP address service for extracting, validating, and formatting IP addresses.
 * Requirements: 1.1, 1.2, 1.3, 1.4, 6.5, 5.2
 */
@Service
public class IpAddressServiceImpl implements IpAddressService {
    
    private static final Logger logger = LoggerFactory.getLogger(IpAddressServiceImpl.class);
    
    private final IpTrackingConfigurationService configService;
    private final IpSecurityService ipSecurityService;
    
    private static final String UNKNOWN_IP_DEFAULT = "Unknown";
    
    // IPv4 pattern: matches standard IPv4 addresses (0.0.0.0 to 255.255.255.255)
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    
    // IPv6 pattern: matches standard IPv6 addresses including compressed forms
    private static final Pattern IPV6_PATTERN = Pattern.compile(
        "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|" +
        "^::1$|" +
        "^::$|" +
        "^([0-9a-fA-F]{1,4}:){1,7}:$|" +
        "^([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}$|" +
        "^([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}$|" +
        "^([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}$|" +
        "^([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}$|" +
        "^([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}$|" +
        "^[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})$|" +
        "^:((:[0-9a-fA-F]{1,4}){1,7}|:)$"
    );
    
    // Headers to check for real client IP in proxy scenarios
    private static final String[] IP_HEADER_CANDIDATES = {
        "X-Forwarded-For",
        "X-Real-IP", 
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "HTTP_VIA",
        "REMOTE_ADDR"
    };
    
    @Autowired
    public IpAddressServiceImpl(IpTrackingConfigurationService configService,
                               @Lazy IpSecurityService ipSecurityService) {
        this.configService = configService;
        this.ipSecurityService = ipSecurityService;
    }
    
    @Override
    public String extractClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            logger.warn("HttpServletRequest is null, returning unknown IP default");
            return UNKNOWN_IP_DEFAULT;
        }
        
        // Check if IP tracking is enabled
        if (!configService.isIpTrackingEnabled()) {
            logger.debug("IP tracking is disabled, returning unknown IP default");
            return UNKNOWN_IP_DEFAULT;
        }
        
        try {
            // Apply timeout for IP extraction to prevent performance issues
            long startTime = System.currentTimeMillis();
            int timeoutMs = configService.getCaptureTimeoutMs();
            
            // Check proxy headers first for real client IP
            for (String header : IP_HEADER_CANDIDATES) {
                // Check timeout
                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    logger.warn("IP extraction timeout exceeded ({}ms), returning unknown default", timeoutMs);
                    return UNKNOWN_IP_DEFAULT;
                }
                
                String ipAddress = request.getHeader(header);
                if (isValidProxyIpAddress(ipAddress)) {
                    // X-Forwarded-For can contain multiple IPs, take the first one
                    if ("X-Forwarded-For".equals(header) && ipAddress.contains(",")) {
                        ipAddress = ipAddress.split(",")[0].trim();
                    }
                    
                    if (isValidIpAddress(ipAddress)) {
                        logger.debug("Extracted IP address from header {}: {}", header, ipAddress);
                        return ipAddress;
                    }
                }
            }
            
            // Fall back to remote address
            String remoteAddr = request.getRemoteAddr();
            if (isValidIpAddress(remoteAddr)) {
                logger.debug("Using remote address as IP: {}", remoteAddr);
                return remoteAddr;
            }
            
            logger.warn("Could not extract valid IP address from request, returning unknown default");
            return UNKNOWN_IP_DEFAULT;
            
        } catch (Exception e) {
            logger.error("Error extracting IP address from request", e);
            return UNKNOWN_IP_DEFAULT;
        }
    }
    
    /**
     * Extracts client IP address with detailed error handling
     * Requirements: 6.2
     */
    public String extractClientIpAddressWithErrorHandling(HttpServletRequest request, boolean throwOnFailure) throws IpCaptureException {
        if (request == null) {
            String message = "HttpServletRequest is null, cannot extract IP address";
            logger.warn(message);
            if (throwOnFailure) {
                throw new IpCaptureException(message, "null_request", false);
            }
            return UNKNOWN_IP_DEFAULT;
        }
        
        // Check if IP tracking is enabled
        if (!configService.isIpTrackingEnabled()) {
            String message = "IP tracking is disabled in configuration";
            logger.debug(message);
            if (throwOnFailure) {
                throw new IpCaptureException(message, "tracking_disabled", false);
            }
            return UNKNOWN_IP_DEFAULT;
        }
        
        try {
            // Apply timeout for IP extraction to prevent performance issues
            long startTime = System.currentTimeMillis();
            int timeoutMs = configService.getCaptureTimeoutMs();
            
            // Check proxy headers first for real client IP
            for (String header : IP_HEADER_CANDIDATES) {
                // Check timeout
                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    String message = String.format("IP extraction timeout exceeded (%dms)", timeoutMs);
                    logger.warn(message);
                    if (throwOnFailure) {
                        throw new IpCaptureException(message, "timeout_exceeded", false);
                    }
                    return UNKNOWN_IP_DEFAULT;
                }
                
                String ipAddress = request.getHeader(header);
                if (isValidProxyIpAddress(ipAddress)) {
                    // X-Forwarded-For can contain multiple IPs, take the first one
                    if ("X-Forwarded-For".equals(header) && ipAddress.contains(",")) {
                        ipAddress = ipAddress.split(",")[0].trim();
                    }
                    
                    if (isValidIpAddress(ipAddress)) {
                        logger.debug("Extracted IP address from header {}: {}", header, ipAddress);
                        return ipAddress;
                    }
                }
            }
            
            // Fall back to remote address
            String remoteAddr = request.getRemoteAddr();
            if (isValidIpAddress(remoteAddr)) {
                logger.debug("Using remote address as IP: {}", remoteAddr);
                return remoteAddr;
            }
            
            String message = "Could not extract valid IP address from any request headers or remote address";
            logger.warn(message);
            if (throwOnFailure) {
                throw new IpCaptureException(message, "no_valid_ip_found", false);
            }
            return UNKNOWN_IP_DEFAULT;
            
        } catch (IpCaptureException e) {
            // Re-throw IP capture exceptions
            throw e;
        } catch (Exception e) {
            String message = "Unexpected error during IP address extraction: " + e.getMessage();
            logger.error(message, e);
            if (throwOnFailure) {
                throw new IpCaptureException(message, "unexpected_error", e);
            }
            return UNKNOWN_IP_DEFAULT;
        }
    }
    
    @Override
    public boolean isValidIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return false;
        }
        
        String trimmedIp = ipAddress.trim();
        
        // Check IPv4 format first - this is more strict
        if (IPV4_PATTERN.matcher(trimmedIp).matches()) {
            return true;
        }
        
        // Check IPv6 format
        if (IPV6_PATTERN.matcher(trimmedIp).matches()) {
            return true;
        }
        
        // Additional validation using InetAddress for edge cases, but only for IPv6-like addresses
        // InetAddress.getByName() is too permissive for IPv4 (accepts incomplete addresses)
        if (trimmedIp.contains(":")) {
            try {
                InetAddress.getByName(trimmedIp);
                return true;
            } catch (UnknownHostException e) {
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Validates IP address and throws detailed exception if invalid
     * Requirements: 1.3, 1.4
     */
    public void validateIpAddressWithException(String ipAddress) throws IpValidationException {
        if (ipAddress == null) {
            throw new IpValidationException("IP address cannot be null", null, "null value");
        }
        
        if (ipAddress.trim().isEmpty()) {
            throw new IpValidationException("IP address cannot be empty", ipAddress, "empty string");
        }
        
        String trimmedIp = ipAddress.trim();
        
        // Check for common invalid patterns
        if (trimmedIp.length() > 45) { // Max length for IPv6
            throw new IpValidationException("IP address too long", trimmedIp, "exceeds maximum length of 45 characters");
        }
        
        if (trimmedIp.contains(" ")) {
            throw new IpValidationException("IP address contains spaces", trimmedIp, "spaces not allowed in IP addresses");
        }
        
        // Check IPv4 format
        if (trimmedIp.contains(".")) {
            if (!IPV4_PATTERN.matcher(trimmedIp).matches()) {
                throw new IpValidationException("Invalid IPv4 address format", trimmedIp, "must be in format x.x.x.x where x is 0-255");
            }
            return;
        }
        
        // Check IPv6 format
        if (trimmedIp.contains(":")) {
            if (!IPV6_PATTERN.matcher(trimmedIp).matches()) {
                try {
                    InetAddress.getByName(trimmedIp);
                } catch (UnknownHostException e) {
                    throw new IpValidationException("Invalid IPv6 address format", trimmedIp, "invalid IPv6 format: " + e.getMessage());
                }
            }
            return;
        }
        
        // Neither IPv4 nor IPv6
        throw new IpValidationException("Invalid IP address format", trimmedIp, "must be valid IPv4 or IPv6 address");
    }
    
    @Override
    public String getUnknownIpDefault() {
        return UNKNOWN_IP_DEFAULT;
    }
    
    @Override
    public String formatIpAddress(String ipAddress) {
        if (ipAddress == null) {
            return UNKNOWN_IP_DEFAULT;
        }
        
        String trimmedIp = ipAddress.trim();
        
        if (trimmedIp.isEmpty() || UNKNOWN_IP_DEFAULT.equals(trimmedIp)) {
            return UNKNOWN_IP_DEFAULT;
        }
        
        // For IPv6, ensure consistent case (lowercase)
        if (trimmedIp.contains(":")) {
            return trimmedIp.toLowerCase();
        }
        
        // For IPv4, return as-is after trimming
        return trimmedIp;
    }
    
    @Override
    public String extractAndSanitizeIpAddress(HttpServletRequest request) throws IpValidationException {
        // First extract the IP address using existing logic
        String rawIpAddress = extractClientIpAddress(request);
        
        // If it's the unknown default, return as-is
        if (UNKNOWN_IP_DEFAULT.equals(rawIpAddress)) {
            return rawIpAddress;
        }
        
        // Sanitize and validate for security
        String sanitizedIp = ipSecurityService.sanitizeIpAddress(rawIpAddress);
        
        // Validate secure storage requirements
        ipSecurityService.validateSecureStorage(sanitizedIp);
        
        logger.debug("Successfully extracted and sanitized IP address: {} -> {}", rawIpAddress, sanitizedIp);
        return sanitizedIp;
    }
    
    /**
     * Checks if IP address from proxy header is valid and not a placeholder.
     * 
     * @param ipAddress IP address from proxy header
     * @return true if valid and not a placeholder value
     */
    private boolean isValidProxyIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return false;
        }
        
        String trimmedIp = ipAddress.trim();
        
        // Common placeholder values that should be ignored
        return !trimmedIp.equalsIgnoreCase("unknown") &&
               !trimmedIp.equalsIgnoreCase("null") &&
               !trimmedIp.equals("-") &&
               !trimmedIp.isEmpty();
    }
}