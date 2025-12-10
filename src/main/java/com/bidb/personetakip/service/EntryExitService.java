package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.EntryExitRecordDto;
import com.bidb.personetakip.model.EntryExitType;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

public interface EntryExitService {
    /**
     * Records entry or exit event
     */
    EntryExitRecordDto recordEntryExit(
        Long userId,
        String qrCodeValue,
        LocalDateTime timestamp,
        Double latitude,
        Double longitude
    );
    
    /**
     * Records entry or exit event with IP address capture
     * Requirements: 1.1, 6.2, 6.3
     */
    EntryExitRecordDto recordEntryExit(
        Long userId,
        String qrCodeValue,
        LocalDateTime timestamp,
        Double latitude,
        Double longitude,
        HttpServletRequest request
    );
    
    /**
     * Determines if next usage is entry or exit based on QR code usage count
     */
    EntryExitType determineEntryExitType(String qrCodeValue);
}
