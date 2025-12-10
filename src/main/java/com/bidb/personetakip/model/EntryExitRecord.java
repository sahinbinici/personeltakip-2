package com.bidb.personetakip.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing an entry or exit event record.
 * Stores personnel movement data with timestamp and GPS location.
 */
@Entity
@Table(name = "entry_exit_records", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_qr_code", columnList = "qr_code_value"),
    @Index(name = "idx_ip_address", columnList = "ip_address")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntryExitRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Reference to the user who performed the entry/exit
     */
    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID is required")
    private Long userId;
    
    /**
     * Type of event (ENTRY or EXIT)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    @NotNull(message = "Entry/Exit type is required")
    private EntryExitType type;
    
    /**
     * Timestamp when the entry/exit occurred
     */
    @Column(name = "timestamp", nullable = false)
    @NotNull(message = "Timestamp is required")
    private LocalDateTime timestamp;
    
    /**
     * GPS latitude coordinate (-90 to 90 degrees)
     */
    @Column(name = "latitude")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90 degrees")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90 degrees")
    private Double latitude;
    
    /**
     * GPS longitude coordinate (-180 to 180 degrees)
     */
    @Column(name = "longitude")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180 degrees")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180 degrees")
    private Double longitude;
    
    /**
     * QR code value that was used for this entry/exit
     */
    @Column(name = "qr_code_value", nullable = false)
    @NotBlank(message = "QR code value is required")
    private String qrCodeValue;
    
    /**
     * IP address from which the entry/exit was performed
     * Supports both IPv4 and IPv6 formats, null for unknown/unavailable
     */
    @Column(name = "ip_address", length = 45)
    @Size(max = 45, message = "IP address must not exceed 45 characters")
    private String ipAddress;
    
    /**
     * Timestamp when the record was created in the database
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Check if GPS coordinates are present
     * @return true if both latitude and longitude are not null
     */
    public boolean hasGpsCoordinates() {
        return latitude != null && longitude != null;
    }
    
    /**
     * Validate GPS coordinates are within valid ranges
     * @return true if coordinates are valid or not present
     */
    public boolean hasValidGpsCoordinates() {
        if (!hasGpsCoordinates()) {
            return true; // No coordinates is valid
        }
        return latitude >= -90.0 && latitude <= 90.0 
            && longitude >= -180.0 && longitude <= 180.0;
    }
}
