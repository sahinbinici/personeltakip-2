package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.AdminRecordDto;
import com.bidb.personetakip.model.EntryExitRecord;
import com.bidb.personetakip.model.EntryExitType;
import com.bidb.personetakip.model.User;
import com.bidb.personetakip.repository.EntryExitRecordRepository;
import com.bidb.personetakip.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private static final Logger logger = LoggerFactory.getLogger(AdminRecordsService.class);
    
    @Autowired
    private EntryExitRecordRepository entryExitRecordRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private IpComplianceService ipComplianceService;
    
    @Autowired
    private IpPrivacyService ipPrivacyService;
    
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
     * @param ipMismatch IP mismatch filter (optional: "mismatch", "match", "unknown")
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of AdminRecordDto objects matching filters
     */
    public Page<AdminRecordDto> getRecordsWithFilters(LocalDate startDate, LocalDate endDate, 
                                                     Long userId, String departmentCode, String ipAddress, String ipMismatch, int page, int size) {
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
                    
                    // IP mismatch filter
                    if (ipMismatch != null && !ipMismatch.isEmpty()) {
                        Optional<User> userOpt = userRepository.findById(record.getUserId());
                        if (userOpt.isPresent()) {
                            IpComplianceService.IpComplianceStatus status = ipComplianceService.getIpComplianceStatus(record, userOpt.get());
                            
                            switch (ipMismatch.toLowerCase()) {
                                case "mismatch":
                                    if (status != IpComplianceService.IpComplianceStatus.MISMATCH) {
                                        return false;
                                    }
                                    break;
                                case "match":
                                    if (status != IpComplianceService.IpComplianceStatus.MATCH) {
                                        return false;
                                    }
                                    break;
                                case "unknown":
                                    if (status != IpComplianceService.IpComplianceStatus.UNKNOWN_IP && 
                                        status != IpComplianceService.IpComplianceStatus.NO_ASSIGNMENT) {
                                        return false;
                                    }
                                    break;
                            }
                        } else if (!"unknown".equalsIgnoreCase(ipMismatch)) {
                            // If user not found and not filtering for unknown, exclude this record
                            return false;
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
     * @param ipMismatch IP mismatch filter (optional)
     * @return CSV content as string
     * Requirements: 3.5 - CSV export functionality
     */
    public String generateCsvExport(LocalDate startDate, LocalDate endDate, Long userId, String departmentCode, String ipAddress, String ipMismatch) {
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
                    
                    // IP mismatch filter
                    if (ipMismatch != null && !ipMismatch.isEmpty()) {
                        Optional<User> userOpt = userRepository.findById(record.getUserId());
                        if (userOpt.isPresent()) {
                            IpComplianceService.IpComplianceStatus status = ipComplianceService.getIpComplianceStatus(record, userOpt.get());
                            
                            switch (ipMismatch.toLowerCase()) {
                                case "mismatch":
                                    if (status != IpComplianceService.IpComplianceStatus.MISMATCH) {
                                        return false;
                                    }
                                    break;
                                case "match":
                                    if (status != IpComplianceService.IpComplianceStatus.MATCH) {
                                        return false;
                                    }
                                    break;
                                case "unknown":
                                    if (status != IpComplianceService.IpComplianceStatus.UNKNOWN_IP && 
                                        status != IpComplianceService.IpComplianceStatus.NO_ASSIGNMENT) {
                                        return false;
                                    }
                                    break;
                            }
                        } else if (!"unknown".equalsIgnoreCase(ipMismatch)) {
                            // If user not found and not filtering for unknown, exclude this record
                            return false;
                        }
                    }
                    
                    return true;
                })
                .sorted((r1, r2) -> r2.getTimestamp().compareTo(r1.getTimestamp()))
                .collect(Collectors.toList());
        
        StringBuilder csv = new StringBuilder();
        
        // CSV Header - Enhanced with IP compliance information
        csv.append("Tarih,Saat,TC Kimlik No,Ad Soyad,Sicil No,Departman,Tür,QR Kod,IP Adresi,IP Uyumluluk,IP Mismatch,Atanmış IP'ler,Enlem,Boylam\n");
        
        // CSV Data
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        
        for (EntryExitRecord record : filteredRecords) {
            AdminRecordDto dto = convertToAdminRecordDto(record);
            Optional<User> userOpt = userRepository.findById(record.getUserId());
            
            // Get IP compliance information
            String ipComplianceStatus = "Bilinmiyor";
            String ipMismatchIndicator = "Hayır";
            String assignedIps = "";
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                IpComplianceService.IpComplianceStatus status = ipComplianceService.getIpComplianceStatus(record, user);
                
                // Set compliance status in Turkish
                switch (status) {
                    case MATCH:
                        ipComplianceStatus = "Uyumlu";
                        ipMismatchIndicator = "Hayır";
                        break;
                    case MISMATCH:
                        ipComplianceStatus = "Uyumsuz";
                        ipMismatchIndicator = "Evet";
                        break;
                    case NO_ASSIGNMENT:
                        ipComplianceStatus = "Atama Yok";
                        ipMismatchIndicator = "Hayır";
                        break;
                    case UNKNOWN_IP:
                        ipComplianceStatus = "IP Bilinmiyor";
                        ipMismatchIndicator = "Hayır";
                        break;
                }
                
                // Get assigned IPs (anonymized if configured)
                if (user.getAssignedIpAddresses() != null && !user.getAssignedIpAddresses().trim().isEmpty()) {
                    List<String> assignedIpList = ipComplianceService.parseAssignedIpAddresses(user.getAssignedIpAddresses());
                    StringBuilder assignedIpsBuilder = new StringBuilder();
                    for (int i = 0; i < assignedIpList.size(); i++) {
                        if (i > 0) assignedIpsBuilder.append("; ");
                        // Apply anonymization if configured
                        String displayIp = ipPrivacyService.displayIpAddress(assignedIpList.get(i), true);
                        assignedIpsBuilder.append(displayIp);
                    }
                    assignedIps = assignedIpsBuilder.toString();
                }
            }
            
            // Get IP address for export (anonymized if configured)
            String exportIpAddress = "Bilinmiyor";
            if (record.getIpAddress() != null) {
                exportIpAddress = ipPrivacyService.displayIpAddress(record.getIpAddress(), true);
                
                // Log IP address access for audit purposes
                try {
                    ipPrivacyService.logIpAddressAccess(record.getIpAddress(), record.getUserId(), null, "ACCESS");
                } catch (Exception e) {
                    logger.warn("Failed to log IP address access for export: {}", e.getMessage());
                }
            }
            
            csv.append(record.getTimestamp().format(dateFormatter)).append(",");
            csv.append(record.getTimestamp().format(timeFormatter)).append(",");
            csv.append(dto.getUserTcNo() != null ? dto.getUserTcNo() : "").append(",");
            csv.append(dto.getUserFullName() != null ? "\"" + dto.getUserFullName() + "\"" : "").append(",");
            csv.append(dto.getUserPersonnelNo() != null ? dto.getUserPersonnelNo() : "").append(",");
            csv.append(dto.getUserDepartmentName() != null ? "\"" + dto.getUserDepartmentName() + "\"" : "").append(",");
            csv.append(getTypeDisplayName(record.getType().name())).append(",");
            csv.append(record.getQrCodeValue()).append(",");
            csv.append(exportIpAddress).append(",");
            csv.append(ipComplianceStatus).append(",");
            csv.append(ipMismatchIndicator).append(",");
            csv.append(assignedIps.isEmpty() ? "Yok" : "\"" + assignedIps + "\"").append(",");
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
     * Get IP address statistics for advanced filtering.
     * 
     * @return Map containing IP statistics and common IP addresses
     * Requirements: 2.4 - IP address filtering functionality
     */
    public Map<String, Object> getIpStatistics() {
        List<EntryExitRecord> allRecords = entryExitRecordRepository.findAll();
        
        Map<String, Object> statistics = new HashMap<>();
        
        // Count total records with IP addresses
        long totalWithIp = allRecords.stream()
                .filter(record -> record.getIpAddress() != null && !record.getIpAddress().isEmpty())
                .count();
        
        long totalWithoutIp = allRecords.size() - totalWithIp;
        
        // Count IPv4 vs IPv6
        long ipv4Count = allRecords.stream()
                .filter(record -> record.getIpAddress() != null && isIPv4(record.getIpAddress()))
                .count();
        
        long ipv6Count = allRecords.stream()
                .filter(record -> record.getIpAddress() != null && isIPv6(record.getIpAddress()))
                .count();
        
        // Get most common IP addresses
        Map<String, Long> ipFrequency = allRecords.stream()
                .filter(record -> record.getIpAddress() != null && !record.getIpAddress().isEmpty())
                .collect(Collectors.groupingBy(EntryExitRecord::getIpAddress, Collectors.counting()));
        
        List<Map<String, Object>> commonIps = ipFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    Map<String, Object> ipInfo = new HashMap<>();
                    ipInfo.put("ipAddress", entry.getKey());
                    ipInfo.put("count", entry.getValue());
                    ipInfo.put("type", isIPv4(entry.getKey()) ? "IPv4" : (isIPv6(entry.getKey()) ? "IPv6" : "Unknown"));
                    return ipInfo;
                })
                .collect(Collectors.toList());
        
        // Get IP compliance statistics
        long mismatchCount = 0;
        long matchCount = 0;
        long noAssignmentCount = 0;
        
        for (EntryExitRecord record : allRecords) {
            Optional<User> userOpt = userRepository.findById(record.getUserId());
            if (userOpt.isPresent()) {
                IpComplianceService.IpComplianceStatus status = ipComplianceService.getIpComplianceStatus(record, userOpt.get());
                switch (status) {
                    case MATCH:
                        matchCount++;
                        break;
                    case MISMATCH:
                        mismatchCount++;
                        break;
                    case NO_ASSIGNMENT:
                    case UNKNOWN_IP:
                        noAssignmentCount++;
                        break;
                }
            } else {
                noAssignmentCount++;
            }
        }
        
        statistics.put("totalRecords", allRecords.size());
        statistics.put("totalWithIp", totalWithIp);
        statistics.put("totalWithoutIp", totalWithoutIp);
        statistics.put("ipv4Count", ipv4Count);
        statistics.put("ipv6Count", ipv6Count);
        statistics.put("commonIps", commonIps);
        statistics.put("mismatchCount", mismatchCount);
        statistics.put("matchCount", matchCount);
        statistics.put("noAssignmentCount", noAssignmentCount);
        
        return statistics;
    }
    
    /**
     * Search records by IP address with advanced options.
     * 
     * @param ipQuery IP search query (supports ranges, CIDR, exact match)
     * @param ipType IP type filter (ipv4, ipv6, unknown)
     * @param complianceStatus Compliance status filter
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of AdminRecordDto objects matching IP search criteria
     * Requirements: 2.4 - IP address filtering functionality
     */
    public Page<AdminRecordDto> searchRecordsByIp(String ipQuery, String ipType, String complianceStatus, int page, int size) {
        List<EntryExitRecord> allRecords = entryExitRecordRepository.findAll();
        
        List<EntryExitRecord> filteredRecords = allRecords.stream()
                .filter(record -> {
                    // IP query filter
                    if (ipQuery != null && !ipQuery.isEmpty()) {
                        if (!matchesIpQuery(record.getIpAddress(), ipQuery)) {
                            return false;
                        }
                    }
                    
                    // IP type filter
                    if (ipType != null && !ipType.isEmpty()) {
                        if (!matchesIpType(record.getIpAddress(), ipType)) {
                            return false;
                        }
                    }
                    
                    // Compliance status filter
                    if (complianceStatus != null && !complianceStatus.isEmpty()) {
                        Optional<User> userOpt = userRepository.findById(record.getUserId());
                        if (userOpt.isPresent()) {
                            IpComplianceService.IpComplianceStatus status = ipComplianceService.getIpComplianceStatus(record, userOpt.get());
                            
                            switch (complianceStatus.toLowerCase()) {
                                case "compliant":
                                    if (status != IpComplianceService.IpComplianceStatus.MATCH) {
                                        return false;
                                    }
                                    break;
                                case "non-compliant":
                                    if (status != IpComplianceService.IpComplianceStatus.MISMATCH) {
                                        return false;
                                    }
                                    break;
                                case "no-assignment":
                                    if (status != IpComplianceService.IpComplianceStatus.NO_ASSIGNMENT && 
                                        status != IpComplianceService.IpComplianceStatus.UNKNOWN_IP) {
                                        return false;
                                    }
                                    break;
                            }
                        } else if (!"no-assignment".equalsIgnoreCase(complianceStatus)) {
                            return false;
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
     * Check if IP address matches the given query.
     * Supports exact match, range, CIDR notation, and contains.
     */
    private boolean matchesIpQuery(String ipAddress, String query) {
        if (ipAddress == null) {
            return "unknown".equalsIgnoreCase(query) || "null".equalsIgnoreCase(query);
        }
        
        query = query.trim();
        
        // Handle special queries
        if ("unknown".equalsIgnoreCase(query) || "null".equalsIgnoreCase(query)) {
            return false; // IP address exists, so it's not unknown
        }
        
        // Handle range queries (e.g., "range:192.168.1.1-192.168.1.255")
        if (query.startsWith("range:")) {
            String rangeSpec = query.substring(6);
            String[] parts = rangeSpec.split("-");
            if (parts.length == 2) {
                return isIpInRange(ipAddress, parts[0].trim(), parts[1].trim());
            }
        }
        
        // Handle subnet queries (e.g., "subnet:192.168.1.0/24")
        if (query.startsWith("subnet:")) {
            String subnet = query.substring(7);
            return isIpInSubnet(ipAddress, subnet);
        }
        
        // Handle prefix matching (e.g., "192.168.1.")
        if (query.endsWith(".")) {
            return ipAddress.startsWith(query);
        }
        
        // Handle CIDR notation
        if (query.contains("/")) {
            return isIpInSubnet(ipAddress, query);
        }
        
        // Handle contains matching
        if (query.startsWith("*") && query.endsWith("*")) {
            String searchTerm = query.substring(1, query.length() - 1);
            return ipAddress.contains(searchTerm);
        }
        
        // Exact match
        return query.equals(ipAddress);
    }
    
    /**
     * Check if IP address matches the given type.
     */
    private boolean matchesIpType(String ipAddress, String type) {
        if (ipAddress == null) {
            return "unknown".equalsIgnoreCase(type);
        }
        
        switch (type.toLowerCase()) {
            case "ipv4":
                return isIPv4(ipAddress);
            case "ipv6":
                return isIPv6(ipAddress);
            case "unknown":
                return !isIPv4(ipAddress) && !isIPv6(ipAddress);
            default:
                return true;
        }
    }
    
    /**
     * Check if IP address is in the specified range.
     */
    private boolean isIpInRange(String ipAddress, String startIp, String endIp) {
        try {
            // Simple IPv4 range check (this could be enhanced for IPv6)
            if (isIPv4(ipAddress) && isIPv4(startIp) && isIPv4(endIp)) {
                long ip = ipToLong(ipAddress);
                long start = ipToLong(startIp);
                long end = ipToLong(endIp);
                return ip >= start && ip <= end;
            }
        } catch (Exception e) {
            // If parsing fails, fall back to string comparison
        }
        return false;
    }
    
    /**
     * Check if IP address is in the specified subnet (CIDR notation).
     */
    private boolean isIpInSubnet(String ipAddress, String subnet) {
        try {
            if (!isIPv4(ipAddress) || !subnet.contains("/")) {
                return false;
            }
            
            String[] parts = subnet.split("/");
            String networkIp = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);
            
            long ip = ipToLong(ipAddress);
            long network = ipToLong(networkIp);
            long mask = (-1L << (32 - prefixLength));
            
            return (ip & mask) == (network & mask);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Convert IPv4 address to long for range calculations.
     */
    private long ipToLong(String ipAddress) {
        String[] parts = ipAddress.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) + Integer.parseInt(parts[i]);
        }
        return result;
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
        
        // Determine IP mismatch status
        Boolean ipMismatch = null;
        if (user != null) {
            IpComplianceService.IpComplianceStatus status = ipComplianceService.getIpComplianceStatus(record, user);
            switch (status) {
                case MATCH:
                    ipMismatch = false;
                    break;
                case MISMATCH:
                    ipMismatch = true;
                    break;
                case NO_ASSIGNMENT:
                case UNKNOWN_IP:
                default:
                    ipMismatch = null; // No assignment or unknown IP
                    break;
            }
        }
        
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
                .ipMismatch(ipMismatch)
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
     * Check if IP address is IPv4 format.
     */
    private boolean isIPv4(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return ip.matches(ipv4Pattern);
    }
    
    /**
     * Check if IP address is IPv6 format.
     */
    private boolean isIPv6(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        // Simple IPv6 check - contains colons and valid hex characters
        return ip.contains(":") && ip.matches("^[0-9a-fA-F:]+$");
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