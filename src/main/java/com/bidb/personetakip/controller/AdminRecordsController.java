package com.bidb.personetakip.controller;

import com.bidb.personetakip.dto.AdminRecordDto;
import com.bidb.personetakip.service.AdminRecordsService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * REST controller for admin entry/exit records management operations.
 * Provides endpoints for record listing, filtering, and CSV export.
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6 - Entry/exit records management API
 */
@RestController
@RequestMapping("/api/admin/records")
@PreAuthorize("hasRole('ADMIN') or hasRole('DEPARTMENT_ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminRecordsController {

    private final AdminRecordsService adminRecordsService;

    public AdminRecordsController(AdminRecordsService adminRecordsService) {
        this.adminRecordsService = adminRecordsService;
    }

    /**
     * Get paginated list of all entry/exit records.
     *
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param authentication Authentication object for department filtering
     * @return Page of AdminRecordDto objects
     * Requirements: 3.1, 3.2 - Paginated record listing with user information
     */
    @GetMapping
    public ResponseEntity<Page<AdminRecordDto>> getAllRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Page<AdminRecordDto> records = adminRecordsService.getAllRecords(page, size, authentication);
        return ResponseEntity.ok(records);
    }

    /**
     * Get entry/exit records with filters.
     *
     * @param startDate Start date (optional, format: yyyy-MM-dd)
     * @param endDate End date (optional, format: yyyy-MM-dd)
     * @param userId User ID (optional)
     * @param departmentCode Department code (optional)
     * @param ipAddress IP address filter (optional)
     * @param ipMismatch IP mismatch filter (optional: "mismatch", "match", "unknown")
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param authentication Authentication object for department filtering
     * @return Page of AdminRecordDto objects matching filters
     * Requirements: 3.3, 3.4, 2.4, 4.4 - Date range, user, department, IP and IP mismatch filtering
     */
    @GetMapping("/filter")
    public ResponseEntity<Page<AdminRecordDto>> getRecordsWithFilters(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String departmentCode,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) String ipMismatch,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Page<AdminRecordDto> records = adminRecordsService.getRecordsWithFilters(
                startDate, endDate, userId, departmentCode, ipAddress, ipMismatch, page, size, authentication);
        return ResponseEntity.ok(records);
    }

    /**
     * Get IP address statistics for advanced filtering.
     *
     * @param authentication Authentication object for department filtering
     * @return IP address statistics and common IPs
     * Requirements: 2.4 - IP address filtering functionality
     */
    @GetMapping("/ip-statistics")
    public ResponseEntity<Map<String, Object>> getIpStatistics(Authentication authentication) {
        Map<String, Object> statistics = adminRecordsService.getIpStatistics(authentication);
        return ResponseEntity.ok(statistics);
    }

    /**
     * Search records by IP address with advanced options.
     *
     * @param ipQuery IP search query (supports ranges, CIDR, exact match)
     * @param ipType IP type filter (ipv4, ipv6, unknown)
     * @param complianceStatus Compliance status filter
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param authentication Authentication object for department filtering
     * @return Page of AdminRecordDto objects matching IP search criteria
     * Requirements: 2.4 - IP address filtering functionality
     */
    @GetMapping("/search-ip")
    public ResponseEntity<Page<AdminRecordDto>> searchRecordsByIp(
            @RequestParam String ipQuery,
            @RequestParam(required = false) String ipType,
            @RequestParam(required = false) String complianceStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Page<AdminRecordDto> records = adminRecordsService.searchRecordsByIp(
                ipQuery, ipType, complianceStatus, page, size, authentication);
        return ResponseEntity.ok(records);
    }

    /**
     * Get entry/exit records by date range.
     *
     * @param startDate Start date (format: yyyy-MM-dd)
     * @param endDate End date (format: yyyy-MM-dd)
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param authentication Authentication object for department filtering
     * @return Page of AdminRecordDto objects within date range
     * Requirements: 3.3 - Date range filtering
     */
    @GetMapping("/date-range")
    public ResponseEntity<Page<AdminRecordDto>> getRecordsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Page<AdminRecordDto> records = adminRecordsService.getRecordsByDateRange(
                startDate, endDate, page, size, authentication);
        return ResponseEntity.ok(records);
    }

    /**
     * Get entry/exit records by user.
     *
     * @param userId User ID
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param authentication Authentication object for department filtering
     * @return Page of AdminRecordDto objects for specified user
     * Requirements: 3.4 - User-specific record filtering
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<AdminRecordDto>> getRecordsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Page<AdminRecordDto> records = adminRecordsService.getRecordsByUser(userId, page, size, authentication);
        return ResponseEntity.ok(records);
    }

    /**
     * Export entry/exit records as CSV.
     *
     * @param startDate Start date (optional, format: yyyy-MM-dd)
     * @param endDate End date (optional, format: yyyy-MM-dd)
     * @param userId User ID (optional)
     * @param departmentCode Department code (optional)
     * @param ipAddress IP address filter (optional)
     * @param ipMismatch IP mismatch filter (optional)
     * @param authentication Authentication object for department filtering
     * @return CSV file download
     * Requirements: 3.5, 2.5 - CSV export functionality with IP addresses
     */
    @GetMapping("/export/csv")
    public ResponseEntity<String> exportRecordsCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String departmentCode,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) String ipMismatch,
            Authentication authentication) {

        String csvContent = adminRecordsService.generateCsvExport(startDate, endDate, userId, departmentCode, ipAddress, ipMismatch, authentication);

        // Generate filename with current date
        String filename = "giris_cikis_kayitlari_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", filename);

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvContent);
    }

    /**
     * Get daily summary statistics.
     *
     * @param date Date to get statistics for (optional, defaults to today)
     * @param authentication Authentication object for department filtering
     * @return Daily summary statistics
     * Requirements: 3.6 - Daily summary statistics
     */
    @GetMapping("/stats/daily")
    public ResponseEntity<AdminRecordsService.DailySummaryStats> getDailySummaryStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {

        if (date == null) {
            date = LocalDate.now();
        }

        AdminRecordsService.DailySummaryStats stats = adminRecordsService.getDailySummaryStats(date, authentication);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get entry/exit records with IP mismatches.
     *
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param authentication Authentication object for department filtering
     * @return Page of AdminRecordDto objects with IP mismatches
     * Requirements: 4.4 - IP mismatch filtering
     */
    @GetMapping("/ip-mismatch")
    public ResponseEntity<Page<AdminRecordDto>> getRecordsWithIpMismatch(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Page<AdminRecordDto> records = adminRecordsService.getRecordsWithIpMismatch(page, size, authentication);
        return ResponseEntity.ok(records);
    }

    /**
     * Get list of departments for filtering.
     *
     * @param authentication Authentication object for department filtering
     * @return List of department codes and names
     */
    @GetMapping("/departments")
    public ResponseEntity<List<Map<String, String>>> getDepartments(Authentication authentication) {
        List<Map<String, String>> departments = adminRecordsService.getDepartments(authentication);
        return ResponseEntity.ok(departments);
    }
}
