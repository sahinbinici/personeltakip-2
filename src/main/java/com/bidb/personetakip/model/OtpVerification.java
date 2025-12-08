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

import java.time.LocalDateTime;

/**
 * Entity representing an OTP (One-Time Password) verification record.
 * Used during user registration to verify mobile phone ownership.
 */
@Entity
@Table(name = "otp_verifications", indexes = {
    @Index(name = "idx_tc_no", columnList = "tc_no"),
    @Index(name = "idx_expires_at", columnList = "expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Turkish Citizen ID Number (TC Kimlik No) - 11 digits
     */
    @Column(name = "tc_no", nullable = false, length = 11)
    @NotBlank(message = "TC number is required")
    @Pattern(regexp = "\\d{11}", message = "TC number must be exactly 11 digits")
    private String tcNo;
    
    /**
     * 6-digit OTP code sent via SMS
     */
    @Column(name = "otp_code", nullable = false, length = 6)
    @NotBlank(message = "OTP code is required")
    @Size(min = 6, max = 6, message = "OTP code must be exactly 6 characters")
    @Pattern(regexp = "\\d{6}", message = "OTP code must be 6 digits")
    private String otpCode;
    
    /**
     * Expiration timestamp for the OTP (typically 5 minutes from creation)
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    /**
     * Flag indicating whether the OTP has been successfully verified
     */
    @Column(name = "verified", nullable = false)
    @Builder.Default
    private Boolean verified = false;
    
    /**
     * Timestamp when the OTP was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Check if the OTP has expired
     * @return true if current time is after expiration time
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Check if the OTP is valid (not expired and not yet verified)
     * @return true if OTP can still be used for verification
     */
    public boolean isValid() {
        return !isExpired() && !verified;
    }
}
