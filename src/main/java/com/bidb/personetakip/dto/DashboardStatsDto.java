package com.bidb.personetakip.dto;

import com.bidb.personetakip.model.AdminAuditLog;
import com.bidb.personetakip.model.EntryExitRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for admin dashboard statistics.
 * Contains aggregated data for dashboard display.
 * 
 * Requirements: 1.2 - Dashboard statistics display
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDto {
    
    /**
     * Total number of registered users
     */
    private long totalUsers;
    
    /**
     * Number of entry records today
     */
    private long todayEntryCount;
    
    /**
     * Number of exit records today
     */
    private long todayExitCount;
    
    /**
     * Total activity count today (entries + exits)
     */
    private long todayTotalActivity;
    
    /**
     * Recent entry/exit records (last 24 hours, max 10)
     */
    private List<EntryExitRecord> recentRecords;
    
    /**
     * Recent admin audit logs (last 24 hours)
     */
    private List<AdminAuditLog> recentAuditLogs;
    
    /**
     * Timestamp when statistics were last updated
     */
    private LocalDateTime lastUpdated;
}