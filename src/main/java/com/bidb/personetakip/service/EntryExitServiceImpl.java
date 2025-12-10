package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.EntryExitRecordDto;
import com.bidb.personetakip.dto.QrCodeValidationDto;
import com.bidb.personetakip.exception.ValidationException;
import com.bidb.personetakip.model.EntryExitRecord;
import com.bidb.personetakip.model.EntryExitType;
import com.bidb.personetakip.model.QrCode;
import com.bidb.personetakip.repository.EntryExitRecordRepository;
import com.bidb.personetakip.repository.QrCodeRepository;
import com.bidb.personetakip.config.IpTrackingConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EntryExitServiceImpl implements EntryExitService {
    
    private final EntryExitRecordRepository entryExitRecordRepository;
    private final QrCodeRepository qrCodeRepository;
    private final QrCodeService qrCodeService;
    private final IpAddressService ipAddressService;
    private final IpTrackingConfig ipTrackingConfig;
    
    public EntryExitServiceImpl(
            EntryExitRecordRepository entryExitRecordRepository,
            QrCodeRepository qrCodeRepository,
            QrCodeService qrCodeService,
            IpAddressService ipAddressService,
            IpTrackingConfig ipTrackingConfig) {
        this.entryExitRecordRepository = entryExitRecordRepository;
        this.qrCodeRepository = qrCodeRepository;
        this.qrCodeService = qrCodeService;
        this.ipAddressService = ipAddressService;
        this.ipTrackingConfig = ipTrackingConfig;
    }
    
    @Override
    @Transactional
    public EntryExitRecordDto recordEntryExit(
            Long userId,
            String qrCodeValue,
            LocalDateTime timestamp,
            Double latitude,
            Double longitude) {
        
        // Call the enhanced method with null request (backward compatibility)
        return recordEntryExit(userId, qrCodeValue, timestamp, latitude, longitude, null);
    }
    
    @Override
    @Transactional
    public EntryExitRecordDto recordEntryExit(
            Long userId,
            String qrCodeValue,
            LocalDateTime timestamp,
            Double latitude,
            Double longitude,
            HttpServletRequest request) {
        
        // Validate GPS coordinates
        validateGpsCoordinates(latitude, longitude);
        
        // Validate QR code ownership and validity
        QrCodeValidationDto validation = qrCodeService.validateQrCode(qrCodeValue, userId);
        if (!validation.valid()) {
            throw new ValidationException(validation.message());
        }
        
        // Determine entry/exit type
        EntryExitType type = determineEntryExitType(qrCodeValue);
        
        // Capture IP address gracefully - Requirements: 1.1, 6.2, 6.5
        String ipAddress = null;
        try {
            // Check if IP tracking is enabled - Requirements: 6.5
            if (ipTrackingConfig.isEnabled() && request != null) {
                ipAddress = ipAddressService.extractClientIpAddress(request);
            }
        } catch (Exception e) {
            // Graceful IP capture failure handling - Requirements: 6.2
            // Log the error but continue with the operation
            // IP address will remain null, which is acceptable
        }
        
        // Create and save entry/exit record
        EntryExitRecord record = new EntryExitRecord();
        record.setUserId(userId);
        record.setType(type);
        record.setTimestamp(timestamp);
        record.setLatitude(latitude);
        record.setLongitude(longitude);
        record.setQrCodeValue(qrCodeValue);
        record.setIpAddress(ipAddress); // IP tracking integration - Requirements: 1.1
        
        EntryExitRecord savedRecord = entryExitRecordRepository.save(record);
        
        // Increment QR code usage count after successful recording - Requirements: 6.3
        qrCodeService.incrementUsageCount(qrCodeValue);
        
        return new EntryExitRecordDto(
            savedRecord.getId(),
            savedRecord.getUserId(),
            savedRecord.getType(),
            savedRecord.getTimestamp(),
            savedRecord.getLatitude(),
            savedRecord.getLongitude()
        );
    }
    
    @Override
    public EntryExitType determineEntryExitType(String qrCodeValue) {
        QrCode qrCode = qrCodeRepository.findByQrCodeValue(qrCodeValue)
            .orElseThrow(() -> new ValidationException("QR code not found"));
        
        // If usage count is 0, next usage is ENTRY
        // If usage count is 1, next usage is EXIT
        return qrCode.getUsageCount() == 0 ? EntryExitType.ENTRY : EntryExitType.EXIT;
    }
    
    private void validateGpsCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new ValidationException("GPS coordinates are required");
        }
        
        // Validate latitude range: -90 to 90
        if (latitude < -90.0 || latitude > 90.0) {
            throw new ValidationException(
                String.format("Latitude must be between -90 and 90, got: %.8f", latitude)
            );
        }
        
        // Validate longitude range: -180 to 180
        if (longitude < -180.0 || longitude > 180.0) {
            throw new ValidationException(
                String.format("Longitude must be between -180 and 180, got: %.8f", longitude)
            );
        }
    }
}
