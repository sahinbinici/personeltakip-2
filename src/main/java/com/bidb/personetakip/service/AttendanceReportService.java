package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.AttendanceReportDto;
import com.bidb.personetakip.dto.AttendanceReportDto.PersonnelAttendanceDto;
import com.bidb.personetakip.model.EntryExitRecord;
import com.bidb.personetakip.model.EntryExitType;
import com.bidb.personetakip.model.User;
import com.bidb.personetakip.repository.EntryExitRecordRepository;
import com.bidb.personetakip.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendanceReportService {
    
    private final EntryExitRecordRepository entryExitRecordRepository;
    private final UserRepository userRepository;
    
    @Value("${attendance.late-entry-time:09:00}")
    private String lateEntryTime;
    
    @Value("${attendance.early-exit-time:17:00}")
    private String earlyExitTime;
    
    public AttendanceReportService(EntryExitRecordRepository entryExitRecordRepository,
                                   UserRepository userRepository) {
        this.entryExitRecordRepository = entryExitRecordRepository;
        this.userRepository = userRepository;
    }
    
    public AttendanceReportDto generateReport(String period, String departmentCode) {
        LocalDateTime[] dateRange = getDateRange(period);
        LocalDateTime startDate = dateRange[0];
        LocalDateTime endDate = dateRange[1];
        
        return generateReport(startDate, endDate, departmentCode, period);
    }
    
    public AttendanceReportDto generateReport(LocalDateTime startDate, LocalDateTime endDate, 
                                               String departmentCode, String period) {
        // Get all users (optionally filtered by department)
        List<User> allUsers;
        if (departmentCode != null && !departmentCode.isEmpty()) {
            Pageable pageable = PageRequest.of(0, 10000);
            allUsers = userRepository.findByDepartmentCode(departmentCode, pageable).getContent();
        } else {
            allUsers = userRepository.findAll();
        }
        
        // Get all records in date range
        List<EntryExitRecord> records = entryExitRecordRepository
                .findByTimestampBetween(startDate, endDate);
        
        // Filter by department if specified
        if (departmentCode != null && !departmentCode.isEmpty()) {
            Set<Long> departmentUserIds = allUsers.stream()
                    .map(User::getId)
                    .collect(Collectors.toSet());
            records = records.stream()
                    .filter(r -> departmentUserIds.contains(r.getUserId()))
                    .collect(Collectors.toList());
        }
        
        // Group records by user
        Map<Long, List<EntryExitRecord>> recordsByUser = records.stream()
                .collect(Collectors.groupingBy(EntryExitRecord::getUserId));
        
        // Parse time thresholds
        LocalTime lateThreshold = LocalTime.parse(lateEntryTime);
        LocalTime earlyThreshold = LocalTime.parse(earlyExitTime);
        
        // Build user map for quick lookup
        Map<Long, User> userMap = allUsers.stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        
        // Calculate statistics
        List<PersonnelAttendanceDto> withEntries = new ArrayList<>();
        List<PersonnelAttendanceDto> withoutEntries = new ArrayList<>();
        List<PersonnelAttendanceDto> lateEntries = new ArrayList<>();
        List<PersonnelAttendanceDto> earlyExits = new ArrayList<>();
        
        for (User user : allUsers) {
            List<EntryExitRecord> userRecords = recordsByUser.getOrDefault(user.getId(), Collections.emptyList());
            
            if (userRecords.isEmpty()) {
                withoutEntries.add(buildPersonnelDto(user, userRecords, lateThreshold, earlyThreshold));
            } else {
                PersonnelAttendanceDto dto = buildPersonnelDto(user, userRecords, lateThreshold, earlyThreshold);
                withEntries.add(dto);
                
                if (dto.getLateEntryCount() > 0) {
                    lateEntries.add(dto);
                }
                if (dto.getEarlyExitCount() > 0) {
                    earlyExits.add(dto);
                }
            }
        }
        
        // Sort lists
        withEntries.sort(Comparator.comparing(PersonnelAttendanceDto::getFullName));
        withoutEntries.sort(Comparator.comparing(PersonnelAttendanceDto::getFullName));
        lateEntries.sort(Comparator.comparingInt(PersonnelAttendanceDto::getLateEntryCount).reversed());
        earlyExits.sort(Comparator.comparingInt(PersonnelAttendanceDto::getEarlyExitCount).reversed());
        
        String deptName = departmentCode != null && !departmentCode.isEmpty() 
                ? allUsers.stream().findFirst().map(User::getDepartmentName).orElse(departmentCode)
                : "Tüm Departmanlar";
        
        return AttendanceReportDto.builder()
                .period(period)
                .departmentCode(departmentCode)
                .departmentName(deptName)
                .startDate(startDate)
                .endDate(endDate)
                .totalPersonnel(allUsers.size())
                .personnelWithEntries(withEntries.size())
                .personnelWithoutEntries(withoutEntries.size())
                .lateEntries(lateEntries.size())
                .earlyExits(earlyExits.size())
                .personnelWithEntriesList(withEntries)
                .personnelWithoutEntriesList(withoutEntries)
                .lateEntriesList(lateEntries)
                .earlyExitsList(earlyExits)
                .build();
    }
    
    private PersonnelAttendanceDto buildPersonnelDto(User user, List<EntryExitRecord> records,
                                                      LocalTime lateThreshold, LocalTime earlyThreshold) {
        int entryCount = 0;
        int exitCount = 0;
        int lateEntryCount = 0;
        int earlyExitCount = 0;
        LocalDateTime lastEntry = null;
        LocalDateTime lastExit = null;
        
        for (EntryExitRecord record : records) {
            if (record.getType() == EntryExitType.ENTRY) {
                entryCount++;
                if (lastEntry == null || record.getTimestamp().isAfter(lastEntry)) {
                    lastEntry = record.getTimestamp();
                }
                // Check if late (after threshold time)
                if (record.getTimestamp().toLocalTime().isAfter(lateThreshold)) {
                    lateEntryCount++;
                }
            } else {
                exitCount++;
                if (lastExit == null || record.getTimestamp().isAfter(lastExit)) {
                    lastExit = record.getTimestamp();
                }
                // Check if early exit (before threshold time)
                if (record.getTimestamp().toLocalTime().isBefore(earlyThreshold)) {
                    earlyExitCount++;
                }
            }
        }
        
        return PersonnelAttendanceDto.builder()
                .userId(user.getId())
                .tcNo(user.getTcNo())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .personnelNo(user.getPersonnelNo())
                .departmentName(user.getDepartmentName())
                .entryCount(entryCount)
                .exitCount(exitCount)
                .lateEntryCount(lateEntryCount)
                .earlyExitCount(earlyExitCount)
                .lastEntryTime(lastEntry)
                .lastExitTime(lastExit)
                .build();
    }
    
    private LocalDateTime[] getDateRange(String period) {
        LocalDate today = LocalDate.now();
        LocalDateTime endDate = today.atTime(23, 59, 59);
        LocalDateTime startDate;
        
        switch (period.toUpperCase()) {
            case "WEEK":
                startDate = today.minusWeeks(1).atStartOfDay();
                break;
            case "MONTH":
                startDate = today.minusMonths(1).atStartOfDay();
                break;
            case "YEAR":
                startDate = today.minusYears(1).atStartOfDay();
                break;
            default:
                startDate = today.minusMonths(1).atStartOfDay();
        }
        
        return new LocalDateTime[]{startDate, endDate};
    }
    
    public List<Map<String, String>> getDepartments() {
        return userRepository.findAll().stream()
                .filter(u -> u.getDepartmentCode() != null)
                .collect(Collectors.groupingBy(User::getDepartmentCode))
                .entrySet().stream()
                .map(e -> {
                    Map<String, String> dept = new HashMap<>();
                    dept.put("code", e.getKey());
                    dept.put("name", e.getValue().get(0).getDepartmentName());
                    return dept;
                })
                .sorted(Comparator.comparing(m -> m.get("name")))
                .collect(Collectors.toList());
    }
}
