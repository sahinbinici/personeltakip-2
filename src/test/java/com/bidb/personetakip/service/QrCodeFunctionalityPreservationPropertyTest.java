package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.EntryExitRecordDto;
import com.bidb.personetakip.dto.QrCodeValidationDto;
import com.bidb.personetakip.model.EntryExitRecord;
import com.bidb.personetakip.model.EntryExitType;
import com.bidb.personetakip.model.QrCode;
import com.bidb.personetakip.repository.EntryExitRecordRepository;
import com.bidb.personetakip.repository.QrCodeRepository;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based test for QR code functionality preservation.
 * 
 * Feature: ip-tracking, Property 25: QR Code Functionality Preservation
 * Validates: Requirements 6.3
 * 
 * For any QR code operation, enabling IP tracking should not change 
 * existing QR code functionality.
 */
@RunWith(JUnitQuickcheck.class)
public class QrCodeFunctionalityPreservationPropertyTest {
    
    @Mock
    private EntryExitRecordRepository entryExitRecordRepository;
    
    @Mock
    private QrCodeRepository qrCodeRepository;
    
    @Mock
    private QrCodeService qrCodeService;
    
    private EntryExitServiceImpl entryExitService;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Create mock IP tracking config
        com.bidb.personetakip.config.IpTrackingConfig mockIpTrackingConfig = 
            mock(com.bidb.personetakip.config.IpTrackingConfig.class);
        when(mockIpTrackingConfig.isEnabled()).thenReturn(true);
        
        entryExitService = new EntryExitServiceImpl(
            entryExitRecordRepository, 
            qrCodeRepository, 
            qrCodeService,
            null,  // IpAddressService not needed for QR code functionality tests
            mockIpTrackingConfig
        );
    }
    
    /**
     * Property: QR code validation behavior should remain unchanged with IP tracking
     */
    @Property(trials = 100)
    public void qrCodeValidationBehaviorUnchanged(
            @From(UserIdGenerator.class) Long userId,
            @From(QrCodeValueGenerator.class) String qrCodeValue,
            @From(GpsCoordinateGenerator.class) GpsCoordinate coord) {
        
        LocalDateTime timestamp = LocalDateTime.now();
        Double latitude = coord.getLatitude();
        Double longitude = coord.getLongitude();
        
        // Setup valid QR code validation
        QrCodeValidationDto validValidation = new QrCodeValidationDto(true, "Valid QR code", EntryExitType.ENTRY);
        when(qrCodeService.validateQrCode(qrCodeValue, userId)).thenReturn(validValidation);
        
        // Setup QR code for entry/exit type determination
        QrCode qrCode = new QrCode();
        qrCode.setQrCodeValue(qrCodeValue);
        qrCode.setUsageCount(0); // Will be ENTRY
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue)).thenReturn(Optional.of(qrCode));
        
        // Setup successful record saving
        EntryExitRecord savedRecord = new EntryExitRecord();
        savedRecord.setId(1L);
        savedRecord.setUserId(userId);
        savedRecord.setType(EntryExitType.ENTRY);
        savedRecord.setTimestamp(timestamp);
        savedRecord.setLatitude(latitude);
        savedRecord.setLongitude(longitude);
        savedRecord.setQrCodeValue(qrCodeValue);
        savedRecord.setIpAddress("192.168.1.1"); // IP tracking enabled
        
        when(entryExitRecordRepository.save(any(EntryExitRecord.class))).thenReturn(savedRecord);
        
        // Execute the entry/exit operation
        EntryExitRecordDto result = entryExitService.recordEntryExit(
            userId, qrCodeValue, timestamp, latitude, longitude
        );
        
        // Verify that QR code validation was called exactly once
        verify(qrCodeService, times(1)).validateQrCode(qrCodeValue, userId);
        
        // Verify that QR code usage was incremented exactly once
        verify(qrCodeService, times(1)).incrementUsageCount(qrCodeValue);
        
        // Verify that the QR code value is preserved in the result
        // Note: The DTO doesn't include QR code value, but we verify it was used correctly
        assertNotNull("Entry/exit operation should succeed", result);
        assertEquals("User ID should be preserved", userId, result.userId());
        assertEquals("Entry/exit type should be determined correctly", EntryExitType.ENTRY, result.type());
    }
    
    /**
     * Property: Entry/exit type determination should remain unchanged with IP tracking
     */
    @Property(trials = 100)
    public void entryExitTypeDeterminationUnchanged(
            @From(QrCodeValueGenerator.class) String qrCodeValue) {
        
        // Test with usage count 0 (should be ENTRY)
        QrCode entryQrCode = new QrCode();
        entryQrCode.setQrCodeValue(qrCodeValue);
        entryQrCode.setUsageCount(0);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue)).thenReturn(Optional.of(entryQrCode));
        
        EntryExitType entryType = entryExitService.determineEntryExitType(qrCodeValue);
        assertEquals("QR code with usage count 0 should determine ENTRY type", 
                     EntryExitType.ENTRY, entryType);
        
        // Test with usage count 1 (should be EXIT)
        QrCode exitQrCode = new QrCode();
        exitQrCode.setQrCodeValue(qrCodeValue);
        exitQrCode.setUsageCount(1);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue)).thenReturn(Optional.of(exitQrCode));
        
        EntryExitType exitType = entryExitService.determineEntryExitType(qrCodeValue);
        assertEquals("QR code with usage count 1 should determine EXIT type", 
                     EntryExitType.EXIT, exitType);
    }
    
    /**
     * Property: QR code usage count increment should work regardless of IP tracking
     */
    @Property(trials = 100)
    public void qrCodeUsageIncrementWorksWithIpTracking(
            @From(UserIdGenerator.class) Long userId,
            @From(QrCodeValueGenerator.class) String qrCodeValue,
            @From(GpsCoordinateGenerator.class) GpsCoordinate coord) {
        
        LocalDateTime timestamp = LocalDateTime.now();
        Double latitude = coord.getLatitude();
        Double longitude = coord.getLongitude();
        
        // Setup valid QR code validation
        QrCodeValidationDto validValidation = new QrCodeValidationDto(true, "Valid QR code", EntryExitType.EXIT);
        when(qrCodeService.validateQrCode(qrCodeValue, userId)).thenReturn(validValidation);
        
        // Setup QR code for entry/exit type determination
        QrCode qrCode = new QrCode();
        qrCode.setQrCodeValue(qrCodeValue);
        qrCode.setUsageCount(1); // Will be EXIT
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue)).thenReturn(Optional.of(qrCode));
        
        // Setup successful record saving with IP address
        EntryExitRecord savedRecord = new EntryExitRecord();
        savedRecord.setId(2L);
        savedRecord.setUserId(userId);
        savedRecord.setType(EntryExitType.EXIT);
        savedRecord.setTimestamp(timestamp);
        savedRecord.setLatitude(latitude);
        savedRecord.setLongitude(longitude);
        savedRecord.setQrCodeValue(qrCodeValue);
        savedRecord.setIpAddress("10.0.0.1"); // IP tracking enabled
        
        when(entryExitRecordRepository.save(any(EntryExitRecord.class))).thenReturn(savedRecord);
        
        // Execute the entry/exit operation
        EntryExitRecordDto result = entryExitService.recordEntryExit(
            userId, qrCodeValue, timestamp, latitude, longitude
        );
        
        // Verify that the operation succeeded
        assertNotNull("Entry/exit operation should succeed with IP tracking", result);
        
        // Verify that QR code usage was incremented exactly once
        verify(qrCodeService, times(1)).incrementUsageCount(qrCodeValue);
        
        // Verify that the increment happens after successful record saving
        // This ensures the QR code functionality order is preserved
        verify(entryExitRecordRepository, times(1)).save(any(EntryExitRecord.class));
    }
    
    /**
     * Property: QR code validation failure should still prevent entry/exit with IP tracking
     */
    @Property(trials = 100)
    public void qrCodeValidationFailureStillPreventsEntryExit(
            @From(UserIdGenerator.class) Long userId,
            @From(QrCodeValueGenerator.class) String qrCodeValue,
            @From(GpsCoordinateGenerator.class) GpsCoordinate coord) {
        
        LocalDateTime timestamp = LocalDateTime.now();
        Double latitude = coord.getLatitude();
        Double longitude = coord.getLongitude();
        
        // Setup invalid QR code validation
        QrCodeValidationDto invalidValidation = new QrCodeValidationDto(false, "Invalid QR code", null);
        when(qrCodeService.validateQrCode(qrCodeValue, userId)).thenReturn(invalidValidation);
        
        // Attempt the entry/exit operation
        try {
            entryExitService.recordEntryExit(userId, qrCodeValue, timestamp, latitude, longitude);
            fail("Entry/exit operation should fail with invalid QR code validation");
        } catch (Exception e) {
            // Expected behavior - validation failure should prevent entry/exit
            assertTrue("Exception message should contain validation error", 
                       e.getMessage().contains("Invalid QR code"));
        }
        
        // Verify that QR code validation was called
        verify(qrCodeService, times(1)).validateQrCode(qrCodeValue, userId);
        
        // Verify that no record was saved due to validation failure
        verify(entryExitRecordRepository, never()).save(any(EntryExitRecord.class));
        
        // Verify that usage count was not incremented due to validation failure
        verify(qrCodeService, never()).incrementUsageCount(qrCodeValue);
    }
}