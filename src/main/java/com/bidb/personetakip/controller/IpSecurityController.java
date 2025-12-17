package com.bidb.personetakip.controller;

import com.bidb.personetakip.model.IpAddressLog;
import com.bidb.personetakip.service.IpDataRetentionService;
import com.bidb.personetakip.service.IpSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for IP security management operations.
 * Provides endpoints for security monitoring, audit logs, and retention policy management.
 * Requirements: 5.1, 5.3
 */
@RestController
@RequestMapping("/api/admin/ip-security")
@PreAuthorize("hasRole('ADMIN')")
public class IpSecurityController {
    
    private static final Logger logger = LoggerFactory.getLogger(IpSecurityController.class);
    
    private final IpSecurityService ipSecurityService;
    private final IpDataRetentionService retentionService;
    
    @Autowired
    public IpSecurityController(IpSecurityService ipSecurityService,
                               IpDataRetentionService retentionService) {
        this.ipSecurityService = ipSecurityService;
        this.retentionService = retentionService;
    }
    
    /**
     * Get security audit logs for IP address operations.
     * Requirements: 5.3
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<List<IpAddressLog>> getSecurityAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        try {
            List<IpAddressLog> logs = ipSecurityService.getSecurityAuditLogs(userId, startDate, endDate);
            return ResponseEntity.ok(logs);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve security audit logs", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Check for suspicious IP access patterns for a specific user.
     * Requirements: 5.3
     */
    @GetMapping("/suspicious-access/{userId}")
    public ResponseEntity<Map<String, Object>> checkSuspiciousAccess(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "24") int timeWindowHours) {
        
        try {
            boolean suspicious = ipSecurityService.detectSuspiciousAccess(userId, timeWindowHours);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("timeWindowHours", timeWindowHours);
            response.put("suspiciousActivity", suspicious);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to check suspicious access for user {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Manually trigger retention policy enforcement.
     * Requirements: 5.3
     */
    @PostMapping("/retention/enforce")
    public ResponseEntity<Map<String, Object>> enforceRetentionPolicy() {
        try {
            int cleanedRecords = retentionService.manualRetentionEnforcement();
            
            Map<String, Object> response = new HashMap<>();
            response.put("cleanedRecords", cleanedRecords);
            response.put("timestamp", LocalDateTime.now());
            response.put("status", "completed");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to enforce retention policy", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get retention policy status and configuration.
     * Requirements: 5.3
     */
    @GetMapping("/retention/status")
    public ResponseEntity<IpDataRetentionService.RetentionPolicyStatus> getRetentionPolicyStatus() {
        try {
            IpDataRetentionService.RetentionPolicyStatus status = retentionService.getRetentionPolicyStatus();
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.error("Failed to get retention policy status", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Validate IP address security constraints.
     * Requirements: 5.1
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateIpSecurity(@RequestBody Map<String, String> request) {
        try {
            String ipAddress = request.get("ipAddress");
            
            Map<String, Object> response = new HashMap<>();
            response.put("ipAddress", ipAddress);
            response.put("secureInput", ipSecurityService.isSecureInput(ipAddress));
            response.put("secureStorage", ipSecurityService.isSecureStorage(ipAddress));
            response.put("timestamp", LocalDateTime.now());
            
            // Try sanitization
            try {
                String sanitized = ipSecurityService.sanitizeIpAddress(ipAddress);
                response.put("sanitized", sanitized);
                response.put("sanitizationSuccess", true);
            } catch (Exception e) {
                response.put("sanitizationSuccess", false);
                response.put("sanitizationError", e.getMessage());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to validate IP security", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get IP security statistics and monitoring data.
     * Requirements: 5.3
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getSecurityStatistics(
            @RequestParam(defaultValue = "24") int timeWindowHours) {
        
        try {
            LocalDateTime startTime = LocalDateTime.now().minusHours(timeWindowHours);
            LocalDateTime endTime = LocalDateTime.now();
            
            List<IpAddressLog> logs = ipSecurityService.getSecurityAuditLogs(null, startTime, endTime);
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("timeWindowHours", timeWindowHours);
            statistics.put("totalLogs", logs.size());
            statistics.put("startTime", startTime);
            statistics.put("endTime", endTime);
            
            // Count by action type
            Map<String, Long> actionCounts = new HashMap<>();
            logs.forEach(log -> {
                String action = log.getAction().toString();
                actionCounts.put(action, actionCounts.getOrDefault(action, 0L) + 1);
            });
            statistics.put("actionCounts", actionCounts);
            
            // Count unique users
            long uniqueUsers = logs.stream()
                .mapToLong(IpAddressLog::getUserId)
                .distinct()
                .count();
            statistics.put("uniqueUsers", uniqueUsers);
            
            // Count unique admin users
            long uniqueAdmins = logs.stream()
                .filter(log -> log.getAdminUserId() != null)
                .mapToLong(IpAddressLog::getAdminUserId)
                .distinct()
                .count();
            statistics.put("uniqueAdmins", uniqueAdmins);
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Failed to get security statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}