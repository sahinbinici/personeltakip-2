package com.bidb.personetakip.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for automated IP data retention policy enforcement.
 * Runs scheduled cleanup tasks to maintain data retention compliance.
 * Requirements: 5.3
 */
@Service
public class IpDataRetentionService {
    
    private static final Logger logger = LoggerFactory.getLogger(IpDataRetentionService.class);
    
    private final IpSecurityService ipSecurityService;
    private final IpTrackingConfigurationService configService;
    
    @Autowired
    public IpDataRetentionService(IpSecurityService ipSecurityService,
                                 IpTrackingConfigurationService configService) {
        this.ipSecurityService = ipSecurityService;
        this.configService = configService;
    }
    
    /**
     * Scheduled task to enforce IP data retention policy.
     * Runs daily at 2:00 AM to clean up old IP address logs.
     * Requirements: 5.3
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2:00 AM
    public void enforceRetentionPolicy() {
        try {
            if (!configService.isAuditLoggingEnabled()) {
                logger.debug("IP audit logging is disabled, skipping retention policy enforcement");
                return;
            }
            
            int retentionDays = configService.getRetentionDays();
            if (retentionDays <= 0) {
                logger.debug("IP data retention policy is disabled (retentionDays={})", retentionDays);
                return;
            }
            
            logger.info("Starting IP data retention policy enforcement (retention: {} days)", retentionDays);
            
            int cleanedRecords = ipSecurityService.enforceRetentionPolicy();
            
            if (cleanedRecords > 0) {
                logger.info("IP data retention policy enforcement completed: {} records cleaned", cleanedRecords);
            } else {
                logger.debug("IP data retention policy enforcement completed: no records to clean");
            }
            
        } catch (Exception e) {
            logger.error("Failed to enforce IP data retention policy", e);
        }
    }
    
    /**
     * Scheduled task to perform security monitoring and suspicious access detection.
     * Runs every hour to detect and log suspicious IP access patterns.
     * Requirements: 5.3
     */
    @Scheduled(fixedRate = 3600000) // Every hour (3600000 ms)
    public void performSecurityMonitoring() {
        try {
            if (!configService.isAuditLoggingEnabled()) {
                return;
            }
            
            logger.debug("Performing IP data security monitoring");
            
            // This is a placeholder for more comprehensive security monitoring
            // In a real implementation, you might:
            // 1. Analyze access patterns across all users
            // 2. Detect anomalies in IP access frequency
            // 3. Check for unauthorized access attempts
            // 4. Generate security alerts for administrators
            
            // For now, we'll just log that monitoring is active
            logger.debug("IP data security monitoring completed");
            
        } catch (Exception e) {
            logger.error("Failed to perform IP data security monitoring", e);
        }
    }
    
    /**
     * Manual trigger for retention policy enforcement.
     * Can be called by administrators for immediate cleanup.
     * Requirements: 5.3
     * 
     * @return Number of records cleaned up
     */
    public int manualRetentionEnforcement() {
        try {
            logger.info("Manual IP data retention policy enforcement triggered");
            
            int cleanedRecords = ipSecurityService.enforceRetentionPolicy();
            
            logger.info("Manual IP data retention policy enforcement completed: {} records cleaned", cleanedRecords);
            return cleanedRecords;
            
        } catch (Exception e) {
            logger.error("Failed to perform manual IP data retention policy enforcement", e);
            return 0;
        }
    }
    
    /**
     * Get retention policy status and statistics.
     * Requirements: 5.3
     * 
     * @return Retention policy status information
     */
    public RetentionPolicyStatus getRetentionPolicyStatus() {
        try {
            boolean enabled = configService.isAuditLoggingEnabled();
            int retentionDays = configService.getRetentionDays();
            boolean policyActive = enabled && retentionDays > 0;
            
            return new RetentionPolicyStatus(enabled, retentionDays, policyActive);
            
        } catch (Exception e) {
            logger.error("Failed to get retention policy status", e);
            return new RetentionPolicyStatus(false, 0, false);
        }
    }
    
    /**
     * Data class for retention policy status information
     */
    public static class RetentionPolicyStatus {
        private final boolean auditLoggingEnabled;
        private final int retentionDays;
        private final boolean policyActive;
        
        public RetentionPolicyStatus(boolean auditLoggingEnabled, int retentionDays, boolean policyActive) {
            this.auditLoggingEnabled = auditLoggingEnabled;
            this.retentionDays = retentionDays;
            this.policyActive = policyActive;
        }
        
        public boolean isAuditLoggingEnabled() {
            return auditLoggingEnabled;
        }
        
        public int getRetentionDays() {
            return retentionDays;
        }
        
        public boolean isPolicyActive() {
            return policyActive;
        }
        
        @Override
        public String toString() {
            return String.format("RetentionPolicyStatus{auditLogging=%s, retentionDays=%d, active=%s}", 
                auditLoggingEnabled, retentionDays, policyActive);
        }
    }
}