package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.DashboardStatsDto;
import com.bidb.personetakip.model.AdminAuditLog;
import com.bidb.personetakip.model.EntryExitRecord;
import com.bidb.personetakip.model.EntryExitType;
import com.bidb.personetakip.model.User;
import com.bidb.personetakip.model.UserRole;
import com.bidb.personetakip.repository.AdminAuditLogRepository;
import com.bidb.personetakip.repository.EntryExitRecordRepository;
import com.bidb.personetakip.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for admin dashboard operations.
 * Provides aggregated statistics and recent activity data.
 * 
 * Requirements: 1.2 - Dashboard statistics and summary data
 */
@Service
public class AdminDashboardService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EntryExitRecordRepository entryExitRecordRepository;
    
    @Autowired
    private AdminAuditLogRepository adminAuditLogRepository;
    
    /**
     * Get dashboard statistics with caching (30 seconds).
     * 
     * @return Dashboard statistics DTO
     * Requirements: 1.2 - Display total user count, today's entry/exit count, and recent activity summary
     */
    @Cacheable(value = "dashboardStats", unless = "#result == null")
    public DashboardStatsDto getDashboardStats() {
        return getDashboardStatsForDepartments(null);
    }
    
    /**
     * Get dashboard statistics with department filtering.
     * 
     * @param authentication Authentication object for department filtering
     * @return Dashboard statistics DTO
     * Requirements: 1.2 - Display total user count, today's entry/exit count, and recent activity summary with department filtering
     */
    public DashboardStatsDto getDashboardStats(Authentication authentication) {
        List<String> accessibleDepartments = getAccessibleDepartmentCodes(authentication);
        return getDashboardStatsForDepartments(accessibleDepartments);
    }
    
    /**
     * Get accessible department codes for the authenticated user.
     * SUPER_ADMIN and ADMIN can access all departments.
     * DEPARTMENT_ADMIN can only access their own department.
     * 
     * @param authentication Authentication object
     * @return List of accessible department codes (null means all departments)
     */
    private List<String> getAccessibleDepartmentCodes(Authentication authentication) {
        if (authentication == null) {
            return null; // Allow all departments if no authentication
        }
        
        // Check if user has SUPER_ADMIN or ADMIN role
        boolean isSuperAdminOrAdmin = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .anyMatch(role -> role.equals("ROLE_SUPER_ADMIN") || role.equals("ROLE_ADMIN"));
        
        if (isSuperAdminOrAdmin) {
            return null; // Can access all departments
        }
        
        // Check if user has DEPARTMENT_ADMIN role
        boolean isDepartmentAdmin = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .anyMatch(role -> role.equals("ROLE_DEPARTMENT_ADMIN"));
        
        if (isDepartmentAdmin) {
            // Get user's department from database
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByTcNo(username);
            if (userOpt.isPresent()) {
                String departmentCode = userOpt.get().getDepartmentCode();
                if (departmentCode != null && !departmentCode.isEmpty()) {
                    return List.of(departmentCode);
                }
            }
        }
        
        return List.of(); // No accessible departments
    }
    
    /**
     * Get dashboard statistics for specific departments (for department admins).
     * 
     * @param departmentCodes List of department codes to filter by (null for all departments)
     * @return Dashboard statistics DTO
     */
    public DashboardStatsDto getDashboardStatsForDepartments(List<String> departmentCodes) {
        try {
            // Get total user count
            long totalUsers;
            if (departmentCodes != null && !departmentCodes.isEmpty()) {
                totalUsers = userRepository.countByDepartmentCodeIn(departmentCodes);
            } else {
                totalUsers = userRepository.count();
            }
            
            // Get today's entry/exit activity count
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
            
            long todayEntryCount = 0;
            long todayExitCount = 0;
            List<EntryExitRecord> recentRecords = List.of();
            List<AdminAuditLog> recentAuditLogs = List.of();
            
            try {
                if (departmentCodes != null && !departmentCodes.isEmpty()) {
                    todayEntryCount = entryExitRecordRepository.countByTypeAndTimestampBetweenAndUserDepartmentCodes(
                        EntryExitType.ENTRY, startOfDay, endOfDay, departmentCodes);
                    todayExitCount = entryExitRecordRepository.countByTypeAndTimestampBetweenAndUserDepartmentCodes(
                        EntryExitType.EXIT, startOfDay, endOfDay, departmentCodes);
                    
                    // Get recent activity (last 24 hours) for specific departments
                    LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
                    recentRecords = entryExitRecordRepository.findTop10ByTimestampAfterAndUserDepartmentCodesOrderByTimestampDesc(
                        last24Hours, departmentCodes);
                } else {
                    todayEntryCount = entryExitRecordRepository.countByTypeAndTimestampBetween(EntryExitType.ENTRY, startOfDay, endOfDay);
                    todayExitCount = entryExitRecordRepository.countByTypeAndTimestampBetween(EntryExitType.EXIT, startOfDay, endOfDay);
                    
                    // Get recent activity (last 24 hours)
                    LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
                    recentRecords = entryExitRecordRepository.findTop10ByTimestampAfterOrderByTimestampDesc(last24Hours);
                }
            } catch (Exception e) {
                // Log error but continue with empty data
                System.err.println("Error fetching entry/exit records: " + e.getMessage());
            }
            
            try {
                // Get recent admin actions (last 24 hours)
                LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
                recentAuditLogs = adminAuditLogRepository.findRecentLogs(last24Hours);
            } catch (Exception e) {
                // Log error but continue with empty data
                System.err.println("Error fetching audit logs: " + e.getMessage());
            }
            
            long todayTotalActivity = todayEntryCount + todayExitCount;
            
            return DashboardStatsDto.builder()
                    .totalUsers(totalUsers)
                    .todayEntryCount(todayEntryCount)
                    .todayExitCount(todayExitCount)
                    .todayTotalActivity(todayTotalActivity)
                    .recentRecords(recentRecords)
                    .recentAuditLogs(recentAuditLogs)
                    .lastUpdated(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            // Return empty stats if everything fails
            System.err.println("Error in getDashboardStats: " + e.getMessage());
            return DashboardStatsDto.builder()
                    .totalUsers(0)
                    .todayEntryCount(0)
                    .todayExitCount(0)
                    .todayTotalActivity(0)
                    .recentRecords(List.of())
                    .recentAuditLogs(List.of())
                    .lastUpdated(LocalDateTime.now())
                    .build();
        }
    }
    
    /**
     * Get user count by role.
     * 
     * @param role User role to count
     * @return Number of users with the specified role
     */
    public long getUserCountByRole(String role) {
        UserRole userRole = UserRole.valueOf(role);
        return userRepository.countByRole(userRole);
    }
    
    /**
     * Get entry/exit activity count for a specific date range.
     * 
     * @param startDate Start date
     * @param endDate End date
     * @return Total activity count
     */
    public long getActivityCountBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return entryExitRecordRepository.countByTimestampBetween(startDate, endDate);
    }
}