package com.bidb.personetakip.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for tracking administrative actions in the system.
 * Provides audit trail for admin operations like role changes, user updates, etc.
 * 
 * Requirements: 4.3 - Audit logging for administrative actions
 */
@Entity
@Table(name = "admin_audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * ID of the admin user who performed the action
     */
    @Column(name = "admin_user_id", nullable = false)
    private Long adminUserId;
    
    /**
     * Type of action performed (e.g., "ROLE_CHANGE", "USER_UPDATE", "RECORD_EXPORT")
     */
    @Column(name = "action", nullable = false, length = 50)
    private String action;
    
    /**
     * ID of the target user affected by the action (if applicable)
     */
    @Column(name = "target_user_id")
    private Long targetUserId;
    
    /**
     * JSON details of the action (e.g., old/new values, parameters)
     */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
    
    /**
     * Timestamp when the action was performed
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    /**
     * Set timestamp before persisting
     */
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}