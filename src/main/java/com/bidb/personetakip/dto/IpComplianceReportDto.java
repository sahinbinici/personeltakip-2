package com.bidb.personetakip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for IP compliance report information.
 * Contains IP compliance statistics and mismatch details for admin reporting.
 * 
 * Requirements: 4.3 - IP compliance statistics in reports
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpComplianceReportDto {
    
    /**
     * Report generation date
     */
    private LocalDate reportDate;
    
    /**
     * Date range for the report
     */
    private LocalDate startDate;
    private LocalDate endDate;
    
    /**
     * Total number of entry/exit records in the period
     */
    private long totalRecords;
    
    /**
     * Number of records with IP address matches
     */
    private long matchingRecords;
    
    /**
     * Number of records with IP address mismatches
     */
    private long mismatchRecords;
    
    /**
     * Number of records from users with no IP assignment
     */
    private long noAssignmentRecords;
    
    /**
     * Number of records with unknown IP addresses
     */
    private long unknownIpRecords;
    
    /**
     * IP compliance percentage (matching / total with assignments)
     */
    private double compliancePercentage;
    
    /**
     * List of users with IP mismatches
     */
    private List<UserIpMismatchDto> userMismatches;
    
    /**
     * List of most frequently used IP addresses
     */
    private List<IpUsageDto> topIpAddresses;
    
    /**
     * Department-wise IP compliance statistics
     */
    private List<DepartmentIpComplianceDto> departmentStats;
    
    /**
     * DTO for user IP mismatch information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserIpMismatchDto {
        private Long userId;
        private String userFullName;
        private String userPersonnelNo;
        private String userDepartmentName;
        private String assignedIpAddresses;
        private long mismatchCount;
        private List<String> actualIpAddresses;
    }
    
    /**
     * DTO for IP address usage statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IpUsageDto {
        private String ipAddress;
        private long usageCount;
        private long uniqueUsers;
        private List<String> userNames;
    }
    
    /**
     * DTO for department IP compliance statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentIpComplianceDto {
        private String departmentCode;
        private String departmentName;
        private long totalRecords;
        private long matchingRecords;
        private long mismatchRecords;
        private long noAssignmentRecords;
        private long unknownIpRecords;
        private double compliancePercentage;
    }
}