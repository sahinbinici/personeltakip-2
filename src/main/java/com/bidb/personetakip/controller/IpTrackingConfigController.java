package com.bidb.personetakip.controller;

import com.bidb.personetakip.config.IpTrackingConfig;
import com.bidb.personetakip.service.IpTrackingConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for managing IP tracking configuration settings.
 * Provides endpoints for viewing and updating IP tracking configuration.
 * 
 * Requirements: 6.5 - IP tracking configuration control, 5.2 - Privacy settings
 */
@Controller
@RequestMapping("/admin/ip-tracking-config")
@PreAuthorize("hasRole('ADMIN')")
public class IpTrackingConfigController {
    
    private static final Logger logger = LoggerFactory.getLogger(IpTrackingConfigController.class);
    
    private final IpTrackingConfigurationService configService;
    
    @Autowired
    public IpTrackingConfigController(IpTrackingConfigurationService configService) {
        this.configService = configService;
    }
    
    /**
     * Display IP tracking configuration page
     */
    @GetMapping
    public String showConfigurationPage(Model model) {
        try {
            IpTrackingConfig config = configService.getConfiguration();
            model.addAttribute("config", config);
            model.addAttribute("anonymizationMethods", IpTrackingConfig.AnonymizationMethod.values());
            
            logger.debug("Displaying IP tracking configuration page");
            return "admin-ip-tracking-config";
            
        } catch (Exception e) {
            logger.error("Error loading IP tracking configuration page", e);
            model.addAttribute("error", "Failed to load configuration: " + e.getMessage());
            return "admin-ip-tracking-config";
        }
    }
    
    /**
     * Get current IP tracking configuration as JSON
     */
    @GetMapping("/api/config")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getConfiguration() {
        try {
            Map<String, Object> configMap = new HashMap<>();
            
            // Main settings
            configMap.put("enabled", configService.isIpTrackingEnabled());
            
            // Privacy settings
            Map<String, Object> privacy = new HashMap<>();
            privacy.put("enabled", configService.isPrivacyModeEnabled());
            privacy.put("anonymizeReports", configService.shouldAnonymizeReports());
            privacy.put("auditLogging", configService.isAuditLoggingEnabled());
            privacy.put("retentionDays", configService.getRetentionDays());
            privacy.put("requireConsent", configService.isConsentRequired());
            configMap.put("privacy", privacy);
            
            // Anonymization settings
            Map<String, Object> anonymization = new HashMap<>();
            anonymization.put("enabled", configService.isAnonymizationEnabled());
            anonymization.put("method", configService.getAnonymizationMethod().name());
            anonymization.put("ipv4PreserveOctets", configService.getIpv4PreserveOctets());
            anonymization.put("ipv6PreserveGroups", configService.getIpv6PreserveGroups());
            anonymization.put("maskCharacter", configService.getMaskCharacter());
            configMap.put("anonymization", anonymization);
            
            // Performance settings
            Map<String, Object> performance = new HashMap<>();
            performance.put("captureTimeoutMs", configService.getCaptureTimeoutMs());
            performance.put("asyncLogging", configService.isAsyncLoggingEnabled());
            performance.put("cacheSize", configService.getCacheSize());
            performance.put("cacheTtlSeconds", configService.getCacheTtlSeconds());
            performance.put("cacheValidation", configService.isValidationCachingEnabled());
            performance.put("batchSize", configService.getBatchSize());
            configMap.put("performance", performance);
            
            return ResponseEntity.ok(configMap);
            
        } catch (Exception e) {
            logger.error("Error retrieving IP tracking configuration", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve configuration: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Get IP tracking status (enabled/disabled)
     */
    @GetMapping("/api/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTrackingStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("enabled", configService.isIpTrackingEnabled());
            status.put("privacyMode", configService.isPrivacyModeEnabled());
            status.put("anonymizationEnabled", configService.isAnonymizationEnabled());
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.error("Error retrieving IP tracking status", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Test IP anonymization with current settings
     */
    @PostMapping("/api/test-anonymization")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testAnonymization(@RequestBody Map<String, String> request) {
        try {
            String testIp = request.get("ipAddress");
            if (testIp == null || testIp.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "IP address is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("originalIp", testIp);
            result.put("anonymizationEnabled", configService.isAnonymizationEnabled());
            
            if (configService.isAnonymizationEnabled()) {
                // This would require access to IpPrivacyService to test anonymization
                result.put("anonymizedIp", "Test anonymization not implemented in this endpoint");
                result.put("method", configService.getAnonymizationMethod().name());
            } else {
                result.put("anonymizedIp", testIp);
                result.put("method", "NONE");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error testing IP anonymization", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to test anonymization: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Get configuration help and documentation
     */
    @GetMapping("/api/help")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getConfigurationHelp() {
        Map<String, Object> help = new HashMap<>();
        
        // Main settings help
        Map<String, String> mainHelp = new HashMap<>();
        mainHelp.put("enabled", "Enable or disable IP address tracking for all entry/exit operations");
        help.put("main", mainHelp);
        
        // Privacy settings help
        Map<String, String> privacyHelp = new HashMap<>();
        privacyHelp.put("enabled", "Enable privacy-compliant IP address handling and display");
        privacyHelp.put("anonymizeReports", "Automatically anonymize IP addresses in all reports and exports");
        privacyHelp.put("auditLogging", "Log all IP address access and modification operations for audit purposes");
        privacyHelp.put("retentionDays", "Number of days to retain IP address logs (0 = no limit)");
        privacyHelp.put("requireConsent", "Require user consent before collecting IP address information");
        help.put("privacy", privacyHelp);
        
        // Anonymization settings help
        Map<String, String> anonymizationHelp = new HashMap<>();
        anonymizationHelp.put("enabled", "Enable IP address anonymization for display and logging");
        anonymizationHelp.put("method", "Anonymization method: MASK (replace with characters), HASH (SHA-256), TRUNCATE (remove parts)");
        anonymizationHelp.put("ipv4PreserveOctets", "Number of IPv4 octets to preserve (1-4, e.g., 2 = 192.168.x.x)");
        anonymizationHelp.put("ipv6PreserveGroups", "Number of IPv6 groups to preserve (1-8)");
        anonymizationHelp.put("maskCharacter", "Character to use for masking IP addresses");
        help.put("anonymization", anonymizationHelp);
        
        // Performance settings help
        Map<String, String> performanceHelp = new HashMap<>();
        performanceHelp.put("captureTimeoutMs", "Maximum time to spend capturing IP address (milliseconds)");
        performanceHelp.put("asyncLogging", "Use asynchronous logging to improve response times");
        performanceHelp.put("cacheSize", "Maximum number of IP addresses to cache for quick lookups");
        performanceHelp.put("cacheTtlSeconds", "Time-to-live for cached IP address data (seconds)");
        performanceHelp.put("cacheValidation", "Cache IP address validation results to improve performance");
        performanceHelp.put("batchSize", "Number of records to process in bulk operations");
        help.put("performance", performanceHelp);
        
        return ResponseEntity.ok(help);
    }
}