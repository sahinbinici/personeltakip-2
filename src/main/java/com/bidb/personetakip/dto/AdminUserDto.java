package com.bidb.personetakip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for admin user management operations.
 * Contains extended user information including last login and activity.
 * 
 * Requirements: 2.2 - User information display in admin interface
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserDto {
    
    /**
     * User ID
     */
    private Long id;
    
    /**
     * Turkish Citizen ID Number
     */
    private String tcNo;
    
    /**
     * Personnel/Employee number
     */
    private String personnelNo;
    
    /**
     * First name
     */
    private String firstName;
    
    /**
     * Last name
     */
    private String lastName;
    
    /**
     * Mobile phone number
     */
    private String mobilePhone;
    
    /**
     * Department code
     */
    private String departmentCode;
    
    /**
     * Department name
     */
    private String departmentName;
    
    /**
     * Title code
     */
    private String titleCode;
    
    /**
     * User role (NORMAL_USER, ADMIN, SUPER_ADMIN)
     */
    private String role;
    
    /**
     * Last login timestamp
     */
    private LocalDateTime lastLoginAt;
    
    /**
     * Account creation timestamp
     */
    private LocalDateTime createdAt;
    
    /**
     * Last update timestamp
     */
    private LocalDateTime updatedAt;
    
    /**
     * Get full name (first + last name)
     */
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return "Bilinmeyen Kullanıcı";
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
    
    /**
     * Get role display name in Turkish
     */
    public String getRoleDisplayName() {
        switch (role) {
            case "NORMAL_USER":
                return "Normal Kullanıcı";
            case "ADMIN":
                return "Yönetici";
            case "SUPER_ADMIN":
                return "Süper Yönetici";
            default:
                return role;
        }
    }
}