package com.bidb.personetakip.controller;

import com.bidb.personetakip.dto.DepartmentDto;
import com.bidb.personetakip.model.DepartmentPermission;
import com.bidb.personetakip.model.User;
import com.bidb.personetakip.repository.DepartmentPermissionRepository;
import com.bidb.personetakip.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for managing department permissions for DEPARTMENT_ADMIN users.
 * Only ADMIN and SUPER_ADMIN can manage department permissions.
 */
@RestController
@RequestMapping("/api/admin/department-permissions")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class DepartmentPermissionController {
    
    @Autowired
    private DepartmentPermissionRepository departmentPermissionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * Get department permissions for a specific user.
     * 
     * @param userId User ID
     * @return List of department permissions
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DepartmentPermission>> getUserDepartmentPermissions(@PathVariable Long userId) {
        List<DepartmentPermission> permissions = departmentPermissionRepository.findByUserId(userId);
        return ResponseEntity.ok(permissions);
    }
    
    /**
     * Get department codes that a user has permission to manage.
     * 
     * @param userId User ID
     * @return List of department codes
     */
    @GetMapping("/user/{userId}/departments")
    public ResponseEntity<List<String>> getUserAccessibleDepartments(@PathVariable Long userId) {
        List<String> departmentCodes = departmentPermissionRepository.findDepartmentCodesByUserId(userId);
        return ResponseEntity.ok(departmentCodes);
    }
    
    /**
     * Add department permission for a user.
     * 
     * @param userId User ID
     * @param departmentCode Department code
     * @return Success response
     */
    @PostMapping("/user/{userId}/department/{departmentCode}")
    public ResponseEntity<Map<String, Object>> addDepartmentPermission(
            @PathVariable Long userId, 
            @PathVariable String departmentCode) {
        
        // Check if user exists and is DEPARTMENT_ADMIN
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        
        User user = userOpt.get();
        if (!user.getRole().name().equals("DEPARTMENT_ADMIN")) {
            return ResponseEntity.badRequest().body(Map.of("error", "User must be DEPARTMENT_ADMIN"));
        }
        
        // Check if permission already exists
        if (departmentPermissionRepository.existsByUserIdAndDepartmentCode(userId, departmentCode)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Permission already exists"));
        }
        
        // Create new permission
        DepartmentPermission permission = DepartmentPermission.builder()
                .userId(userId)
                .departmentCode(departmentCode)
                .createdBy(1L) // TODO: Get from authentication
                .build();
        
        departmentPermissionRepository.save(permission);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Department permission added successfully"
        ));
    }
    
    /**
     * Remove department permission for a user.
     * 
     * @param userId User ID
     * @param departmentCode Department code
     * @return Success response
     */
    @DeleteMapping("/user/{userId}/department/{departmentCode}")
    @Transactional
    public ResponseEntity<Map<String, Object>> removeDepartmentPermission(
            @PathVariable Long userId, 
            @PathVariable String departmentCode) {
        
        departmentPermissionRepository.deleteByUserIdAndDepartmentCode(userId, departmentCode);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Department permission removed successfully"
        ));
    }
    
    /**
     * Set all department permissions for a user (replaces existing permissions).
     * 
     * @param userId User ID
     * @param request Request containing list of department codes
     * @return Success response
     */
    @PutMapping("/user/{userId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> setUserDepartmentPermissions(
            @PathVariable Long userId,
            @RequestBody Map<String, List<String>> request) {
        
        List<String> departmentCodes = request.get("departmentCodes");
        if (departmentCodes == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "departmentCodes is required"));
        }
        
        // Check if user exists and is DEPARTMENT_ADMIN
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        
        User user = userOpt.get();
        if (!user.getRole().name().equals("DEPARTMENT_ADMIN")) {
            return ResponseEntity.badRequest().body(Map.of("error", "User must be DEPARTMENT_ADMIN"));
        }
        
        // Remove all existing permissions using native query to avoid caching issues
        departmentPermissionRepository.deleteByUserId(userId);
        entityManager.flush();
        entityManager.clear();
        
        // Add new permissions
        for (String departmentCode : departmentCodes) {
            DepartmentPermission permission = DepartmentPermission.builder()
                    .userId(userId)
                    .departmentCode(departmentCode)
                    .createdBy(1L) // TODO: Get from authentication
                    .build();
            
            departmentPermissionRepository.save(permission);
        }
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Department permissions updated successfully",
                "departmentCodes", departmentCodes
        ));
    }
    
    /**
     * Get all available departments for permission assignment.
     * 
     * @return List of departments
     */
    @GetMapping("/available-departments")
    public ResponseEntity<List<DepartmentDto>> getAvailableDepartments() {
        List<Object[]> departments = userRepository.findDistinctDepartments();
        
        List<DepartmentDto> departmentDtos = departments.stream()
                .map(dept -> DepartmentDto.builder()
                        .code((String) dept[0])
                        .name((String) dept[1])
                        .userCount(((Number) dept[2]).longValue())
                        .build())
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(departmentDtos);
    }
    
    /**
     * Get all DEPARTMENT_ADMIN users for permission management.
     * 
     * @return List of department admin users
     */
    @GetMapping("/department-admins")
    public ResponseEntity<List<Map<String, Object>>> getDepartmentAdmins() {
        List<User> departmentAdmins = userRepository.findByRole(
                com.bidb.personetakip.model.UserRole.DEPARTMENT_ADMIN, 
                org.springframework.data.domain.PageRequest.of(0, 1000)
        ).getContent();
        
        List<Map<String, Object>> result = departmentAdmins.stream()
                .map(user -> {
                    List<String> permissions = departmentPermissionRepository.findDepartmentCodesByUserId(user.getId());
                    return Map.of(
                            "id", user.getId(),
                            "tcNo", user.getTcNo(),
                            "fullName", user.getFirstName() + " " + user.getLastName(),
                            "departmentCode", user.getDepartmentCode() != null ? user.getDepartmentCode() : "",
                            "departmentName", user.getDepartmentName() != null ? user.getDepartmentName() : "",
                            "permissions", permissions
                    );
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Debug endpoint to check all department permissions in database.
     * 
     * @return List of all department permissions
     */
    @GetMapping("/debug/all-permissions")
    public ResponseEntity<List<DepartmentPermission>> getAllPermissions() {
        List<DepartmentPermission> allPermissions = departmentPermissionRepository.findAll();
        return ResponseEntity.ok(allPermissions);
    }
    
    /**
     * Debug endpoint to check permissions for a specific user by TC number.
     * 
     * @param tcNo TC number
     * @return Debug info
     */
    @GetMapping("/debug/user-permissions/{tcNo}")
    public ResponseEntity<Map<String, Object>> getUserPermissionsByTcNo(@PathVariable String tcNo) {
        Optional<User> userOpt = userRepository.findByTcNo(tcNo);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("error", "User not found", "tcNo", tcNo));
        }
        
        User user = userOpt.get();
        List<String> permissions = departmentPermissionRepository.findDepartmentCodesByUserId(user.getId());
        
        return ResponseEntity.ok(Map.of(
            "userId", user.getId(),
            "tcNo", user.getTcNo(),
            "fullName", user.getFirstName() + " " + user.getLastName(),
            "role", user.getRole().name(),
            "departmentCode", user.getDepartmentCode() != null ? user.getDepartmentCode() : "",
            "departmentName", user.getDepartmentName() != null ? user.getDepartmentName() : "",
            "permissions", permissions
        ));
    }
}