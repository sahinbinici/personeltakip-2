package com.bidb.personetakip.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for IP address tracking functionality.
 * Allows enabling/disabling IP tracking and configuring privacy settings.
 * 
 * Requirements: 6.5 - IP tracking configuration control, 5.2 - Privacy settings
 */
@Configuration
@ConfigurationProperties(prefix = "ip.tracking")
public class IpTrackingConfig {
    
    /**
     * Enable or disable IP address tracking for entry/exit operations.
     * When disabled, IP addresses will not be captured or stored.
     */
    private boolean enabled = true;
    
    /**
     * Privacy settings for IP address handling
     */
    private Privacy privacy = new Privacy();
    
    /**
     * Anonymization settings for IP addresses
     */
    private Anonymization anonymization = new Anonymization();
    
    /**
     * Performance settings for IP tracking operations
     */
    private Performance performance = new Performance();
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Privacy getPrivacy() {
        return privacy;
    }
    
    public void setPrivacy(Privacy privacy) {
        this.privacy = privacy;
    }
    
    public Anonymization getAnonymization() {
        return anonymization;
    }
    
    public void setAnonymization(Anonymization anonymization) {
        this.anonymization = anonymization;
    }
    
    public Performance getPerformance() {
        return performance;
    }
    
    public void setPerformance(Performance performance) {
        this.performance = performance;
    }
    
    /**
     * Privacy-related configuration for IP address handling
     * Requirements: 5.2 - Privacy settings configuration
     */
    public static class Privacy {
        
        /**
         * Enable privacy-compliant IP address display (masking/anonymization)
         */
        private boolean enabled = false;
        
        /**
         * Enable IP address anonymization in reports and exports
         */
        private boolean anonymizeReports = false;
        
        /**
         * Enable audit logging for IP address access and modifications
         */
        private boolean auditLogging = true;
        
        /**
         * Data retention period for IP address logs in days (0 = no retention limit)
         */
        private int retentionDays = 365;
        
        /**
         * Enable user consent tracking for IP address collection
         */
        private boolean requireConsent = false;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public boolean isAnonymizeReports() {
            return anonymizeReports;
        }
        
        public void setAnonymizeReports(boolean anonymizeReports) {
            this.anonymizeReports = anonymizeReports;
        }
        
        public boolean isAuditLogging() {
            return auditLogging;
        }
        
        public void setAuditLogging(boolean auditLogging) {
            this.auditLogging = auditLogging;
        }
        
        public int getRetentionDays() {
            return retentionDays;
        }
        
        public void setRetentionDays(int retentionDays) {
            this.retentionDays = retentionDays;
        }
        
        public boolean isRequireConsent() {
            return requireConsent;
        }
        
        public void setRequireConsent(boolean requireConsent) {
            this.requireConsent = requireConsent;
        }
    }
    
    /**
     * Anonymization configuration for IP addresses
     * Requirements: 5.2 - IP anonymization options
     */
    public static class Anonymization {
        
        /**
         * Enable IP address anonymization in all displays
         */
        private boolean enabled = false;
        
        /**
         * Anonymization method: MASK, HASH, TRUNCATE
         */
        private AnonymizationMethod method = AnonymizationMethod.MASK;
        
        /**
         * Number of octets to preserve for IPv4 (1-4, default 2 = xxx.xxx.0.0)
         */
        private int ipv4PreserveOctets = 2;
        
        /**
         * Number of groups to preserve for IPv6 (1-8, default 4)
         */
        private int ipv6PreserveGroups = 4;
        
        /**
         * Custom mask character for IP masking
         */
        private String maskCharacter = "x";
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public AnonymizationMethod getMethod() {
            return method;
        }
        
        public void setMethod(AnonymizationMethod method) {
            this.method = method;
        }
        
        public int getIpv4PreserveOctets() {
            return ipv4PreserveOctets;
        }
        
        public void setIpv4PreserveOctets(int ipv4PreserveOctets) {
            this.ipv4PreserveOctets = ipv4PreserveOctets;
        }
        
        public int getIpv6PreserveGroups() {
            return ipv6PreserveGroups;
        }
        
        public void setIpv6PreserveGroups(int ipv6PreserveGroups) {
            this.ipv6PreserveGroups = ipv6PreserveGroups;
        }
        
        public String getMaskCharacter() {
            return maskCharacter;
        }
        
        public void setMaskCharacter(String maskCharacter) {
            this.maskCharacter = maskCharacter;
        }
    }
    
    /**
     * Performance configuration for IP tracking operations
     * Requirements: 6.5 - Performance settings
     */
    public static class Performance {
        
        /**
         * Timeout for IP address capture operations in milliseconds
         */
        private int captureTimeoutMs = 1000;
        
        /**
         * Enable asynchronous IP address logging to improve response times
         */
        private boolean asyncLogging = true;
        
        /**
         * Maximum number of IP addresses to cache for quick lookups
         */
        private int cacheSize = 1000;
        
        /**
         * Cache TTL for IP address lookups in seconds
         */
        private int cacheTtlSeconds = 300;
        
        /**
         * Enable IP address validation caching to improve performance
         */
        private boolean cacheValidation = true;
        
        /**
         * Batch size for bulk IP address operations
         */
        private int batchSize = 100;
        
        public int getCaptureTimeoutMs() {
            return captureTimeoutMs;
        }
        
        public void setCaptureTimeoutMs(int captureTimeoutMs) {
            this.captureTimeoutMs = captureTimeoutMs;
        }
        
        public boolean isAsyncLogging() {
            return asyncLogging;
        }
        
        public void setAsyncLogging(boolean asyncLogging) {
            this.asyncLogging = asyncLogging;
        }
        
        public int getCacheSize() {
            return cacheSize;
        }
        
        public void setCacheSize(int cacheSize) {
            this.cacheSize = cacheSize;
        }
        
        public int getCacheTtlSeconds() {
            return cacheTtlSeconds;
        }
        
        public void setCacheTtlSeconds(int cacheTtlSeconds) {
            this.cacheTtlSeconds = cacheTtlSeconds;
        }
        
        public boolean isCacheValidation() {
            return cacheValidation;
        }
        
        public void setCacheValidation(boolean cacheValidation) {
            this.cacheValidation = cacheValidation;
        }
        
        public int getBatchSize() {
            return batchSize;
        }
        
        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }
    
    /**
     * Anonymization methods for IP addresses
     */
    public enum AnonymizationMethod {
        /**
         * Mask parts of IP address with specified character (e.g., 192.168.x.x)
         */
        MASK,
        
        /**
         * Hash the IP address using SHA-256
         */
        HASH,
        
        /**
         * Truncate IP address to preserve only specified octets/groups
         */
        TRUNCATE
    }
}