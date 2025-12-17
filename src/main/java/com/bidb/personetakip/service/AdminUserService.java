package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.AdminUserDto;
import com.bidb.personetakip.exception.IpAssignmentException;
import com.bidb.personetakip.model.AdminAuditLog;
import com.bidb.personetakip.model.User;
import com.bidb.personetakip.model.UserRole;
import com.bidb.personetakip.repository.AdminAuditLogRepository;
import com.bidb.personetakip.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for admin user management operations.
 * Provides user listing, search, filtering, and role management with audit logging.
 * 
 * Requirements: 2.1, 2.4, 2.5, 2.6 - User management operations
 */
@Service
public class AdminUserService {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminUserService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AdminAuditLogRepository adminAuditLogRepository;
    
    @Autowired
    private IpComplianceService ipComplianceService;
    
    @Autowired
    private IpPrivacyService ipPrivacyService;
    
    /**
     * Get paginated list of all users.
     * 
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of AdminUserDto objects
     * Requirements: 2.1 - Paginated user listing
     */
    public Page<AdminUserDto> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users = userRepository.findAll(pageable);
        return users.map(this::convertToAdminUserDto);
    }
    
    /**
     * Search users by TC number, name, or personnel number.
     * 
     * @param searchTerm Search term
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of matching AdminUserDto objects
     * Requirements: 2.5 - User search functionality
     */
    public Page<AdminUserDto> searchUsers(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users = userRepository.findBySearchTerm(searchTerm, pageable);
        return users.map(this::convertToAdminUserDto);
    }
    
    /**
     * Filter users by role.
     * 
     * @param role User role to filter by
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of AdminUserDto objects with specified role
     * Requirements: 2.6 - Role-based user filtering
     */
    public Page<AdminUserDto> getUsersByRole(String role, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        UserRole userRole = UserRole.valueOf(role);
        Page<User> users = userRepository.findByRole(userRole, pageable);
        return users.map(this::convertToAdminUserDto);
    }
    
    /**
     * Get users filtered by role and/or department.
     * 
     * @param role User role (optional)
     * @param departmentCode Department code (optional)
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of AdminUserDto objects matching filters
     */
    public Page<AdminUserDto> getUsersWithFilters(String role, String departmentCode, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        if (role != null && !role.isEmpty() && departmentCode != null && !departmentCode.isEmpty()) {
            // Both filters
            UserRole userRole = UserRole.valueOf(role);
            Page<User> users = userRepository.findByRoleAndDepartmentCode(userRole, departmentCode, pageable);
            return users.map(this::convertToAdminUserDto);
        } else if (role != null && !role.isEmpty()) {
            // Only role filter
            return getUsersByRole(role, page, size);
        } else if (departmentCode != null && !departmentCode.isEmpty()) {
            // Only department filter
            Page<User> users = userRepository.findByDepartmentCode(departmentCode, pageable);
            return users.map(this::convertToAdminUserDto);
        } else {
            // No filters
            return getAllUsers(page, size);
        }
    }
    
    /**
     * Get user details by ID.
     * 
     * @param userId User ID
     * @return AdminUserDto or null if not found
     * Requirements: 2.3 - User detail view
     */
    public AdminUserDto getUserById(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.map(this::convertToAdminUserDto).orElse(null);
    }
    
    /**
     * Update user role with audit logging.
     * 
     * @param userId User ID to update
     * @param newRole New role to assign
     * @param adminUserId ID of admin performing the action
     * @return Updated AdminUserDto or null if user not found
     * Requirements: 2.4 - Role update with audit logging
     */
    @Transactional
    public AdminUserDto updateUserRole(Long userId, String newRole, Long adminUserId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return null;
        }
        
        User user = userOpt.get();
        UserRole oldRole = user.getRole();
        UserRole newUserRole = UserRole.valueOf(newRole);
        
        // Update role
        user.setRole(newUserRole);
        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);
        
        // Create audit log
        AdminAuditLog auditLog = AdminAuditLog.builder()
                .adminUserId(adminUserId)
                .action("ROLE_CHANGE")
                .targetUserId(userId)
                .details(String.format("{\"oldRole\":\"%s\",\"newRole\":\"%s\"}", oldRole.name(), newRole))
                .timestamp(LocalDateTime.now())
                .build();
        adminAuditLogRepository.save(auditLog);
        
        return convertToAdminUserDto(updatedUser);
    }
    
    /**
     * Get user statistics by role.
     * 
     * @return Map of role counts
     */
    public UserRoleStats getUserRoleStats() {
        long normalUsers = userRepository.countByRole(UserRole.NORMAL_USER);
        long adminUsers = userRepository.countByRole(UserRole.ADMIN);
        long superAdminUsers = userRepository.countByRole(UserRole.SUPER_ADMIN);
        long totalUsers = userRepository.count();
        
        return UserRoleStats.builder()
                .normalUsers(normalUsers)
                .adminUsers(adminUsers)
                .superAdminUsers(superAdminUsers)
                .totalUsers(totalUsers)
                .build();
    }
    
    /**
     * Update user IP assignment with comprehensive error handling and validation.
     * 
     * @param userId User ID to update
     * @param ipAddresses Comma-separated IP addresses to assign
     * @param adminUserId ID of admin performing the action
     * @return Updated AdminUserDto
     * @throws IpAssignmentException if IP assignment validation fails
     * Requirements: 3.2, 3.5 - IP assignment validation and management
     */
    @Transactional
    public AdminUserDto updateUserIpAssignment(Long userId, String ipAddresses, Long adminUserId) throws IpAssignmentException {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IpAssignmentException("User not found", "update", userId.toString());
        }
        
        User user = userOpt.get();
        String oldIpAddresses = user.getAssignedIpAddresses();
        
        try {
            // Validate IP addresses using comprehensive validation
            if (ipAddresses != null && !ipAddresses.trim().isEmpty()) {
                ipComplianceService.validateAssignedIpAddressesWithException(ipAddresses, userId.toString());
            }
            
            // Update IP assignment
            user.setAssignedIpAddresses(ipAddresses != null && !ipAddresses.trim().isEmpty() ? ipAddresses.trim() : null);
            user.setUpdatedAt(LocalDateTime.now());
            User updatedUser = userRepository.save(user);
            
            // Log IP address modification for audit purposes
            try {
                ipPrivacyService.logIpAddressModification(
                    oldIpAddresses, 
                    ipAddresses, 
                    userId, 
                    adminUserId, 
                    ipAddresses == null ? "REMOVE" : "ASSIGN"
                );
            } catch (Exception e) {
                // Log error but don't fail the operation
                logger.warn("Failed to log IP address modification for user {}: {}", userId, e.getMessage());
            }
            
            // Create audit log
            AdminAuditLog auditLog = AdminAuditLog.builder()
                    .adminUserId(adminUserId)
                    .action("IP_ASSIGNMENT_CHANGE")
                    .targetUserId(userId)
                    .details(String.format("{\"oldIpAddresses\":\"%s\",\"newIpAddresses\":\"%s\"}", 
                            oldIpAddresses != null ? oldIpAddresses : "", 
                            ipAddresses != null ? ipAddresses : ""))
                    .timestamp(LocalDateTime.now())
                    .build();
            adminAuditLogRepository.save(auditLog);
            
            return convertToAdminUserDto(updatedUser);
            
        } catch (IpAssignmentException e) {
            // Re-throw IP assignment exceptions with context
            throw new IpAssignmentException(
                "IP assignment failed for user " + userId + ": " + e.getMessage(), 
                e.getAssignmentOperation(), 
                userId.toString());
        } catch (Exception e) {
            // Wrap unexpected exceptions
            throw new IpAssignmentException(
                "Unexpected error during IP assignment for user " + userId + ": " + e.getMessage(), 
                e);
        }
    }
    
    /**
     * Remove IP assignment from user with error handling.
     * 
     * @param userId User ID to update
     * @param adminUserId ID of admin performing the action
     * @return Updated AdminUserDto
     * @throws IpAssignmentException if removal fails
     * Requirements: 3.5 - IP assignment removal functionality
     */
    @Transactional
    public AdminUserDto removeUserIpAssignment(Long userId, Long adminUserId) throws IpAssignmentException {
        return updateUserIpAssignment(userId, null, adminUserId);
    }
    
    /**
     * Convert User entity to AdminUserDto.
     * 
     * @param user User entity
     * @return AdminUserDto
     */
    private AdminUserDto convertToAdminUserDto(User user) {
        return AdminUserDto.builder()
                .id(user.getId())
                .tcNo(user.getTcNo())
                .personnelNo(user.getPersonnelNo())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .mobilePhone(user.getMobilePhone())
                .departmentCode(user.getDepartmentCode())
                .departmentName(user.getDepartmentName())
                .titleCode(user.getTitleCode())
                .role(user.getRole().name())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .assignedIpAddresses(user.getAssignedIpAddresses())
                .build();
    }
    
    /**
     * DTO for user role statistics.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserRoleStats {
        private long normalUsers;
        private long adminUsers;
        private long superAdminUsers;
        private long totalUsers;
    }
}