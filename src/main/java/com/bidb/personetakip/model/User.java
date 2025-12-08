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
}
