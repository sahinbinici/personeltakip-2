package com.bidb.personetakip.service;

import com.bidb.personetakip.config.IpTrackingConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for accessing IP tracking configuration settings.
 * Provides centralized access to IP tracking configuration options.
 * 
 * Requirements: 6.5 - IP tracking configuration control, 5.2 - Privacy settings
 */
@Service
public class IpTrackingConfigurationService {
    
    private final IpTrackingConfig ipTrackingConfig;
    
    @Autowired
    public IpTrackingConfigurationService(IpTrackingConfig ipTrackingConfig) {
        this.ipTrackingConfig = ipTrackingConfig;
    }
    
    /**
     * Check if IP tracking is enabled
     * @return true if IP tracking is enabled, false otherwise
     */
    public boolean isIpTrackingEnabled() {
        return ipTrackingConfig.isEnabled();
    }
    
    /**
     * Check if privacy mode is enabled for IP addresses
     * @return true if privacy mode is enabled, false otherwise
     */
    public boolean isPrivacyModeEnabled() {
        return ipTrackingConfig.getPrivacy().isEnabled();
    }
    
    /**
     * Check if IP addresses should be anonymized in reports
     * @return true if reports should anonymize IP addresses, false otherwise
     */
    public boolean shouldAnonymizeReports() {
        return ipTrackingConfig.getPrivacy().isAnonymizeReports();
    }
    
    /**
     * Check if audit logging is enabled for IP address operations
     * @return true if audit logging is enabled, false otherwise
     */
    public boolean isAuditLoggingEnabled() {
        return ipTrackingConfig.getPrivacy().isAuditLogging();
    }
    
    /**
     * Get the data retention period for IP address logs in days
     * @return retention period in days, 0 means no retention limit
     */
    public int getRetentionDays() {
        return ipTrackingConfig.getPrivacy().getRetentionDays();
    }
    
    /**
     * Check if user consent is required for IP address collection
     * @return true if consent is required, false otherwise
     */
    public boolean isConsentRequired() {
        return ipTrackingConfig.getPrivacy().isRequireConsent();
    }
    
    /**
     * Check if IP address anonymization is enabled
     * @return true if anonymization is enabled, false otherwise
     */
    public boolean isAnonymizationEnabled() {
        return ipTrackingConfig.getAnonymization().isEnabled();
    }
    
    /**
     * Get the anonymization method for IP addresses
     * @return the anonymization method (MASK, HASH, TRUNCATE)
     */
    public IpTrackingConfig.AnonymizationMethod getAnonymizationMethod() {
        return ipTrackingConfig.getAnonymization().getMethod();
    }
    
    /**
     * Get the number of octets to preserve for IPv4 anonymization
     * @return number of octets to preserve (1-4)
     */
    public int getIpv4PreserveOctets() {
        return ipTrackingConfig.getAnonymization().getIpv4PreserveOctets();
    }
    
    /**
     * Get the number of groups to preserve for IPv6 anonymization
     * @return number of groups to preserve (1-8)
     */
    public int getIpv6PreserveGroups() {
        return ipTrackingConfig.getAnonymization().getIpv6PreserveGroups();
    }
    
    /**
     * Get the mask character used for IP address masking
     * @return the mask character (default: "x")
     */
    public String getMaskCharacter() {
        return ipTrackingConfig.getAnonymization().getMaskCharacter();
    }
    
    /**
     * Get the timeout for IP address capture operations in milliseconds
     * @return timeout in milliseconds
     */
    public int getCaptureTimeoutMs() {
        return ipTrackingConfig.getPerformance().getCaptureTimeoutMs();
    }
    
    /**
     * Check if asynchronous logging is enabled for IP operations
     * @return true if async logging is enabled, false otherwise
     */
    public boolean isAsyncLoggingEnabled() {
        return ipTrackingConfig.getPerformance().isAsyncLogging();
    }
    
    /**
     * Get the cache size for IP address lookups
     * @return cache size
     */
    public int getCacheSize() {
        return ipTrackingConfig.getPerformance().getCacheSize();
    }
    
    /**
     * Get the cache TTL for IP address lookups in seconds
     * @return cache TTL in seconds
     */
    public int getCacheTtlSeconds() {
        return ipTrackingConfig.getPerformance().getCacheTtlSeconds();
    }
    
    /**
     * Check if IP address validation caching is enabled
     * @return true if validation caching is enabled, false otherwise
     */
    public boolean isValidationCachingEnabled() {
        return ipTrackingConfig.getPerformance().isCacheValidation();
    }
    
    /**
     * Get the batch size for bulk IP address operations
     * @return batch size
     */
    public int getBatchSize() {
        return ipTrackingConfig.getPerformance().getBatchSize();
    }
    
    /**
     * Get the complete IP tracking configuration
     * @return the IP tracking configuration object
     */
    public IpTrackingConfig getConfiguration() {
        return ipTrackingConfig;
    }
}