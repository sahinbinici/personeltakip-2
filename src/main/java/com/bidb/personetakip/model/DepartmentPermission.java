package com.bidb.personetakip.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing department permissions for DEPARTMENT_ADMIN users.
 * This allows a department admin to manage multiple departments.
 */
@Entity
@Table(name = "department_permissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentPermission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "department_code", nullable = false, length = 10)
    private String departmentCode;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "created_by")
    private Long createdBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "updated_by")
    private Long updatedBy;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}