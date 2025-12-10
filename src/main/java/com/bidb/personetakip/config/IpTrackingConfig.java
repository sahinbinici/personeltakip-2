package com.bidb.personetakip.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for IP address tracking functionality.
 * Allows enabling/disabling IP tracking and configuring privacy settings.
 * 
 * Requirements: 6.5 - IP tracking configuration control
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
    
    /**
     * Privacy-related configuration for IP address handling
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
    }
}