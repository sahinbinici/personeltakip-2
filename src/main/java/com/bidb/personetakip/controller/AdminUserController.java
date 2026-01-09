package com.bidb.personetakip.controller;

import com.bidb.personetakip.dto.AdminUserDto;
import com.bidb.personetakip.dto.DepartmentDto;
import com.bidb.personetakip.security.JwtAuthenticationFilter;
import com.bidb.personetakip.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for admin user management operations.
 * Provides endpoints for user listing, search, filtering, and role updates.
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6 - User management API endpoints
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN') or hasRole('DEPARTMENT_ADMIN') or hasRole('SUPER_ADMIN')")
@Tag(name = "Admin - User Management", description = "Admin endpoints for user management, role updates, and IP assignments")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {
    
    @Autowired
    private AdminUserService adminUserService;
    
    /**
     * Get paginated list of all users.
     * 
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return Page of AdminUserDto objects
     * Requirements: 2.1, 2.2 - Paginated user listing with user information
     */
    @Operation(
        summary = "Get all users",
        description = "Retrieves a paginated list of all users in the system"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - Admin role required",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Access Denied",
                    value = """
                    {
                        "error": "Access Denied",
                        "message": "Admin role required"
                    }
                    """
                )
            )
        )
    })
    @GetMapping
    public ResponseEntity<Page<AdminUserDto>> getAllUsers(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        // Get admin user details from authentication
        Long adminUserId = (Long) authentication.getPrincipal();
        String adminRole = null;
        String adminDepartmentCode = null;
        
        // Extract role from authentication details
        if (authentication.getDetails() instanceof JwtAuthenticationFilter.JwtAuthenticationDetails) {
            JwtAuthenticationFilter.JwtAuthenticationDetails details = 
                (JwtAuthenticationFilter.JwtAuthenticationDetails) authentication.getDetails();
            adminRole = details.getRole();
        }
        
        // Get admin user's department if they are a department admin
        if ("DEPARTMENT_ADMIN".equals(adminRole)) {
            AdminUserDto adminUser = adminUserService.getUserById(adminUserId);
            if (adminUser != null) {
                adminDepartmentCode = adminUser.getDepartmentCode();
            }
        }
        
        Page<AdminUserDto> users = adminUserService.getAllUsersWithDepartmentFilter(page, size, adminUserId, adminRole, adminDepartmentCode);
        return ResponseEntity.ok(users);
    }
    
    /**
     * Search users by TC number, name, or personnel number.
     * 
     * @param searchTerm Search term
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return Page of matching AdminUserDto objects
     * Requirements: 2.5 - User search functionality
     */
    @GetMapping("/search")
    public ResponseEntity<Page<AdminUserDto>> searchUsers(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        // Get admin user details from authentication
        Long adminUserId = (Long) authentication.getPrincipal();
        String adminRole = null;
        String adminDepartmentCode = null;
        
        // Extract role from authentication details
        if (authentication.getDetails() instanceof JwtAuthenticationFilter.JwtAuthenticationDetails) {
            JwtAuthenticationFilter.JwtAuthenticationDetails details = 
                (JwtAuthenticationFilter.JwtAuthenticationDetails) authentication.getDetails();
            adminRole = details.getRole();
        }
        
        // Get admin user's department if they are a department admin
        if ("DEPARTMENT_ADMIN".equals(adminRole)) {
            AdminUserDto adminUser = adminUserService.getUserById(adminUserId);
            if (adminUser != null) {
                adminDepartmentCode = adminUser.getDepartmentCode();
            }
        }
        
        Page<AdminUserDto> users = adminUserService.searchUsersWithDepartmentFilter(searchTerm, page, size, adminUserId, adminRole, adminDepartmentCode);
        return ResponseEntity.ok(users);
    }
    
    /**
     * Filter users by role.
     * 
     * @param role User role to filter by
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return Page of AdminUserDto objects with specified role
     * Requirements: 2.6 - Role-based user filtering
     */
    @GetMapping("/filter")
    public ResponseEntity<Page<AdminUserDto>> getUsersWithFilters(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String departmentCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<AdminUserDto> users = adminUserService.getUsersWithFilters(role, departmentCode, page, size);
        return ResponseEntity.ok(users);
    }
    
    /**
     * Get user details by ID.
     * 
     * @param userId User ID
     * @return AdminUserDto or 404 if not found
     * Requirements: 2.3 - User detail view
     */
    @GetMapping("/{userId}")
    public ResponseEntity<AdminUserDto> getUserById(@PathVariable Long userId) {
        AdminUserDto user = adminUserService.getUserById(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }
    
    /**
     * Update user role.
     * 
     * @param userId User ID to update
     * @param request Request body containing new role
     * @param authentication Current admin authentication
     * @return Updated AdminUserDto or 404 if user not found
     * Requirements: 2.4 - Role update with audit logging
     */
    @Operation(
        summary = "Update user role",
        description = "Updates the role of a specific user (NORMAL_USER, ADMIN, SUPER_ADMIN). Only ADMIN and SUPER_ADMIN can change roles."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User role updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AdminUserDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid role or missing data",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Invalid Role",
                    value = """
                    {
                        "message": "Invalid role specified"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - Only ADMIN and SUPER_ADMIN can change roles",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Access Denied",
                    value = """
                    {
                        "message": "Access denied - Only ADMIN and SUPER_ADMIN can change roles"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "User Not Found",
                    value = """
                    {
                        "message": "User not found"
                    }
                    """
                )
            )
        )
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{userId}/role")
    public ResponseEntity<?> updateUserRole(
            @Parameter(description = "User ID to update", required = true, example = "123")
            @PathVariable Long userId,
            @Parameter(description = "Request body containing new role", required = true)
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        String newRole = request.get("role");
        if (newRole == null || newRole.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Role is required"));
        }
        
        // Validate role
        if (!isValidRole(newRole)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid role specified"));
        }
        
        // Get admin user ID from authentication
        Long adminUserId = (Long) authentication.getPrincipal();
        
        AdminUserDto updatedUser = adminUserService.updateUserRole(userId, newRole, adminUserId);
        if (updatedUser == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }
        
        return ResponseEntity.ok(updatedUser);
    }
    
    /**
     * Update user IP assignment.
     * 
     * @param userId User ID to update
     * @param request Request body containing IP addresses
     * @param authentication Current admin authentication
     * @return Updated AdminUserDto or 404 if user not found
     * Requirements: 3.1, 3.2 - IP assignment field availability and validation
     */
    @PutMapping("/{userId}/ip-assignment")
    public ResponseEntity<?> updateUserIpAssignment(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        String ipAddresses = request.get("ipAddresses");
        
        try {
            // Get admin user ID from authentication
            Long adminUserId = (Long) authentication.getPrincipal();
            
            AdminUserDto updatedUser = adminUserService.updateUserIpAssignment(userId, ipAddresses, adminUserId);
            if (updatedUser == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Remove user IP assignment.
     * 
     * @param userId User ID to update
     * @param authentication Current admin authentication
     * @return Updated AdminUserDto or 404 if user not found
     * Requirements: 3.5 - IP assignment removal functionality
     */
    @DeleteMapping("/{userId}/ip-assignment")
    public ResponseEntity<AdminUserDto> removeUserIpAssignment(
            @PathVariable Long userId,
            Authentication authentication) {
        
        // Get admin user ID from authentication
        Long adminUserId = (Long) authentication.getPrincipal();
        
        AdminUserDto updatedUser = adminUserService.removeUserIpAssignment(userId, adminUserId);
        if (updatedUser == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(updatedUser);
    }
    
    /**
     * Get user role statistics.
     * 
     * @return User role statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminUserService.UserRoleStats> getUserRoleStats() {
        AdminUserService.UserRoleStats stats = adminUserService.getUserRoleStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get all departments with user counts.
     * 
     * @param authentication Authentication object for department filtering
     * @return List of departments
     */
    @GetMapping("/departments")
    public ResponseEntity<java.util.List<com.bidb.personetakip.dto.DepartmentDto>> getAllDepartments(Authentication authentication) {
        // Get admin user details from authentication
        Long adminUserId = (Long) authentication.getPrincipal();
        String adminRole = null;
        String adminDepartmentCode = null;
        
        // Extract role from authentication details
        if (authentication.getDetails() instanceof JwtAuthenticationFilter.JwtAuthenticationDetails) {
            JwtAuthenticationFilter.JwtAuthenticationDetails details = 
                (JwtAuthenticationFilter.JwtAuthenticationDetails) authentication.getDetails();
            adminRole = details.getRole();
        }
        
        // Get admin user's department if they are a department admin
        if ("DEPARTMENT_ADMIN".equals(adminRole)) {
            AdminUserDto adminUser = adminUserService.getUserById(adminUserId);
            if (adminUser != null) {
                adminDepartmentCode = adminUser.getDepartmentCode();
            }
        }
        
        java.util.List<com.bidb.personetakip.dto.DepartmentDto> departments = adminUserService.getAllDepartmentsWithFilter(adminUserId, adminRole, adminDepartmentCode);
        return ResponseEntity.ok(departments);
    }
    
    /**
     * Get users by attendance status.
     * 
     * @param attendanceStatus "NO_RECORDS", "INSIDE", "OUTSIDE"
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return Page of users matching attendance status
     */
    @GetMapping("/attendance-filter")
    public ResponseEntity<Page<AdminUserDto>> getUsersByAttendanceStatus(
            @RequestParam String attendanceStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<AdminUserDto> users = adminUserService.getUsersByAttendanceStatus(attendanceStatus, page, size);
        return ResponseEntity.ok(users);
    }
    
    
    /**
     * Validate if role is valid.
     * 
     * @param role Role to validate
     * @return true if valid
     */
    private boolean isValidRole(String role) {
        return "NORMAL_USER".equals(role) || "DEPARTMENT_ADMIN".equals(role) || "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
    }
}