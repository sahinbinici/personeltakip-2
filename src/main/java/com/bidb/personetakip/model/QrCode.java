package com.bidb.personetakip.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a daily QR code for a user.
 * Each user gets one unique QR code per day, usable twice (entry and exit).
 */
@Entity
@Table(name = "qr_codes", 
    indexes = {
        @Index(name = "idx_user_date", columnList = "user_id, valid_date"),
        @Index(name = "idx_qr_value", columnList = "qr_code_value")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_qr_code_value", columnNames = "qr_code_value")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrCode {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Reference to the user who owns this QR code
     */
    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID is required")
    private Long userId;
    
    /**
     * Unique QR code value (generated from userId, date, and random salt)
     */
    @Column(name = "qr_code_value", unique = true, nullable = false)
    @NotBlank(message = "QR code value is required")
    private String qrCodeValue;
    
    /**
     * Date for which this QR code is valid (00:00 to 23:59)
     */
    @Column(name = "valid_date", nullable = false)
    @NotNull(message = "Valid date is required")
    private LocalDate validDate;
    
    /**
     * Number of times this QR code has been used (0, 1, or 2 in normal mode, unlimited in dev mode)
     * 0 = unused, 1 = used for entry, 2 = used for entry and exit
     */
    @Column(name = "usage_count", nullable = false)
    @Min(value = 0, message = "Usage count cannot be negative")
    // Note: Max validation removed to allow development mode with unlimited usage
    @Builder.Default
    private Integer usageCount = 0;
    
    /**
     * Timestamp when the QR code was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Version field for optimistic locking
     */
    @Version
    @Column(name = "version")
    private Long version;
    
    /**
     * Maximum number of times a QR code can be used
     */
    public static final int MAX_USAGE = 2;
    
    /**
     * Check if the QR code is still valid for the current date
     * @return true if valid date matches today
     */
    public boolean isValidForToday() {
        return validDate.equals(LocalDate.now());
    }
    
    /**
     * Check if the QR code has reached its usage limit
     * @param maxUsage Maximum allowed usage count (configurable)
     * @return true if usage count is less than maximum
     */
    public boolean canBeUsed(int maxUsage) {
        return usageCount < maxUsage;
    }
    
    /**
     * Check if the QR code has reached its usage limit (legacy method for backward compatibility)
     * @return true if usage count is less than default maximum (2)
     */
    public boolean canBeUsed() {
        return canBeUsed(MAX_USAGE);
    }
    
    /**
     * Check if the QR code is valid and can be used
     * @param maxUsage Maximum allowed usage count (configurable)
     * @return true if valid for today and not at usage limit
     */
    public boolean isValid(int maxUsage) {
        return isValidForToday() && canBeUsed(maxUsage);
    }
    
    /**
     * Check if the QR code is valid and can be used (legacy method for backward compatibility)
     * @return true if valid for today and not at usage limit
     */
    public boolean isValid() {
        return isValid(MAX_USAGE);
    }
    
    /**
     * Increment the usage count by 1
     * @param maxUsage Maximum allowed usage count (configurable)
     * @throws IllegalStateException if already at maximum usage
     */
    public void incrementUsage(int maxUsage) {
        if (usageCount >= maxUsage) {
            throw new IllegalStateException("QR code has already been used maximum times (" + maxUsage + ")");
        }
        usageCount++;
    }
    
    /**
     * Increment the usage count by 1 (legacy method for backward compatibility)
     * @throws IllegalStateException if already at maximum usage
     */
    public void incrementUsage() {
        incrementUsage(MAX_USAGE);
    }
}
