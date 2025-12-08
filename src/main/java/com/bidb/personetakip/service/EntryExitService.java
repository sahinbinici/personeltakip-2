package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.EntryExitRecordDto;
import com.bidb.personetakip.model.EntryExitType;
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
     * Determines if next usage is entry or exit based on QR code usage count
     */
    EntryExitType determineEntryExitType(String qrCodeValue);
}
