package com.bidb.personetakip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for admin entry/exit record information.
 * Contains record details with user information for admin interface.
 * 
 * Requirements: 3.1, 3.2 - Entry/exit record display with user information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRecordDto {
    
    /**
     * Record ID
     */
    private Long id;
    
    /**
     * User ID who performed the entry/exit
     */
    private Long userId;
    
    /**
     * User's TC Kimlik No
     */
    private String userTcNo;
    
    /**
     * User's full name (first name + last name)
     */
    private String userFullName;
    
    /**
     * User's personnel number
     */
    private String userPersonnelNo;
    
    /**
     * User's department code
     */
    private String userDepartmentCode;
    
    /**
     * User's department name
     */
    private String userDepartmentName;
    
    /**
     * Entry/Exit type (ENTRY or EXIT)
     */
    private String type;
    
    /**
     * Display name for type (Giriş or Çıkış)
     */
    private String typeDisplayName;
    
    /**
     * Timestamp when the entry/exit occurred
     */
    private LocalDateTime timestamp;
    
    /**
     * GPS latitude coordinate
     */
    private Double latitude;
    
    /**
     * GPS longitude coordinate
     */
    private Double longitude;
    
    /**
     * QR code value used for entry/exit
     */
    private String qrCodeValue;
    
    /**
     * Whether GPS coordinates are available
     */
    private boolean hasGpsCoordinates;
    
    /**
     * IP address from which the entry/exit was performed
     */
    private String ipAddress;
    
    /**
     * Whether there is an IP address mismatch (true = mismatch, false = match, null = no assignment or unknown)
     */
    private Boolean ipMismatch;
    
    /**
     * Timestamp when the record was created
     */
    private LocalDateTime createdAt;
}