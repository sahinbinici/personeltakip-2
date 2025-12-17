package com.bidb.personetakip.service;

import com.bidb.personetakip.config.IpTrackingConfig;
import com.bidb.personetakip.exception.IpPrivacyConfigurationException;
import com.bidb.personetakip.model.IpAddressAction;
import com.bidb.personetakip.model.IpAddressLog;
import com.bidb.personetakip.repository.IpAddressLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

/**
 * Implementation of IP privacy service for anonymization and audit logging.
 * Requirements: 5.2, 5.3, 5.5, 6.5
 */
@Service
public class IpPrivacyServiceImpl implements IpPrivacyService {
    
    private static final Logger logger = LoggerFactory.getLogger(IpPrivacyServiceImpl.class);
    
    private final IpAddressLogRepository ipAddressLogRepository;
    private final IpAddressService ipAddressService;
    private final IpTrackingConfigurationService configService;
    
    @Autowired
    public IpPrivacyServiceImpl(IpAddressLogRepository ipAddressLogRepository, 
                               IpAddressService ipAddressService,
                               IpTrackingConfigurationService configService) {
        this.ipAddressLogRepository = ipAddressLogRepository;
        this.ipAddressService = ipAddressService;
        this.configService = configService;
    }
    
    @Override
    public String displayIpAddress(String ipAddress, boolean respectPrivacySettings) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return ipAddressService.getUnknownIpDefault();
        }
        
        try {
            // If privacy settings should not be respected, return original IP
            if (!respectPrivacySettings || !configService.isPrivacyModeEnabled()) {
                return ipAddressService.formatIpAddress(ipAddress);
            }
            
            // Apply anonymization if enabled
            if (configService.isAnonymizationEnabled()) {
                return anonymizeIpAddressInternal(ipAddress);
            }
            
            // Return formatted IP without anonymization
            return ipAddressService.formatIpAddress(ipAddress);
            
        } catch (Exception e) {
            logger.error("Error displaying IP address with privacy settings", e);
            // Fallback to unknown IP to protect privacy
            return ipAddressService.getUnknownIpDefault();
        }
    }
    
    /**
     * Displays IP address with detailed error handling
     * Requirements: 5.2, 5.5
     */
    public String displayIpAddressWithErrorHandling(String ipAddress, boolean respectPrivacySettings, boolean throwOnError) throws IpPrivacyConfigurationException {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return ipAddressService.getUnknownIpDefault();
        }
        
        try {
            // Validate privacy configuration
            if (respectPrivacySettings) {
                validatePrivacyConfiguration();
            }
            
            // If privacy settings should not be respected, return original IP
            if (!respectPrivacySettings || !configService.isPrivacyModeEnabled()) {
                return ipAddressService.formatIpAddress(ipAddress);
            }
            
            // Apply anonymization if enabled
            if (configService.isAnonymizationEnabled()) {
                return anonymizeIpAddressInternal(ipAddress);
            }
            
            // Return formatted IP without anonymization
            return ipAddressService.formatIpAddress(ipAddress);
            
        } catch (IpPrivacyConfigurationException e) {
            if (throwOnError) {
                throw e;
            }
            logger.error("Privacy configuration error, falling back to unknown IP", e);
            return ipAddressService.getUnknownIpDefault();
        } catch (Exception e) {
            String message = "Unexpected error displaying IP address with privacy settings: " + e.getMessage();
            if (throwOnError) {
                throw new IpPrivacyConfigurationException(message, e);
            }
            logger.error(message, e);
            return ipAddressService.getUnknownIpDefault();
        }
    }
    
    /**
     * Validates privacy configuration settings
     */
    private void validatePrivacyConfiguration() throws IpPrivacyConfigurationException {
        try {
            // Check if anonymization is enabled but method is invalid
            if (configService.isAnonymizationEnabled()) {
                IpTrackingConfig.AnonymizationMethod method = configService.getAnonymizationMethod();
                if (method == null) {
                    throw new IpPrivacyConfigurationException("Anonymization method is null", "anonymization.method", "null", "validate");
                }
                
                // Validate method-specific settings
                switch (method) {
                    case MASK:
                        String maskChar = configService.getMaskCharacter();
                        if (maskChar == null || maskChar.isEmpty()) {
                            throw new IpPrivacyConfigurationException("Mask character is not configured", "anonymization.maskCharacter", maskChar, "validate");
                        }
                        break;
                    case TRUNCATE:
                        int ipv4Preserve = configService.getIpv4PreserveOctets();
                        int ipv6Preserve = configService.getIpv6PreserveGroups();
                        if (ipv4Preserve < 0 || ipv4Preserve > 4) {
                            throw new IpPrivacyConfigurationException("Invalid IPv4 preserve octets configuration", "anonymization.ipv4PreserveOctets", String.valueOf(ipv4Preserve), "validate");
                        }
                        if (ipv6Preserve < 0 || ipv6Preserve > 8) {
                            throw new IpPrivacyConfigurationException("Invalid IPv6 preserve groups configuration", "anonymization.ipv6PreserveGroups", String.valueOf(ipv6Preserve), "validate");
                        }
                        break;
                    case HASH:
                        // Hash method doesn't require additional validation
                        break;
                    default:
                        throw new IpPrivacyConfigurationException("Unknown anonymization method", "anonymization.method", method.toString(), "validate");
                }
            }
            
        } catch (IpPrivacyConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new IpPrivacyConfigurationException("Error validating privacy configuration: " + e.getMessage(), e);
        }
    }
    
    /**
     * Anonymize IP address based on configuration settings
     */
    private String anonymizeIpAddressInternal(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return ipAddressService.getUnknownIpDefault();
        }
        
        IpTrackingConfig.AnonymizationMethod method = configService.getAnonymizationMethod();
        
        switch (method) {
            case MASK:
                return maskIpAddress(ipAddress);
            case HASH:
                return hashIpAddress(ipAddress);
            case TRUNCATE:
                return truncateIpAddress(ipAddress);
            default:
                logger.warn("Unknown anonymization method: {}, using MASK", method);
                return maskIpAddress(ipAddress);
        }
    }
    
    /**
     * Mask IP address by replacing parts with mask character
     */
    private String maskIpAddress(String ipAddress) {
        String maskChar = configService.getMaskCharacter();
        
        if (ipAddress.contains(".")) {
            // IPv4 masking
            String[] parts = ipAddress.split("\\.");
            if (parts.length == 4) {
                int preserveOctets = Math.min(configService.getIpv4PreserveOctets(), 4);
                StringBuilder masked = new StringBuilder();
                
                for (int i = 0; i < 4; i++) {
                    if (i > 0) masked.append(".");
                    if (i < preserveOctets) {
                        masked.append(parts[i]);
                    } else {
                        masked.append(maskChar.repeat(parts[i].length()));
                    }
                }
                return masked.toString();
            }
        } else if (ipAddress.contains(":")) {
            // IPv6 masking
            String[] parts = ipAddress.split(":");
            int preserveGroups = Math.min(configService.getIpv6PreserveGroups(), parts.length);
            StringBuilder masked = new StringBuilder();
            
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) masked.append(":");
                if (i < preserveGroups) {
                    masked.append(parts[i]);
                } else {
                    masked.append(maskChar.repeat(Math.max(parts[i].length(), 4)));
                }
            }
            return masked.toString();
        }
        
        // Fallback: mask entire IP
        return maskChar.repeat(ipAddress.length());
    }
    
    /**
     * Hash IP address using SHA-256
     */
    private String hashIpAddress(String ipAddress) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ipAddress.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            // Return first 16 characters of hash for readability
            return "hash:" + hexString.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available for IP hashing", e);
            return maskIpAddress(ipAddress); // Fallback to masking
        }
    }
    
    /**
     * Truncate IP address to preserve only specified parts
     */
    private String truncateIpAddress(String ipAddress) {
        if (ipAddress.contains(".")) {
            // IPv4 truncation
            String[] parts = ipAddress.split("\\.");
            if (parts.length == 4) {
                int preserveOctets = Math.min(configService.getIpv4PreserveOctets(), 4);
                StringBuilder truncated = new StringBuilder();
                
                for (int i = 0; i < preserveOctets; i++) {
                    if (i > 0) truncated.append(".");
                    truncated.append(parts[i]);
                }
                
                // Add zeros for remaining octets
                for (int i = preserveOctets; i < 4; i++) {
                    truncated.append(".0");
                }
                
                return truncated.toString();
            }
        } else if (ipAddress.contains(":")) {
            // IPv6 truncation
            String[] parts = ipAddress.split(":");
            int preserveGroups = Math.min(configService.getIpv6PreserveGroups(), parts.length);
            StringBuilder truncated = new StringBuilder();
            
            for (int i = 0; i < preserveGroups; i++) {
                if (i > 0) truncated.append(":");
                truncated.append(parts[i]);
            }
            
            // Add :: for remaining groups
            if (preserveGroups < parts.length) {
                truncated.append("::");
            }
            
            return truncated.toString();
        }
        
        // Fallback: return as-is
        return ipAddress;
    }
    
    @Override
    public String anonymizeIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return ipAddressService.getUnknownIpDefault();
        }
        
        // Use the new configuration-based anonymization
        return anonymizeIpAddressInternal(ipAddress);
    }
    
    @Override
    public AnonymizationLevel getAnonymizationLevel() {
        // Map new configuration to old enum for backward compatibility
        if (!configService.isAnonymizationEnabled()) {
            return AnonymizationLevel.NONE;
        }
        
        IpTrackingConfig.AnonymizationMethod method = configService.getAnonymizationMethod();
        switch (method) {
            case HASH:
                return AnonymizationLevel.FULL;
            case MASK:
            case TRUNCATE:
            default:
                return AnonymizationLevel.PARTIAL;
        }
    }
    
    @Override
    public void logIpAddressAccess(String ipAddress, Long userId, Long adminUserId, String action) {
        // Check if audit logging is enabled
        if (!configService.isAuditLoggingEnabled()) {
            return;
        }
        
        try {
            // Validate action parameter - be lenient for backward compatibility
            if (action == null || action.trim().isEmpty()) {
                logger.warn("Audit log action is null or empty, skipping audit log");
                return;
            }
            
            // Validate action is a valid enum value
            IpAddressAction actionEnum;
            try {
                actionEnum = IpAddressAction.valueOf(action.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid audit log action: {}, skipping audit log", action);
                return;
            }
            
            IpAddressLog log = new IpAddressLog();
            log.setUserId(userId);
            log.setIpAddress(ipAddress);
            log.setAction(actionEnum);
            log.setAdminUserId(adminUserId);
            log.setTimestamp(LocalDateTime.now());
            
            // Add details about the access with error handling
            String displayedIp;
            try {
                displayedIp = displayIpAddress(ipAddress, true);
            } catch (Exception e) {
                logger.warn("Error displaying IP address for audit log, using raw IP", e);
                displayedIp = ipAddress != null ? ipAddress : "null";
            }
            
            String details = String.format("{\"accessType\": \"view\", \"ipAddress\": \"%s\"}", displayedIp);
            log.setDetails(details);
            
            ipAddressLogRepository.save(log);
            
            logger.debug("Logged IP address access: action={}, userId={}, adminUserId={}, ip={}", 
                action, userId, adminUserId, displayedIp);
                
        } catch (IpPrivacyConfigurationException e) {
            logger.error("Privacy configuration error during audit logging: {}", e.getMessage(), e);
            // Don't re-throw to avoid breaking calling operations
        } catch (Exception e) {
            logger.error("Failed to log IP address access: action={}, userId={}, adminUserId={}", 
                action, userId, adminUserId, e);
        }
    }
    
    @Override
    public void logIpAddressModification(String oldIpAddress, String newIpAddress, Long userId, Long adminUserId, String action) {
        // Check if audit logging is enabled
        if (!configService.isAuditLoggingEnabled()) {
            return;
        }
        
        try {
            // Validate action parameter - be lenient for backward compatibility
            if (action == null || action.trim().isEmpty()) {
                logger.warn("Audit log action is null or empty, skipping audit log");
                return;
            }
            
            // Validate action is a valid enum value
            IpAddressAction actionEnum;
            try {
                actionEnum = IpAddressAction.valueOf(action.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid audit log action: {}, skipping audit log", action);
                return;
            }
            
            IpAddressLog log = new IpAddressLog();
            log.setUserId(userId);
            log.setIpAddress(newIpAddress != null ? newIpAddress : oldIpAddress);
            log.setAction(actionEnum);
            log.setAdminUserId(adminUserId);
            log.setTimestamp(LocalDateTime.now());
            
            // Add details about the modification with error handling
            String displayedOldIp, displayedNewIp;
            try {
                displayedOldIp = oldIpAddress != null ? displayIpAddress(oldIpAddress, true) : "null";
                displayedNewIp = newIpAddress != null ? displayIpAddress(newIpAddress, true) : "null";
            } catch (Exception e) {
                logger.warn("Error displaying IP addresses for audit log, using raw IPs", e);
                displayedOldIp = oldIpAddress != null ? oldIpAddress : "null";
                displayedNewIp = newIpAddress != null ? newIpAddress : "null";
            }
            
            String details = String.format("{\"modificationType\": \"%s\", \"oldIp\": \"%s\", \"newIp\": \"%s\"}", 
                action.toLowerCase(), displayedOldIp, displayedNewIp);
            log.setDetails(details);
            
            ipAddressLogRepository.save(log);
            
            logger.debug("Logged IP address modification: action={}, userId={}, adminUserId={}, oldIp={}, newIp={}", 
                action, userId, adminUserId, displayedOldIp, displayedNewIp);
                
        } catch (IpPrivacyConfigurationException e) {
            logger.error("Privacy configuration error during audit logging: {}", e.getMessage(), e);
            // Don't re-throw to avoid breaking calling operations
        } catch (Exception e) {
            logger.error("Failed to log IP address modification: action={}, userId={}, adminUserId={}", 
                action, userId, adminUserId, e);
        }
    }
    
    @Override
    public boolean isPrivacyModeEnabled() {
        return configService.isPrivacyModeEnabled();
    }
    

}