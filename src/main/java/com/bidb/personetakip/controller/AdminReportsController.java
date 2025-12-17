package com.bidb.personetakip.controller;

import com.bidb.personetakip.dto.IpComplianceReportDto;
import com.bidb.personetakip.service.AdminReportsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST controller for admin IP compliance reporting operations.
 * Provides endpoints for IP compliance statistics, mismatch detection, and dashboard views.
 * 
 * Requirements: 4.3 - IP compliance statistics in reports
 */
@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminReportsController {
    
    @Autowired
    private AdminReportsService adminReportsService;
    
    /**
     * Get IP compliance report for a specific date range.
     * 
     * @param startDate Start date (format: yyyy-MM-dd)
     * @param endDate End date (format: yyyy-MM-dd)
     * @return IP compliance report
     * Requirements: 4.3 - IP compliance statistics in reports
     */
    @GetMapping("/ip-compliance")
    public ResponseEntity<IpComplianceReportDto> getIpComplianceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        IpComplianceReportDto report = adminReportsService.generateIpComplianceReport(startDate, endDate);
        return ResponseEntity.ok(report);
    }
    
    /**
     * Get IP compliance dashboard statistics for today.
     * 
     * @return IP compliance report for today
     * Requirements: 4.3 - IP compliance statistics in reports
     */
    @GetMapping("/ip-compliance/today")
    public ResponseEntity<IpComplianceReportDto> getTodayIpComplianceStats() {
        IpComplianceReportDto report = adminReportsService.getTodayIpComplianceStats();
        return ResponseEntity.ok(report);
    }
    
    /**
     * Get IP compliance dashboard statistics for this week.
     * 
     * @return IP compliance report for this week
     * Requirements: 4.3 - IP compliance statistics in reports
     */
    @GetMapping("/ip-compliance/weekly")
    public ResponseEntity<IpComplianceReportDto> getWeeklyIpComplianceStats() {
        IpComplianceReportDto report = adminReportsService.getWeeklyIpComplianceStats();
        return ResponseEntity.ok(report);
    }
    
    /**
     * Get IP compliance dashboard statistics for this month.
     * 
     * @return IP compliance report for this month
     * Requirements: 4.3 - IP compliance statistics in reports
     */
    @GetMapping("/ip-compliance/monthly")
    public ResponseEntity<IpComplianceReportDto> getMonthlyIpComplianceStats() {
        IpComplianceReportDto report = adminReportsService.getMonthlyIpComplianceStats();
        return ResponseEntity.ok(report);
    }
    
    /**
     * Get IP compliance dashboard view.
     * This endpoint provides a summary view suitable for dashboard display.
     * 
     * @return IP compliance dashboard data
     * Requirements: 4.3 - IP compliance dashboard views
     */
    @GetMapping("/ip-compliance/dashboard")
    public ResponseEntity<IpComplianceDashboardDto> getIpComplianceDashboard() {
        IpComplianceReportDto todayReport = adminReportsService.getTodayIpComplianceStats();
        IpComplianceReportDto weeklyReport = adminReportsService.getWeeklyIpComplianceStats();
        IpComplianceReportDto monthlyReport = adminReportsService.getMonthlyIpComplianceStats();
        
        IpComplianceDashboardDto dashboard = IpComplianceDashboardDto.builder()
                .todayStats(todayReport)
                .weeklyStats(weeklyReport)
                .monthlyStats(monthlyReport)
                .build();
        
        return ResponseEntity.ok(dashboard);
    }
    
    /**
     * DTO for IP compliance dashboard data
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class IpComplianceDashboardDto {
        private IpComplianceReportDto todayStats;
        private IpComplianceReportDto weeklyStats;
        private IpComplianceReportDto monthlyStats;
    }
}