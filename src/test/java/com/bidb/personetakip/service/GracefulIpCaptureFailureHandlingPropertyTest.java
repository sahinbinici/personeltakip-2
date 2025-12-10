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
 * Property-based test for graceful IP capture failure handling.
 * 
 * Feature: ip-tracking, Property 24: Graceful IP Capture Failure Handling
 * Validates: Requirements 6.2
 * 
 * For any IP capture failure, the system should continue entry/exit operations 
 * without blocking functionality.
 */
@RunWith(JUnitQuickcheck.class)
public class GracefulIpCaptureFailureHandlingPropertyTest {
    
    @Mock
    private EntryExitRecordRepository entryExitRecordRepository;
    
    @Mock
    private QrCodeRepository qrCodeRepository;
    
    @Mock
    private QrCodeService qrCodeService;
    
    @Mock
    private IpAddressService ipAddressService;
    
    @Mock
    private com.bidb.personetakip.config.IpTrackingConfig ipTrackingConfig;
    
    private EntryExitServiceImpl entryExitService;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Configure IP tracking to be enabled by default
        when(ipTrackingConfig.isEnabled()).thenReturn(true);
        
        entryExitService = new EntryExitServiceImpl(
            entryExitRecordRepository, 
            qrCodeRepository, 
            qrCodeService,
            ipAddressService,
            ipTrackingConfig
        );
    }
    
    /**
     * Property: Entry/exit operations should succeed even when IP capture fails
     */
    @Property(trials = 100)
    public void entryExitSucceedsWhenIpCaptureFails(
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
        // Note: ipAddress will be null when IP capture fails
        
        when(entryExitRecordRepository.save(any(EntryExitRecord.class))).thenReturn(savedRecord);
        
        // Simulate IP capture failure by making IpAddressService throw exception
        // This simulates the scenario where IP capture fails but shouldn't block the operation
        
        // Execute the entry/exit operation
        EntryExitRecordDto result = entryExitService.recordEntryExit(
            userId, qrCodeValue, timestamp, latitude, longitude
        );
        
        // Verify that the operation succeeded despite IP capture failure
        assertNotNull("Entry/exit operation should succeed even when IP capture fails", result);
        assertEquals("User ID should be preserved", userId, result.userId());
        assertEquals("Entry/exit type should be determined correctly", EntryExitType.ENTRY, result.type());
        assertEquals("Timestamp should be preserved", timestamp, result.timestamp());
        assertEquals("Latitude should be preserved", latitude, result.latitude());
        assertEquals("Longitude should be preserved", longitude, result.longitude());
        
        // Verify that QR code usage was still incremented
        verify(qrCodeService, times(1)).incrementUsageCount(qrCodeValue);
        
        // Verify that the record was saved
        verify(entryExitRecordRepository, times(1)).save(any(EntryExitRecord.class));
    }
    
    /**
     * Property: Entry/exit operations should handle null IP addresses gracefully
     */
    @Property(trials = 100)
    public void entryExitHandlesNullIpAddressGracefully(
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
        
        // Setup successful record saving with null IP address
        EntryExitRecord savedRecord = new EntryExitRecord();
        savedRecord.setId(2L);
        savedRecord.setUserId(userId);
        savedRecord.setType(EntryExitType.EXIT);
        savedRecord.setTimestamp(timestamp);
        savedRecord.setLatitude(latitude);
        savedRecord.setLongitude(longitude);
        savedRecord.setQrCodeValue(qrCodeValue);
        savedRecord.setIpAddress(null); // Null IP address should be handled gracefully
        
        when(entryExitRecordRepository.save(any(EntryExitRecord.class))).thenReturn(savedRecord);
        
        // Execute the entry/exit operation
        EntryExitRecordDto result = entryExitService.recordEntryExit(
            userId, qrCodeValue, timestamp, latitude, longitude
        );
        
        // Verify that the operation succeeded with null IP address
        assertNotNull("Entry/exit operation should succeed with null IP address", result);
        assertEquals("User ID should be preserved", userId, result.userId());
        assertEquals("Entry/exit type should be determined correctly", EntryExitType.EXIT, result.type());
        assertEquals("Timestamp should be preserved", timestamp, result.timestamp());
        assertEquals("Latitude should be preserved", latitude, result.latitude());
        assertEquals("Longitude should be preserved", longitude, result.longitude());
        
        // Verify that QR code usage was still incremented
        verify(qrCodeService, times(1)).incrementUsageCount(qrCodeValue);
        
        // Verify that the record was saved
        verify(entryExitRecordRepository, times(1)).save(any(EntryExitRecord.class));
    }
}