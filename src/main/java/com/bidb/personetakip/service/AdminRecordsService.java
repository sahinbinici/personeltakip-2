package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.AdminRecordDto;
import com.bidb.personetakip.model.EntryExitRecord;
import com.bidb.personetakip.model.EntryExitType;
import com.bidb.personetakip.model.User;
import com.bidb.personetakip.repository.EntryExitRecordRepository;
import com.bidb.personetakip.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for admin entry/exit records management operations.
 * Provides record listing, filtering, and export functionality.
 * 
 * Requirements: 3.1, 3.3, 3.4, 3.5, 3.6 - Entry/exit records management
 */
@Service
public class AdminRecordsService {
    
    @Autowired
    private EntryExitRecordRepository entryExitRecordRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private IpComplianceService ipComplianceService;
    
    /**
     * Get paginated list of all entry/exit records.
     * 
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of AdminRecordDto objects
     * Requirements: 3.1 - Paginated record listing
     */
    public Page<AdminRecordDto> getAllRecords(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EntryExitRecord> records = entryExitRecordRepository.findAllByOrderByTimestampDesc(pageable);
        return records.map(this::convertToAdminRecordDto);
    }
    
    /**
     * Get entry/exit records filtered by date range.
     * 
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of AdminRecordDto objects within date range
     * Requirements: 3.3 - Date range filtering
     */
    public Page<AdminRecordDto> getRecordsByDateRange(LocalDate startDate, LocalDate endDate, int page, int size) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<EntryExitRecord> records = entryExitRecordRepository.findByTimestampBetweenOrderByTimestampDesc(
                startDateTime, endDateTime, pageable);
        return records.map(this::convertToAdminRecordDto);
    }
    
    /**
     * Get entry/exit records filtered by user.
     * 
     * @param userId User ID to filter by
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of AdminRecordDto objects for specified user
     * Requirements: 3.4 - User-specific record filtering
     */
    public Page<AdminRecordDto> getRecordsByUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EntryExitRecord> records = entryExitRecordRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
        return records.map(this::convertToAdminRecordDto);
    }
    
    /**
     * Get entry/exit records with combined filters.
     * 
     * @param startDate Start date (optional)
     * @param endDate End date (optional)
     * @param userId User ID (optional)
     * @param departmentCode Department code (optional)
     * @param ipAddress IP address filter (optional)
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of AdminRecordDto objects matching filters
     */
    public Page<AdminRecordDto> getRecordsWithFilters(LocalDate startDate, LocalDate endDate, 
                                                     Long userId, String departmentCode, String ipAddress, int page, int size) {
        List<EntryExitRecord> allRecords = entryExitRecordRepository.findAll();
        
        // Apply filters
        List<EntryExitRecord> filteredRecords = allRecords.stream()
                .filter(record -> {
                    // Date range filter
                    if (startDate != null && endDate != null) {
                        LocalDateTime startDateTime = startDate.atStartOfDay();
                        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
                        if (record.getTimestamp().isBefore(startDateTime) || 
                            record.getTimestamp().isAfter(endDateTime)) {
                            return false;
                        }
                    }
                    
                    // User filter
                    if (userId != null && !record.getUserId().equals(userId)) {
                        return false;
                    }
                    
                    // Department filter
                    if (departmentCode != null && !departmentCode.isEmpty()) {
                        Optional<User> userOpt = userRepository.findById(record.getUserId());
                        if (userOpt.isEmpty() || !departmentCode.equals(userOpt.get().getDepartmentCode())) {
                            return false;
                        }
                    }
                    
                    // IP address filter
                    if (ipAddress != null && !ipAddress.isEmpty()) {
                        if ("unknown".equalsIgnoreCase(ipAddress) || "null".equalsIgnoreCase(ipAddress)) {
                            // Filter for unknown/null IP addresses
                            if (record.getIpAddress() != null) {
                                return false;
                            }
                        } else {
                            // Filter for specific IP address or IP range
                            if (record.getIpAddress() == null) {
                                return false;
                            }
                            
                            // Support IP range filtering (e.g., "192.168.1." matches "192.168.1.100")
                            if (ipAddress.endsWith(".")) {
                                if (!record.getIpAddress().startsWith(ipAddress)) {
                                    return false;
                                }
                            } else {
                                // Exact IP match
                                if (!ipAddress.equals(record.getIpAddress())) {
                                    return false;
                                }
                            }
                        }
                    }
                    
                    return true;
                })
                .sorted((r1, r2) -> r2.getTimestamp().compareTo(r1.getTimestamp()))
                .collect(Collectors.toList());
        
        Pageable pageable = PageRequest.of(page, size);
        int start = page * size;
        int end = Math.min(start + size, filteredRecords.size());
        
        List<AdminRecordDto> pageContent = filteredRecords.subList(start, end)
                .stream()
                .map(this::convertToAdminRecordDto)
                .collect(Collectors.toList());
        
        return new PageImpl<>(pageContent, pageable, filteredRecords.size());
    }
    
    /**
     * Get entry/exit records with IP mismatch filtering.
     * 
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of AdminRecordDto objects with IP mismatches
     * Requirements: 4.4 - IP mismatch filtering
     */
    public Page<AdminRecordDto> getRecordsWithIpMismatch(int page, int size) {
        List<EntryExitRecord> allRecords = entryExitRecordRepository.findAll();
        
        // Filter for IP mismatches
        List<EntryExitRecord> mismatchRecords = allRecords.stream()
                .filter(record -> {
                    Optional<User> userOpt = userRepository.findById(record.getUserId());
                    return userOpt.isPresent() && ipComplianceService.hasIpMismatch(record, userOpt.get());
                })
                .sorted((r1, r2) -> r2.getTimestamp().compareTo(r1.getTimestamp()))
                .collect(Collectors.toList());
        
        Pageable pageable = PageRequest.of(page, size);
        int start = page * size;
        int end = Math.min(start + size, mismatchRecords.size());
        
        List<AdminRecordDto> pageContent = mismatchRecords.subList(start, end)
                .stream()
                .map(this::convertToAdminRecordDto)
                .collect(Collectors.toList());
        
        return new PageImpl<>(pageContent, pageable, mismatchRecords.size());
    }
    
    /**
     * Generate CSV export data for records.
     * 
     * @param startDate Start date (optional)
     * @param endDate End date (optional)
     * @param userId User ID (optional)
     * @param departmentCode Department code (optional)
     * @param ipAddress IP address filter (optional)
     * @return CSV content as string
     * Requirements: 3.5 - CSV export functionality
     */
    public String generateCsvExport(LocalDate startDate, LocalDate endDate, Long userId, String departmentCode, String ipAddress) {
        List<EntryExitRecord> records = entryExitRecordRepository.findAll();
        
        // Apply filters
        List<EntryExitRecord> filteredRecords = records.stream()
                .filter(record -> {
                    // Date range filter
                    if (startDate != null && endDate != null) {
                        LocalDateTime startDateTime = startDate.atStartOfDay();
                        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
                        if (record.getTimestamp().isBefore(startDateTime) || 
                            record.getTimestamp().isAfter(endDateTime)) {
                            return false;
                        }
                    }
                    
                    // User filter
                    if (userId != null && !record.getUserId().equals(userId)) {
                        return false;
                    }
                    
                    // Department filter
                    if (departmentCode != null && !departmentCode.isEmpty()) {
                        Optional<User> userOpt = userRepository.findById(record.getUserId());
                        if (userOpt.isEmpty() || !departmentCode.equals(userOpt.get().getDepartmentCode())) {
                            return false;
                        }
                    }
                    
                    // IP address filter
                    if (ipAddress != null && !ipAddress.isEmpty()) {
                        if ("unknown".equalsIgnoreCase(ipAddress) || "null".equalsIgnoreCase(ipAddress)) {
                            // Filter for unknown/null IP addresses
                            if (record.getIpAddress() != null) {
                                return false;
                            }
                        } else {
                            // Filter for specific IP address or IP range
                            if (record.getIpAddress() == null) {
                                return false;
                            }
                            
                            // Support IP range filtering (e.g., "192.168.1." matches "192.168.1.100")
                            if (ipAddress.endsWith(".")) {
                                if (!record.getIpAddress().startsWith(ipAddress)) {
                                    return false;
                                }
                            } else {
                                // Exact IP match
                                if (!ipAddress.equals(record.getIpAddress())) {
                                    return false;
                                }
                            }
                        }
                    }
                    
                    return true;
                })
                .sorted((r1, r2) -> r2.getTimestamp().compareTo(r1.getTimestamp()))
                .collect(Collectors.toList());
        
        StringBuilder csv = new StringBuilder();
        
        // CSV Header
        csv.append("Tarih,Saat,TC Kimlik No,Ad Soyad,Sicil No,Departman,Tür,QR Kod,IP Adresi,Enlem,Boylam\n");
        
        // CSV Data
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        
        for (EntryExitRecord record : filteredRecords) {
            AdminRecordDto dto = convertToAdminRecordDto(record);
            
            csv.append(record.getTimestamp().format(dateFormatter)).append(",");
            csv.append(record.getTimestamp().format(timeFormatter)).append(",");
            csv.append(dto.getUserTcNo() != null ? dto.getUserTcNo() : "").append(",");
            csv.append(dto.getUserFullName() != null ? "\"" + dto.getUserFullName() + "\"" : "").append(",");
            csv.append(dto.getUserPersonnelNo() != null ? dto.getUserPersonnelNo() : "").append(",");
            csv.append(dto.getUserDepartmentName() != null ? "\"" + dto.getUserDepartmentName() + "\"" : "").append(",");
            csv.append(getTypeDisplayName(record.getType().name())).append(",");
            csv.append(record.getQrCodeValue()).append(",");
            csv.append(record.getIpAddress() != null ? record.getIpAddress() : "Bilinmiyor").append(",");
            csv.append(record.getLatitude() != null ? record.getLatitude().toString() : "").append(",");
            csv.append(record.getLongitude() != null ? record.getLongitude().toString() : "").append("\n");
        }
        
        return csv.toString();
    }
    
    /**
     * Get daily summary statistics.
     * 
     * @param date Date to get statistics for
     * @return Daily summary statistics
     * Requirements: 3.6 - Daily summary statistics
     */
    public DailySummaryStats getDailySummaryStats(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        
        long totalRecords = entryExitRecordRepository.countByTimestampBetween(startOfDay, endOfDay);
        long entryCount = entryExitRecordRepository.countByTypeAndTimestampBetween(EntryExitType.ENTRY, startOfDay, endOfDay);
        long exitCount = entryExitRecordRepository.countByTypeAndTimestampBetween(EntryExitType.EXIT, startOfDay, endOfDay);
        
        // Get unique users count
        List<EntryExitRecord> dayRecords = entryExitRecordRepository.findAll()
                .stream()
                .filter(record -> !record.getTimestamp().isBefore(startOfDay) && 
                                !record.getTimestamp().isAfter(endOfDay))
                .collect(Collectors.toList());
        
        long uniqueUsers = dayRecords.stream()
                .map(EntryExitRecord::getUserId)
                .distinct()
                .count();
        
        return DailySummaryStats.builder()
                .date(date)
                .totalRecords(totalRecords)
                .entryCount(entryCount)
                .exitCount(exitCount)
                .uniqueUsers(uniqueUsers)
                .build();
    }
    
    /**
     * Get list of departments for filtering.
     * 
     * @return List of department codes and names
     */
    public List<Map<String, String>> getDepartments() {
        List<User> users = userRepository.findAll();
        
        return users.stream()
                .filter(user -> user.getDepartmentCode() != null && !user.getDepartmentCode().isEmpty())
                .collect(Collectors.groupingBy(User::getDepartmentCode))
                .entrySet().stream()
                .map(entry -> {
                    String departmentCode = entry.getKey();
                    String departmentName = entry.getValue().stream()
                            .map(User::getDepartmentName)
                            .filter(name -> name != null && !name.isEmpty())
                            .findFirst()
                            .orElse(departmentCode);
                    
                    Map<String, String> department = new HashMap<>();
                    department.put("code", departmentCode);
                    department.put("name", departmentName);
                    return department;
                })
                .sorted((d1, d2) -> d1.get("name").compareTo(d2.get("name")))
                .collect(Collectors.toList());
    }
    
    /**
     * Convert EntryExitRecord entity to AdminRecordDto.
     * 
     * @param record EntryExitRecord entity
     * @return AdminRecordDto
     */
    private AdminRecordDto convertToAdminRecordDto(EntryExitRecord record) {
        // Get user information
        Optional<User> userOpt = userRepository.findById(record.getUserId());
        User user = userOpt.orElse(null);
        
        return AdminRecordDto.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .userTcNo(user != null ? user.getTcNo() : null)
                .userFullName(user != null ? user.getFirstName() + " " + user.getLastName() : "Bilinmeyen Kullanıcı")
                .userPersonnelNo(user != null ? user.getPersonnelNo() : null)
                .userDepartmentCode(user != null ? user.getDepartmentCode() : null)
                .userDepartmentName(user != null ? user.getDepartmentName() : null)
                .type(record.getType().name())
                .typeDisplayName(getTypeDisplayName(record.getType().name()))
                .timestamp(record.getTimestamp())
                .latitude(record.getLatitude())
                .longitude(record.getLongitude())
                .qrCodeValue(record.getQrCodeValue())
                .ipAddress(record.getIpAddress())
                .hasGpsCoordinates(record.hasGpsCoordinates())
                .createdAt(record.getCreatedAt())
                .build();
    }
    
    /**
     * Get display name for entry/exit type.
     * 
     * @param type Type string
     * @return Display name
     */
    private String getTypeDisplayName(String type) {
        switch (type) {
            case "ENTRY":
                return "Giriş";
            case "EXIT":
                return "Çıkış";
            default:
                return type;
        }
    }
    
    /**
     * DTO for daily summary statistics.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DailySummaryStats {
        private LocalDate date;
        private long totalRecords;
        private long entryCount;
        private long exitCount;
        private long uniqueUsers;
    }
}