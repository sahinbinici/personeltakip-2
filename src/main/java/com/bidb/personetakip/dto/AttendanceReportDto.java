package com.bidb.personetakip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceReportDto {
    private String period; // WEEK, MONTH, YEAR
    private String departmentCode;
    private String departmentName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    // Summary statistics
    private int totalPersonnel;
    private int personnelWithEntries;
    private int personnelWithoutEntries;
    private int lateEntries;
    private int earlyExits;
    
    // Detailed lists
    private List<PersonnelAttendanceDto> personnelWithEntriesList;
    private List<PersonnelAttendanceDto> personnelWithoutEntriesList;
    private List<PersonnelAttendanceDto> lateEntriesList;
    private List<PersonnelAttendanceDto> earlyExitsList;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonnelAttendanceDto {
        private Long userId;
        private String tcNo;
        private String fullName;
        private String personnelNo;
        private String departmentName;
        private int entryCount;
        private int exitCount;
        private int lateEntryCount;
        private int earlyExitCount;
        private LocalDateTime lastEntryTime;
        private LocalDateTime lastExitTime;
    }
}
