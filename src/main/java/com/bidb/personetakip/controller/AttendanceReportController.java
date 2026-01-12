package com.bidb.personetakip.controller;

import com.bidb.personetakip.dto.AttendanceReportDto;
import com.bidb.personetakip.service.AttendanceReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class AttendanceReportController {
    
    private final AttendanceReportService attendanceReportService;
    
    public AttendanceReportController(AttendanceReportService attendanceReportService) {
        this.attendanceReportService = attendanceReportService;
    }
    
    /**
     * Attendance reports page
     */
    @GetMapping("/admin/attendance-reports")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEPARTMENT_ADMIN') or hasRole('SUPER_ADMIN')")
    public String attendanceReportsPage() {
        return "admin-attendance-reports";
    }
    
    /**
     * Get attendance report API
     */
    @GetMapping("/api/admin/attendance-reports")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEPARTMENT_ADMIN') or hasRole('SUPER_ADMIN')")
    @ResponseBody
    public ResponseEntity<AttendanceReportDto> getAttendanceReport(
            @RequestParam(defaultValue = "MONTH") String period,
            @RequestParam(required = false) String departmentCode) {
        
        AttendanceReportDto report = attendanceReportService.generateReport(period, departmentCode);
        return ResponseEntity.ok(report);
    }
    
    /**
     * Get departments for filter dropdown
     */
    @GetMapping("/api/admin/attendance-reports/departments")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEPARTMENT_ADMIN') or hasRole('SUPER_ADMIN')")
    @ResponseBody
    public ResponseEntity<List<Map<String, String>>> getDepartments() {
        return ResponseEntity.ok(attendanceReportService.getDepartments());
    }
}
