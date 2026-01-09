package com.bidb.personetakip.controller;

import com.bidb.personetakip.dto.DashboardStatsDto;
import com.bidb.personetakip.service.AdminDashboardService;
import com.bidb.personetakip.service.IpTrackingInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for admin dashboard API endpoints.
 * Provides dashboard statistics and summary data.
 * 
 * Requirements: 1.2, 4.4 - Dashboard statistics with JWT role validation
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN') or hasRole('DEPARTMENT_ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminDashboardController {
    
    @Autowired
    private AdminDashboardService adminDashboardService;
    
    @Autowired
    private IpTrackingInformationService ipTrackingInformationService;
    
    /**
     * Get dashboard statistics.
     * 
     * @param authentication Authentication object for department filtering
     * @return Dashboard statistics DTO
     * Requirements: 1.2 - Display total user count, today's entry/exit count, and recent activity summary
     *               4.4 - JWT role validation for all admin endpoints
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getDashboardStats(Authentication authentication) {
        DashboardStatsDto stats = adminDashboardService.getDashboardStats(authentication);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get user count by role.
     * 
     * @param role User role to count
     * @return User count
     */
    @GetMapping("/users/count")
    public ResponseEntity<Long> getUserCountByRole(String role) {
        long count = adminDashboardService.getUserCountByRole(role);
        return ResponseEntity.ok(count);
    }
    
    /**
     * Get IP tracking information for dashboard display.
     * 
     * @return IP tracking information and status indicators
     * Requirements: 6.4 - Clear IP tracking information display
     */
    @GetMapping("/ip-tracking/info")
    public ResponseEntity<Map<String, Object>> getIpTrackingInfo() {
        Map<String, Object> info = new HashMap<>();
        
        info.put("enabled", ipTrackingInformationService.isIpTrackingEnabled());
        info.put("privacyEnabled", ipTrackingInformationService.isPrivacyModeEnabled());
        info.put("anonymizeReports", ipTrackingInformationService.isReportAnonymizationEnabled());
        info.put("statusDisplay", ipTrackingInformationService.getIpTrackingStatusDisplay());
        info.put("privacyInfo", ipTrackingInformationService.getPrivacyInformationDisplay());
        info.put("notice", ipTrackingInformationService.getIpTrackingNotice());
        info.put("statusIndicator", ipTrackingInformationService.getStatusIndicator());
        info.put("privacyIndicator", ipTrackingInformationService.getPrivacyStatusIndicator());
        info.put("helpText", ipTrackingInformationService.getIpAssignmentHelpText());
        
        return ResponseEntity.ok(info);
    }
}