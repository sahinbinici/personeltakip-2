package com.bidb.personetakip.controller;

import com.bidb.personetakip.dto.DashboardStatsDto;
import com.bidb.personetakip.service.AdminDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for admin dashboard API endpoints.
 * Provides dashboard statistics and summary data.
 * 
 * Requirements: 1.2, 4.4 - Dashboard statistics with JWT role validation
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminDashboardController {
    
    @Autowired
    private AdminDashboardService adminDashboardService;
    
    /**
     * Get dashboard statistics.
     * 
     * @return Dashboard statistics DTO
     * Requirements: 1.2 - Display total user count, today's entry/exit count, and recent activity summary
     *               4.4 - JWT role validation for all admin endpoints
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        DashboardStatsDto stats = adminDashboardService.getDashboardStats();
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
}