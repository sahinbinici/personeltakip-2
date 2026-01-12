package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.AdminUserDto;
import com.bidb.personetakip.dto.DepartmentDto;
import com.bidb.personetakip.dto.ManualUserCreateDto;
import com.bidb.personetakip.dto.UserUpdateDto;
import com.bidb.personetakip.exception.IpAssignmentException;
import com.bidb.personetakip.model.AdminAuditLog;
import com.bidb.personetakip.model.User;
import com.bidb.personetakip.model.UserRole;
import com.bidb.personetakip.repository.AdminAuditLogRepository;
import com.bidb.personetakip.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private com.bidb.personetakip.repository.DepartmentPermissionRepository departmentPermissionRepository;
    
    /**
     * Get accessible department codes for the authenticated user.
     * SUPER_ADMIN and ADMIN can access all departments.
     * DEPARTMENT_ADMIN can access departments they have permissions for.
     * 
     * @param adminUserId ID of the admin making the request
     * @param adminRole Role of the admin making the request
     * @return List of accessible department codes (null means all departments)
     */
    private List<String> getAccessibleDepartmentCodes(Long adminUserId, String adminRole) {
        logger.info("getAccessibleDepartmentCodes - adminUserId: {}, adminRole: {}", adminUserId, adminRole);
        
        if ("SUPER_ADMIN".equals(adminRole) || "ADMIN".equals(adminRole)) {
            logger.info("User is SUPER_ADMIN or ADMIN - returning null (all departments)");
            return null; // Can access all departments
        }
        
        if ("DEPARTMENT_ADMIN".equals(adminRole) && adminUserId != null) {
            // Get departments this admin has permission for
            List<String> permittedDepartments = departmentPermissionRepository.findDepartmentCodesByUserId(adminUserId);
            logger.info("DEPARTMENT_ADMIN - permittedDepartments from DB: {}", permittedDepartments);
            
            // If no specific permissions found, fall back to user's own department for backward compatibility
            if (permittedDepartments.isEmpty()) {
                Optional<User> adminUser = userRepository.findById(adminUserId);
                if (adminUser.isPresent()) {
                    String deptCode = adminUser.get().getDepartmentCode();
                    logger.info("No permissions found, falling back to user's departmentCode: {}", deptCode);
                    if (deptCode != null) {
                        return List.of(deptCode);
                    }
                }
            }
            
            return permittedDepartments.isEmpty() ? List.of() : permittedDepartments;
        }
        
        logger.info("No accessible departments - returning empty list");
        return List.of(); // No accessible departments
    }
    
    /**
     * Get paginated list of all users with department-based filtering for department admins.
     * 
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param adminUserId ID of the admin making the request
     * @param adminRole Role of the admin making the request
     * @param adminDepartmentCode Department code of the admin (for department admins) - deprecated, use permissions instead
     * @return Page of AdminUserDto objects
     * Requirements: 2.1, 2.2 - Paginated user listing with department-based access control
     */
    public Page<AdminUserDto> getAllUsersWithDepartmentFilter(int page, int size, Long adminUserId, String adminRole, String adminDepartmentCode) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        List<String> accessibleDepartments = getAccessibleDepartmentCodes(adminUserId, adminRole);
        
        if (accessibleDepartments == null) {
            // For ADMIN and SUPER_ADMIN, show all users
            Page<User> users = userRepository.findAll(pageable);
            return users.map(this::convertToAdminUserDto);
        } else if (accessibleDepartments.isEmpty()) {
            // No accessible departments - return empty page
            return new PageImpl<>(List.of(), pageable, 0);
        } else {
            // For DEPARTMENT_ADMIN, show users from accessible departments
            Page<User> users = userRepository.findByDepartmentCodeIn(accessibleDepartments, pageable);
            return users.map(this::convertToAdminUserDto);
        }
    }
    
    /**
     * Get paginated list of all users.
     * 
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return Page of AdminUserDto objects
     * Requirements: 2.1, 2.2 - Paginated user listing
     */
    public Page<AdminUserDto> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users = userRepository.findAll(pageable);
        return users.map(this::convertToAdminUserDto);
    }
    
    /**
     * Search users with department-based filtering for department admins.
     * 
     * @param searchTerm Search term
     * @param page Page number
     * @param size Page size
     * @param adminUserId ID of the admin making the request
     * @param adminRole Role of the admin making the request
     * @param adminDepartmentCode Department code of the admin (deprecated, use permissions instead)
     * @return Page of matching AdminUserDto objects
     */
    public Page<AdminUserDto> searchUsersWithDepartmentFilter(String searchTerm, int page, int size, Long adminUserId, String adminRole, String adminDepartmentCode) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        List<String> accessibleDepartments = getAccessibleDepartmentCodes(adminUserId, adminRole);
        
        if (accessibleDepartments == null) {
            // For ADMIN and SUPER_ADMIN, search all users
            Page<User> users = userRepository.findBySearchTerm(searchTerm, pageable);
            return users.map(this::convertToAdminUserDto);
        } else if (accessibleDepartments.isEmpty()) {
            // No accessible departments - return empty page
            return new PageImpl<>(List.of(), pageable, 0);
        } else {
            // For department admins, search only within accessible departments
            Page<User> users = userRepository.findBySearchTermAndDepartmentCodeIn(searchTerm, accessibleDepartments, pageable);
            return users.map(this::convertToAdminUserDto);
        }
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
        long departmentAdminUsers = userRepository.countByRole(UserRole.DEPARTMENT_ADMIN);
        long adminUsers = userRepository.countByRole(UserRole.ADMIN);
        long superAdminUsers = userRepository.countByRole(UserRole.SUPER_ADMIN);
        long totalUsers = userRepository.count();
        
        return UserRoleStats.builder()
                .normalUsers(normalUsers)
                .departmentAdminUsers(departmentAdminUsers)
                .adminUsers(adminUsers)
                .superAdminUsers(superAdminUsers)
                .totalUsers(totalUsers)
                .build();
    }
    
    /**
     * Get list of all departments from users.
     * 
     * @return List of department codes and names
     */
    public List<DepartmentDto> getAllDepartments() {
        List<Object[]> departments = userRepository.findDistinctDepartments();
        return departments.stream()
                .map(dept -> DepartmentDto.builder()
                        .code((String) dept[0])
                        .name((String) dept[1])
                        .userCount(((Number) dept[2]).longValue())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * Get list of departments with filtering for department admins.
     * 
     * @param adminUserId ID of the admin making the request
     * @param adminRole Role of the admin making the request
     * @param adminDepartmentCode Department code of the admin (deprecated, use permissions instead)
     * @return List of department codes and names
     */
    public List<DepartmentDto> getAllDepartmentsWithFilter(Long adminUserId, String adminRole, String adminDepartmentCode) {
        List<Object[]> departments = userRepository.findDistinctDepartments();
        List<String> accessibleDepartments = getAccessibleDepartmentCodes(adminUserId, adminRole);
        
        return departments.stream()
                .filter(dept -> {
                    if (accessibleDepartments == null) {
                        return true; // ADMIN and SUPER_ADMIN can see all departments
                    } else if (accessibleDepartments.isEmpty()) {
                        return false; // No accessible departments
                    } else {
                        return accessibleDepartments.contains((String) dept[0]);
                    }
                })
                .map(dept -> DepartmentDto.builder()
                        .code((String) dept[0])
                        .name((String) dept[1])
                        .userCount(((Number) dept[2]).longValue())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * Get users by attendance status.
     * 
     * @param attendanceStatus "NO_RECORDS", "INSIDE", "OUTSIDE"
     * @param page Page number
     * @param size Page size
     * @return Page of users matching attendance status
     */
    public Page<AdminUserDto> getUsersByAttendanceStatus(String attendanceStatus, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        switch (attendanceStatus.toUpperCase()) {
            case "NO_RECORDS":
                // Users who have never made any entry/exit records
                Page<User> usersWithNoRecords = userRepository.findUsersWithNoEntryExitRecords(pageable);
                return usersWithNoRecords.map(this::convertToAdminUserDto);
                
            case "INSIDE":
                // Users who are currently inside (last record is ENTRY)
                Page<User> usersInside = userRepository.findUsersCurrentlyInside(pageable);
                return usersInside.map(this::convertToAdminUserDto);
                
            case "OUTSIDE":
                // Users who are currently outside (last record is EXIT or no records)
                Page<User> usersOutside = userRepository.findUsersCurrentlyOutside(pageable);
                return usersOutside.map(this::convertToAdminUserDto);
                
            default:
                return getAllUsers(page, size);
        }
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
        private long departmentAdminUsers;
        private long adminUsers;
        private long superAdminUsers;
        private long totalUsers;
    }
    
    /**
     * Create a new user manually by admin.
     * The user will be created with a default password (TC No's last 6 digits).
     * 
     * @param createDto User creation data
     * @param adminUserId ID of admin performing the action
     * @return Created AdminUserDto
     * @throws IllegalArgumentException if validation fails or user already exists
     */
    @Transactional
    public AdminUserDto createUserManually(ManualUserCreateDto createDto, Long adminUserId) {
        // Validate TC No
        if (createDto.getTcNo() == null || !createDto.getTcNo().matches("\\d{11}")) {
            throw new IllegalArgumentException("TC Kimlik No 11 haneli olmalıdır");
        }
        
        // Check if user already exists
        if (userRepository.findByTcNo(createDto.getTcNo()).isPresent()) {
            throw new IllegalArgumentException("Bu TC Kimlik No ile kayıtlı kullanıcı zaten mevcut");
        }
        
        // Check if personnel number already exists
        if (userRepository.findByPersonnelNo(createDto.getPersonnelNo()).isPresent()) {
            throw new IllegalArgumentException("Bu Sicil No ile kayıtlı kullanıcı zaten mevcut");
        }
        
        // Create default password or use provided password
        String password = createDto.getPassword();
        if (password == null || password.trim().isEmpty()) {
            // Default: last 6 digits of TC No
            password = createDto.getTcNo().substring(5);
        }
        String passwordHash = passwordEncoder.encode(password);
        
        // Determine role
        UserRole role = UserRole.NORMAL_USER;
        if (createDto.getRole() != null && !createDto.getRole().isEmpty()) {
            try {
                role = UserRole.valueOf(createDto.getRole());
            } catch (IllegalArgumentException e) {
                role = UserRole.NORMAL_USER;
            }
        }
        
        // Create user
        User user = User.builder()
                .tcNo(createDto.getTcNo())
                .personnelNo(createDto.getPersonnelNo())
                .firstName(createDto.getFirstName())
                .lastName(createDto.getLastName())
                .mobilePhone(createDto.getMobilePhone())
                .departmentCode(createDto.getDepartmentCode())
                .departmentName(createDto.getDepartmentName())
                .titleCode(createDto.getTitleCode())
                .passwordHash(passwordHash)
                .role(role)
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Create audit log
        AdminAuditLog auditLog = AdminAuditLog.builder()
                .adminUserId(adminUserId)
                .action("MANUAL_USER_CREATE")
                .targetUserId(savedUser.getId())
                .details(String.format("{\"tcNo\":\"%s\",\"personnelNo\":\"%s\",\"fullName\":\"%s %s\"}", 
                        createDto.getTcNo(), createDto.getPersonnelNo(), 
                        createDto.getFirstName(), createDto.getLastName()))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        adminAuditLogRepository.save(auditLog);
        
        logger.info("Manual user created: {} by admin {}", savedUser.getTcNo(), adminUserId);
        
        return convertToAdminUserDto(savedUser);
    }
    
    /**
     * Update user information by admin.
     * 
     * @param userId User ID to update
     * @param updateDto Update data
     * @param adminUserId ID of admin performing the action
     * @return Updated AdminUserDto
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public AdminUserDto updateUser(Long userId, UserUpdateDto updateDto, Long adminUserId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Kullanıcı bulunamadı");
        }
        
        User user = userOpt.get();
        StringBuilder changes = new StringBuilder();
        
        // Update fields if provided
        if (updateDto.getPersonnelNo() != null && !updateDto.getPersonnelNo().isEmpty()) {
            // Check if personnel number is already used by another user
            Optional<User> existingUser = userRepository.findByPersonnelNo(updateDto.getPersonnelNo());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                throw new IllegalArgumentException("Bu Sicil No başka bir kullanıcı tarafından kullanılıyor");
            }
            changes.append("personnelNo: ").append(user.getPersonnelNo()).append(" -> ").append(updateDto.getPersonnelNo()).append("; ");
            user.setPersonnelNo(updateDto.getPersonnelNo());
        }
        
        if (updateDto.getFirstName() != null && !updateDto.getFirstName().isEmpty()) {
            changes.append("firstName: ").append(user.getFirstName()).append(" -> ").append(updateDto.getFirstName()).append("; ");
            user.setFirstName(updateDto.getFirstName());
        }
        
        if (updateDto.getLastName() != null && !updateDto.getLastName().isEmpty()) {
            changes.append("lastName: ").append(user.getLastName()).append(" -> ").append(updateDto.getLastName()).append("; ");
            user.setLastName(updateDto.getLastName());
        }
        
        if (updateDto.getMobilePhone() != null && !updateDto.getMobilePhone().isEmpty()) {
            changes.append("mobilePhone: ").append(user.getMobilePhone()).append(" -> ").append(updateDto.getMobilePhone()).append("; ");
            user.setMobilePhone(updateDto.getMobilePhone());
        }
        
        if (updateDto.getDepartmentCode() != null) {
            changes.append("departmentCode: ").append(user.getDepartmentCode()).append(" -> ").append(updateDto.getDepartmentCode()).append("; ");
            user.setDepartmentCode(updateDto.getDepartmentCode().isEmpty() ? null : updateDto.getDepartmentCode());
        }
        
        if (updateDto.getDepartmentName() != null) {
            changes.append("departmentName: ").append(user.getDepartmentName()).append(" -> ").append(updateDto.getDepartmentName()).append("; ");
            user.setDepartmentName(updateDto.getDepartmentName().isEmpty() ? null : updateDto.getDepartmentName());
        }
        
        if (updateDto.getTitleCode() != null) {
            user.setTitleCode(updateDto.getTitleCode().isEmpty() ? null : updateDto.getTitleCode());
        }
        
        // Update password if provided
        if (updateDto.getNewPassword() != null && !updateDto.getNewPassword().isEmpty()) {
            if (updateDto.getNewPassword().length() < 6) {
                throw new IllegalArgumentException("Şifre en az 6 karakter olmalıdır");
            }
            user.setPasswordHash(passwordEncoder.encode(updateDto.getNewPassword()));
            changes.append("password: updated; ");
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);
        
        // Create audit log
        AdminAuditLog auditLog = AdminAuditLog.builder()
                .adminUserId(adminUserId)
                .action("USER_UPDATE")
                .targetUserId(userId)
                .details(changes.toString())
                .timestamp(LocalDateTime.now())
                .build();
        adminAuditLogRepository.save(auditLog);
        
        logger.info("User {} updated by admin {}", userId, adminUserId);
        
        return convertToAdminUserDto(updatedUser);
    }
    
    /**
     * Delete user by admin.
     * 
     * @param userId User ID to delete
     * @param adminUserId ID of admin performing the action
     * @throws IllegalArgumentException if user not found or cannot be deleted
     */
    @Transactional
    public void deleteUser(Long userId, Long adminUserId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Kullanıcı bulunamadı");
        }
        
        User user = userOpt.get();
        
        // Prevent deleting yourself
        if (userId.equals(adminUserId)) {
            throw new IllegalArgumentException("Kendinizi silemezsiniz");
        }
        
        // Prevent deleting SUPER_ADMIN users (only another SUPER_ADMIN can do this)
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Süper yönetici kullanıcıları silinemez");
        }
        
        String userInfo = String.format("{\"tcNo\":\"%s\",\"personnelNo\":\"%s\",\"fullName\":\"%s %s\"}", 
                user.getTcNo(), user.getPersonnelNo(), user.getFirstName(), user.getLastName());
        
        // Delete user
        userRepository.delete(user);
        
        // Create audit log
        AdminAuditLog auditLog = AdminAuditLog.builder()
                .adminUserId(adminUserId)
                .action("USER_DELETE")
                .targetUserId(userId)
                .details(userInfo)
                .timestamp(LocalDateTime.now())
                .build();
        adminAuditLogRepository.save(auditLog);
        
        logger.info("User {} deleted by admin {}", userId, adminUserId);
    }
}