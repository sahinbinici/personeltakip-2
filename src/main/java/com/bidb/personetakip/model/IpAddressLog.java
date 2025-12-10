package com.bidb.personetakip.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing IP address access and modification audit logs.
 * Tracks all IP-related operations for security and compliance purposes.
 */
@Entity
@Table(name = "ip_address_logs", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_action", columnList = "action")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpAddressLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Reference to the user whose IP data was accessed or modified
     */
    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID is required")
    private Long userId;
    
    /**
     * IP address that was accessed, assigned, or removed
     */
    @Column(name = "ip_address", length = 45)
    @Size(max = 45, message = "IP address must not exceed 45 characters")
    private String ipAddress;
    
    /**
     * Type of action performed on the IP address
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    @NotNull(message = "Action is required")
    private IpAddressAction action;
    
    /**
     * Reference to the admin user who performed the action (if applicable)
     */
    @Column(name = "admin_user_id")
    private Long adminUserId;
    
    /**
     * Timestamp when the action was performed
     */
    @Column(name = "timestamp", nullable = false)
    @NotNull(message = "Timestamp is required")
    private LocalDateTime timestamp;
    
    /**
     * Additional details about the action in JSON format
     */
    @Column(name = "details", columnDefinition = "JSON")
    private String details;
    
    /**
     * Timestamp when the log entry was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Pre-persist callback to set timestamp if not specified
     */
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}