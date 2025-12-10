package com.bidb.personetakip.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/**
 * Implementation of IP address service for extracting, validating, and formatting IP addresses.
 * Requirements: 1.1, 1.2, 1.3, 1.4
 */
@Service
public class IpAddressServiceImpl implements IpAddressService {
    
    private static final Logger logger = LoggerFactory.getLogger(IpAddressServiceImpl.class);
    
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
    
    @Override
    public String extractClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            logger.warn("HttpServletRequest is null, returning unknown IP default");
            return UNKNOWN_IP_DEFAULT;
        }
        
        try {
            // Check proxy headers first for real client IP
            for (String header : IP_HEADER_CANDIDATES) {
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