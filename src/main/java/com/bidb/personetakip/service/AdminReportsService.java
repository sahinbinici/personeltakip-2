package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.IpComplianceReportDto;
import com.bidb.personetakip.model.EntryExitRecord;
import com.bidb.personetakip.model.User;
import com.bidb.personetakip.repository.EntryExitRecordRepository;
import com.bidb.personetakip.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for admin IP compliance reporting operations.
 * Provides IP compliance statistics, mismatch detection, and reporting functionality.
 * 
 * Requirements: 4.3 - IP compliance statistics in reports
 */
@Service
public class AdminReportsService {
    
    @Autowired
    private EntryExitRecordRepository entryExitRecordRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private IpComplianceService ipComplianceService;
    
    /**
     * Generate IP compliance report for a date range.
     * 
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return IP compliance report
     * Requirements: 4.3 - IP compliance statistics in reports
     */
    public IpComplianceReportDto generateIpComplianceReport(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        // Get all records in the date range
        List<EntryExitRecord> records = entryExitRecordRepository.findByTimestampBetween(startDateTime, endDateTime);
        
        // Calculate compliance statistics
        long totalRecords = records.size();
        long matchingRecords = 0;
        long mismatchRecords = 0;
        long noAssignmentRecords = 0;
        long unknownIpRecords = 0;
        
        Map<Long, User> userCache = new HashMap<>();
        Map<Long, List<String>> userMismatchIps = new HashMap<>();
        Map<String, IpUsageStats> ipUsageMap = new HashMap<>();
        Map<String, DepartmentStats> departmentStatsMap = new HashMap<>();
        
        for (EntryExitRecord record : records) {
            User user = userCache.computeIfAbsent(record.getUserId(), 
                    id -> userRepository.findById(id).orElse(null));
            
            if (user == null) {
                continue;
            }
            
            IpComplianceService.IpComplianceStatus status = ipComplianceService.getIpComplianceStatus(record, user);
            
            switch (status) {
                case MATCH:
                    matchingRecords++;
                    break;
                case MISMATCH:
                    mismatchRecords++;
                    userMismatchIps.computeIfAbsent(user.getId(), k -> new ArrayList<>())
                            .add(record.getIpAddress());
                    break;
                case NO_ASSIGNMENT:
                    noAssignmentRecords++;
                    break;
                case UNKNOWN_IP:
                    unknownIpRecords++;
                    break;
            }
            
            // Track IP usage
            String ipAddress = record.getIpAddress() != null ? record.getIpAddress() : "Unknown";
            IpUsageStats ipStats = ipUsageMap.computeIfAbsent(ipAddress, k -> new IpUsageStats());
            ipStats.usageCount++;
            ipStats.userIds.add(user.getId());
            ipStats.userNames.add(user.getFirstName() + " " + user.getLastName());
            
            // Track department statistics
            String deptKey = user.getDepartmentCode() != null ? user.getDepartmentCode() : "UNKNOWN";
            DepartmentStats deptStats = departmentStatsMap.computeIfAbsent(deptKey, k -> new DepartmentStats());
            deptStats.departmentCode = user.getDepartmentCode();
            deptStats.departmentName = user.getDepartmentName();
            deptStats.totalRecords++;
            
            switch (status) {
                case MATCH:
                    deptStats.matchingRecords++;
                    break;
                case MISMATCH:
                    deptStats.mismatchRecords++;
                    break;
                case NO_ASSIGNMENT:
                    deptStats.noAssignmentRecords++;
                    break;
                case UNKNOWN_IP:
                    deptStats.unknownIpRecords++;
                    break;
            }
        }
        
        // Calculate compliance percentage
        long recordsWithAssignments = matchingRecords + mismatchRecords;
        double compliancePercentage = recordsWithAssignments > 0 ? 
                (double) matchingRecords / recordsWithAssignments * 100.0 : 0.0;
        
        // Build user mismatch list
        List<IpComplianceReportDto.UserIpMismatchDto> userMismatches = userMismatchIps.entrySet().stream()
                .map(entry -> {
                    User user = userCache.get(entry.getKey());
                    if (user == null) return null;
                    
                    return IpComplianceReportDto.UserIpMismatchDto.builder()
                            .userId(user.getId())
                            .userFullName(user.getFirstName() + " " + user.getLastName())
                            .userPersonnelNo(user.getPersonnelNo())
                            .userDepartmentName(user.getDepartmentName())
                            .assignedIpAddresses(user.getAssignedIpAddresses())
                            .mismatchCount(entry.getValue().size())
                            .actualIpAddresses(entry.getValue().stream().distinct().collect(Collectors.toList()))
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> Long.compare(b.getMismatchCount(), a.getMismatchCount()))
                .collect(Collectors.toList());
        
        // Build top IP addresses list
        List<IpComplianceReportDto.IpUsageDto> topIpAddresses = ipUsageMap.entrySet().stream()
                .map(entry -> IpComplianceReportDto.IpUsageDto.builder()
                        .ipAddress(entry.getKey())
                        .usageCount(entry.getValue().usageCount)
                        .uniqueUsers(entry.getValue().userIds.size())
                        .userNames(entry.getValue().userNames.stream().distinct()
                                .limit(5).collect(Collectors.toList()))
                        .build())
                .sorted((a, b) -> Long.compare(b.getUsageCount(), a.getUsageCount()))
                .limit(10)
                .collect(Collectors.toList());
        
        // Build department statistics
        List<IpComplianceReportDto.DepartmentIpComplianceDto> departmentStats = departmentStatsMap.values().stream()
                .map(stats -> {
                    long deptRecordsWithAssignments = stats.matchingRecords + stats.mismatchRecords;
                    double deptCompliancePercentage = deptRecordsWithAssignments > 0 ? 
                            (double) stats.matchingRecords / deptRecordsWithAssignments * 100.0 : 0.0;
                    
                    return IpComplianceReportDto.DepartmentIpComplianceDto.builder()
                            .departmentCode(stats.departmentCode)
                            .departmentName(stats.departmentName != null ? stats.departmentName : stats.departmentCode)
                            .totalRecords(stats.totalRecords)
                            .matchingRecords(stats.matchingRecords)
                            .mismatchRecords(stats.mismatchRecords)
                            .noAssignmentRecords(stats.noAssignmentRecords)
                            .unknownIpRecords(stats.unknownIpRecords)
                            .compliancePercentage(deptCompliancePercentage)
                            .build();
                })
                .sorted((a, b) -> a.getDepartmentName().compareTo(b.getDepartmentName()))
                .collect(Collectors.toList());
        
        return IpComplianceReportDto.builder()
                .reportDate(LocalDate.now())
                .startDate(startDate)
                .endDate(endDate)
                .totalRecords(totalRecords)
                .matchingRecords(matchingRecords)
                .mismatchRecords(mismatchRecords)
                .noAssignmentRecords(noAssignmentRecords)
                .unknownIpRecords(unknownIpRecords)
                .compliancePercentage(compliancePercentage)
                .userMismatches(userMismatches)
                .topIpAddresses(topIpAddresses)
                .departmentStats(departmentStats)
                .build();
    }
    
    /**
     * Get IP compliance dashboard statistics for today.
     * 
     * @return IP compliance report for today
     * Requirements: 4.3 - IP compliance statistics in reports
     */
    public IpComplianceReportDto getTodayIpComplianceStats() {
        LocalDate today = LocalDate.now();
        return generateIpComplianceReport(today, today);
    }
    
    /**
     * Get IP compliance dashboard statistics for this week.
     * 
     * @return IP compliance report for this week
     * Requirements: 4.3 - IP compliance statistics in reports
     */
    public IpComplianceReportDto getWeeklyIpComplianceStats() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        return generateIpComplianceReport(startOfWeek, today);
    }
    
    /**
     * Get IP compliance dashboard statistics for this month.
     * 
     * @return IP compliance report for this month
     * Requirements: 4.3 - IP compliance statistics in reports
     */
    public IpComplianceReportDto getMonthlyIpComplianceStats() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        return generateIpComplianceReport(startOfMonth, today);
    }
    
    /**
     * Helper class for tracking IP usage statistics
     */
    private static class IpUsageStats {
        long usageCount = 0;
        Set<Long> userIds = new HashSet<>();
        List<String> userNames = new ArrayList<>();
    }
    
    /**
     * Helper class for tracking department statistics
     */
    private static class DepartmentStats {
        String departmentCode;
        String departmentName;
        long totalRecords = 0;
        long matchingRecords = 0;
        long mismatchRecords = 0;
        long noAssignmentRecords = 0;
        long unknownIpRecords = 0;
    }
}