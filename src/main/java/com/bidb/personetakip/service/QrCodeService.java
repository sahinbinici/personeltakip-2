package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.QrCodeDto;
import com.bidb.personetakip.dto.QrCodeValidationDto;

public interface QrCodeService {
    /**
     * Generates or retrieves daily QR code for user
     */
    QrCodeDto getDailyQrCode(Long userId);
    
    /**
     * Validates QR code for usage
     */
    QrCodeValidationDto validateQrCode(String qrCodeValue, Long userId);
    
    /**
     * Increments usage count
     */
    void incrementUsageCount(String qrCodeValue);
    
    /**
     * Generates QR code image
     */
    byte[] generateQrCodeImage(String qrCodeValue);
    
    /**
     * Resets QR code usage count for development (development mode only)
     */
    void resetUsageCount(Long userId);
}
