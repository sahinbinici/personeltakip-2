package com.bidb.personetakip.service;

import com.bidb.personetakip.model.IpAddressAction;
import com.bidb.personetakip.model.IpAddressLog;
import com.bidb.personetakip.repository.IpAddressLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Implementation of IP privacy service for anonymization and audit logging.
 * Requirements: 5.2, 5.3, 5.5
 */
@Service
public class IpPrivacyServiceImpl implements IpPrivacyService {
    
    private static final Logger logger = LoggerFactory.getLogger(IpPrivacyServiceImpl.class);
    
    @Autowired
    private IpAddressLogRepository ipAddressLogRepository;
    
    @Autowired
    private IpAddressService ipAddressService;
    
    // Configuration properties for privacy settings
    @Value("${ip.privacy.enabled:false}")
    private boolean privacyModeEnabled;
    
    @Value("${ip.anonymization.level:PARTIAL}")
    private String anonymizationLevelConfig;
    
    private static final String ANONYMIZATION_MASK_IPV4 = "xxx";
    private static final String ANONYMIZATION_MASK_IPV6 = "xxxx:xxxx:xxxx:xxxx";
    
    @Override
    public String displayIpAddress(String ipAddress, boolean respectPrivacySettings) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return ipAddressService.getUnknownIpDefault();
        }
        
        // If privacy settings should not be respected, return original IP
        if (!respectPrivacySettings || !isPrivacyModeEnabled()) {
            return ipAddressService.formatIpAddress(ipAddress);
        }
        
        // Apply anonymization based on configured level
        AnonymizationLevel level = getAnonymizationLevel();
        switch (level) {
            case NONE:
                return ipAddressService.formatIpAddress(ipAddress);
            case PARTIAL:
                return anonymizeIpAddress(ipAddress);
            case FULL:
                return "***.***.***.**";
            default:
                return anonymizeIpAddress(ipAddress);
        }
    }
    
    @Override
    public String anonymizeIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return ipAddressService.getUnknownIpDefault();
        }
        
        String trimmedIp = ipAddress.trim();
        
        // Handle unknown IP default
        if (ipAddressService.getUnknownIpDefault().equals(trimmedIp)) {
            return trimmedIp;
        }
        
        // Anonymize IPv4 addresses (mask last octet)
        if (isIPv4(trimmedIp)) {
            return anonymizeIPv4(trimmedIp);
        }
        
        // Anonymize IPv6 addresses (mask last 64 bits)
        if (isIPv6(trimmedIp)) {
            return anonymizeIPv6(trimmedIp);
        }
        
        // If format is unknown, return masked version
        return "***";
    }
    
    @Override
    public void logIpAddressAccess(String ipAddress, Long userId, Long adminUserId, String action) {
        try {
            IpAddressLog log = new IpAddressLog();
            log.setUserId(userId);
            log.setIpAddress(ipAddress);
            log.setAction(IpAddressAction.valueOf(action.toUpperCase()));
            log.setAdminUserId(adminUserId);
            log.setTimestamp(LocalDateTime.now());
            
            // Add details about the access
            String details = String.format("{\"accessType\": \"view\", \"ipAddress\": \"%s\"}", 
                anonymizeIpAddress(ipAddress));
            log.setDetails(details);
            
            ipAddressLogRepository.save(log);
            
            logger.debug("Logged IP address access: action={}, userId={}, adminUserId={}, ip={}", 
                action, userId, adminUserId, anonymizeIpAddress(ipAddress));
                
        } catch (Exception e) {
            logger.error("Failed to log IP address access: action={}, userId={}, adminUserId={}", 
                action, userId, adminUserId, e);
        }
    }
    
    @Override
    public void logIpAddressModification(String oldIpAddress, String newIpAddress, Long userId, Long adminUserId, String action) {
        try {
            IpAddressLog log = new IpAddressLog();
            log.setUserId(userId);
            log.setIpAddress(newIpAddress != null ? newIpAddress : oldIpAddress);
            log.setAction(IpAddressAction.valueOf(action.toUpperCase()));
            log.setAdminUserId(adminUserId);
            log.setTimestamp(LocalDateTime.now());
            
            // Add details about the modification
            String details = String.format("{\"modificationType\": \"%s\", \"oldIp\": \"%s\", \"newIp\": \"%s\"}", 
                action.toLowerCase(),
                oldIpAddress != null ? anonymizeIpAddress(oldIpAddress) : "null",
                newIpAddress != null ? anonymizeIpAddress(newIpAddress) : "null");
            log.setDetails(details);
            
            ipAddressLogRepository.save(log);
            
            logger.debug("Logged IP address modification: action={}, userId={}, adminUserId={}, oldIp={}, newIp={}", 
                action, userId, adminUserId, 
                oldIpAddress != null ? anonymizeIpAddress(oldIpAddress) : "null",
                newIpAddress != null ? anonymizeIpAddress(newIpAddress) : "null");
                
        } catch (Exception e) {
            logger.error("Failed to log IP address modification: action={}, userId={}, adminUserId={}", 
                action, userId, adminUserId, e);
        }
    }
    
    @Override
    public boolean isPrivacyModeEnabled() {
        return privacyModeEnabled;
    }
    
    @Override
    public AnonymizationLevel getAnonymizationLevel() {
        try {
            return AnonymizationLevel.valueOf(anonymizationLevelConfig.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid anonymization level configuration: {}, using PARTIAL", anonymizationLevelConfig);
            return AnonymizationLevel.PARTIAL;
        }
    }
    
    /**
     * Checks if the given IP address is IPv4 format.
     */
    private boolean isIPv4(String ipAddress) {
        return ipAddress.contains(".") && !ipAddress.contains(":");
    }
    
    /**
     * Checks if the given IP address is IPv6 format.
     */
    private boolean isIPv6(String ipAddress) {
        return ipAddress.contains(":");
    }
    
    /**
     * Anonymizes IPv4 address by masking the last octet.
     * Example: 192.168.1.100 -> 192.168.1.xxx
     */
    private String anonymizeIPv4(String ipv4) {
        String[] parts = ipv4.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + "." + parts[2] + "." + ANONYMIZATION_MASK_IPV4;
        }
        return ANONYMIZATION_MASK_IPV4;
    }
    
    /**
     * Anonymizes IPv6 address by masking the last 64 bits.
     * Example: 2001:db8:85a3::8a2e:370:7334 -> 2001:db8:85a3::xxxx:xxxx:xxxx:xxxx
     */
    private String anonymizeIPv6(String ipv6) {
        // Handle compressed IPv6 addresses
        if (ipv6.contains("::")) {
            String[] parts = ipv6.split("::");
            if (parts.length >= 1) {
                return parts[0] + "::" + ANONYMIZATION_MASK_IPV6;
            }
        }
        
        // Handle full IPv6 addresses
        String[] parts = ipv6.split(":");
        if (parts.length >= 4) {
            StringBuilder anonymized = new StringBuilder();
            // Keep first 4 groups (64 bits), mask the rest
            for (int i = 0; i < Math.min(4, parts.length); i++) {
                if (i > 0) anonymized.append(":");
                anonymized.append(parts[i]);
            }
            anonymized.append("::").append(ANONYMIZATION_MASK_IPV6);
            return anonymized.toString();
        }
        
        return ANONYMIZATION_MASK_IPV6;
    }
}