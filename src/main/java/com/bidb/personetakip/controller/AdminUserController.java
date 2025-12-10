package com.bidb.personetakip.controller;

import com.bidb.personetakip.dto.AdminUserDto;
import com.bidb.personetakip.security.JwtAuthenticationFilter;
import com.bidb.personetakip.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for admin user management operations.
 * Provides endpoints for user listing, search, filtering, and role updates.
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6 - User management API endpoints
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
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
    @GetMapping
    public ResponseEntity<Page<AdminUserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<AdminUserDto> users = adminUserService.getAllUsers(page, size);
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
            @RequestParam(defaultValue = "20") int size) {
        
        Page<AdminUserDto> users = adminUserService.searchUsers(searchTerm, page, size);
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
    @PutMapping("/{userId}/role")
    public ResponseEntity<AdminUserDto> updateUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        String newRole = request.get("role");
        if (newRole == null || newRole.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        // Validate role
        if (!isValidRole(newRole)) {
            return ResponseEntity.badRequest().build();
        }
        
        // Get admin user ID from authentication
        Long adminUserId = (Long) authentication.getPrincipal();
        
        AdminUserDto updatedUser = adminUserService.updateUserRole(userId, newRole, adminUserId);
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
     * Validate if role is valid.
     * 
     * @param role Role to validate
     * @return true if valid
     */
    private boolean isValidRole(String role) {
        return "NORMAL_USER".equals(role) || "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
    }
}