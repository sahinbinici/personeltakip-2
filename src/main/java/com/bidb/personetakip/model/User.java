package com.bidb.personetakip.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.stream.Collectors;

/**
 * Entity representing a user in the personnel tracking system.
 * Stores user credentials, personal information, and role.
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_tc_no", columnList = "tc_no"),
    @Index(name = "idx_personnel_no", columnList = "personnel_no")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Turkish Citizen ID Number (TC Kimlik No) - 11 digits, unique identifier
     */
    @Column(name = "tc_no", unique = true, nullable = false, length = 11)
    @NotBlank(message = "TC number is required")
    @Pattern(regexp = "\\d{11}", message = "TC number must be exactly 11 digits")
    private String tcNo;
    
    /**
     * Personnel/Employee Number (Sicil No)
     */
    @Column(name = "personnel_no", nullable = false, length = 20)
    @NotBlank(message = "Personnel number is required")
    @Size(max = 20, message = "Personnel number must not exceed 20 characters")
    private String personnelNo;
    
    /**
     * First name of the user
     */
    @Column(name = "first_name", nullable = false, length = 100)
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;
    
    /**
     * Last name of the user
     */
    @Column(name = "last_name", nullable = false, length = 100)
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;
    
    /**
     * Mobile phone number
     */
    @Column(name = "mobile_phone", nullable = false, length = 15)
    @NotBlank(message = "Mobile phone is required")
    @Size(max = 15, message = "Mobile phone must not exceed 15 characters")
    private String mobilePhone;
    
    /**
     * Department code from external database
     */
    @Column(name = "department_code", length = 10)
    private String departmentCode;
    
    /**
     * Department name from external database
     */
    @Column(name = "department_name", length = 200)
    private String departmentName;
    
    /**
     * Title code from external database
     */
    @Column(name = "title_code", length = 10)
    private String titleCode;
    
    /**
     * Comma-separated list of assigned IP addresses for this user
     * Used for IP compliance checking and mismatch detection
     */
    @Column(name = "assigned_ip_addresses", columnDefinition = "TEXT")
    private String assignedIpAddresses;
    
    /**
     * Last login timestamp
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    /**
     * BCrypt hashed password
     */
    @Column(name = "password_hash", nullable = false)
    @NotBlank(message = "Password hash is required")
    private String passwordHash;
    
    /**
     * User role in the system
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;
    
    /**
     * Timestamp when the user was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when the user was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Pre-persist callback to set default role if not specified
     */
    @PrePersist
    protected void onCreate() {
        if (role == null) {
            role = UserRole.NORMAL_USER;
        }
    }
    
    // IP Address Assignment Methods
    
    /**
     * IPv4 address pattern for validation
     */
    private static final java.util.regex.Pattern IPV4_PATTERN = java.util.regex.Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    
    /**
     * IPv6 address pattern for validation (simplified)
     */
    private static final java.util.regex.Pattern IPV6_PATTERN = java.util.regex.Pattern.compile(
        "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|" +
        "^::1$|^::$|" +
        "^([0-9a-fA-F]{1,4}:){1,7}:$|" +
        "^([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}$|" +
        "^([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}$|" +
        "^([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}$|" +
        "^([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}$|" +
        "^([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}$|" +
        "^[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})$|" +
        "^:((:[0-9a-fA-F]{1,4}){1,7}|:)$"
    );
    
    /**
     * Parse assigned IP addresses from the stored string format
     * @return List of assigned IP addresses, empty list if none assigned
     */
    public List<String> getAssignedIpAddressesList() {
        if (assignedIpAddresses == null || assignedIpAddresses.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        return Arrays.stream(assignedIpAddresses.split("[,;]"))
                .map(String::trim)
                .filter(ip -> !ip.isEmpty())
                .collect(Collectors.toList());
    }
    
    /**
     * Set assigned IP addresses from a list
     * @param ipAddresses List of IP addresses to assign
     */
    public void setAssignedIpAddressesList(List<String> ipAddresses) {
        if (ipAddresses == null || ipAddresses.isEmpty()) {
            this.assignedIpAddresses = null;
        } else {
            this.assignedIpAddresses = ipAddresses.stream()
                    .map(String::trim)
                    .filter(ip -> !ip.isEmpty())
                    .collect(Collectors.joining(","));
        }
    }
    
    /**
     * Validate IP address format (supports both IPv4 and IPv6)
     * @param ipAddress IP address to validate
     * @return true if valid IPv4 or IPv6 address
     */
    public static boolean isValidIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return false;
        }
        
        String trimmedIp = ipAddress.trim();
        return IPV4_PATTERN.matcher(trimmedIp).matches() || 
               IPV6_PATTERN.matcher(trimmedIp).matches();
    }
    
    /**
     * Validate all assigned IP addresses
     * @return true if all assigned IP addresses are valid
     */
    public boolean hasValidAssignedIpAddresses() {
        List<String> ipList = getAssignedIpAddressesList();
        return ipList.stream().allMatch(User::isValidIpAddress);
    }
    
    /**
     * Add an IP address to the assigned list
     * @param ipAddress IP address to add
     * @return true if added successfully, false if invalid or already exists
     */
    public boolean addAssignedIpAddress(String ipAddress) {
        if (!isValidIpAddress(ipAddress)) {
            return false;
        }
        
        List<String> currentIps = getAssignedIpAddressesList();
        String trimmedIp = ipAddress.trim();
        
        if (currentIps.contains(trimmedIp)) {
            return false; // Already exists
        }
        
        currentIps.add(trimmedIp);
        setAssignedIpAddressesList(currentIps);
        return true;
    }
    
    /**
     * Remove an IP address from the assigned list
     * @param ipAddress IP address to remove
     * @return true if removed successfully, false if not found
     */
    public boolean removeAssignedIpAddress(String ipAddress) {
        if (ipAddress == null) {
            return false;
        }
        
        List<String> currentIps = getAssignedIpAddressesList();
        String trimmedIp = ipAddress.trim();
        
        boolean removed = currentIps.remove(trimmedIp);
        if (removed) {
            setAssignedIpAddressesList(currentIps);
        }
        
        return removed;
    }
    
    /**
     * Check if a specific IP address is assigned to this user
     * @param ipAddress IP address to check
     * @return true if the IP address is assigned to this user
     */
    public boolean hasAssignedIpAddress(String ipAddress) {
        if (ipAddress == null) {
            return false;
        }
        
        return getAssignedIpAddressesList().contains(ipAddress.trim());
    }
    
    /**
     * Clear all assigned IP addresses
     */
    public void clearAssignedIpAddresses() {
        this.assignedIpAddresses = null;
    }
}
