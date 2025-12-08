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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test for entry/exit record completeness.
 * 
 * Feature: personnel-tracking-system, Property 17: Entry/exit record completeness
 * Validates: Requirements 8.5, 12.1
 * 
 * For any successful entry/exit event, the stored record should contain all required fields: 
 * Personnel ID, Type, Timestamp, GPS Latitude, GPS Longitude, and QR Code Value.
 */
@RunWith(JUnitQuickcheck.class)
public class EntryExitRecordCompletenessPropertyTest {
    
    @Mock
    private EntryExitRecordRepository entryExitRecordRepository;
    
    @Mock
    private QrCodeRepository qrCodeRepository;
    
    @Mock
    private QrCodeService qrCodeService;
    
    private EntryExitService entryExitService;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        entryExitService = new EntryExitServiceImpl(
            entryExitRecordRepository, 
            qrCodeRepository, 
            qrCodeService
        );
    }
    
    /**
     * Property: All required fields are present in saved record
     */
    @Property(trials = 100)
    public void allRequiredFieldsPresent(@From(UserIdGenerator.class) Long userId,
                                         @From(QrCodeValueGenerator.class) String qrCodeValue) {
        LocalDate today = LocalDate.now();
        LocalDateTime timestamp = LocalDateTime.now();
        Double latitude = 40.7128;  // Valid latitude
        Double longitude = -74.0060; // Valid longitude
        
        // Setup mocks
        QrCode qrCode = createQrCode(userId, today, qrCodeValue, 0);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue))
            .thenReturn(Optional.of(qrCode));
        when(qrCodeService.validateQrCode(qrCodeValue, userId))
            .thenReturn(new QrCodeValidationDto(true, "Valid", EntryExitType.ENTRY));
        
        ArgumentCaptor<EntryExitRecord> recordCaptor = ArgumentCaptor.forClass(EntryExitRecord.class);
        when(entryExitRecordRepository.save(recordCaptor.capture()))
            .thenAnswer(invocation -> {
                EntryExitRecord record = invocation.getArgument(0);
                record.setId(1L);
                return record;
            });
        
        // Record entry/exit
        EntryExitRecordDto result = entryExitService.recordEntryExit(
            userId, qrCodeValue, timestamp, latitude, longitude
        );
        
        // Verify saved record has all required fields
        EntryExitRecord savedRecord = recordCaptor.getValue();
        assertNotNull("Personnel ID should be present", savedRecord.getUserId());
        assertEquals("Personnel ID should match input", userId, savedRecord.getUserId());
        
        assertNotNull("Type should be present", savedRecord.getType());
        
        assertNotNull("Timestamp should be present", savedRecord.getTimestamp());
        assertEquals("Timestamp should match input", timestamp, savedRecord.getTimestamp());
        
        assertNotNull("Latitude should be present", savedRecord.getLatitude());
        assertEquals("Latitude should match input", latitude, savedRecord.getLatitude(), 0.0001);
        
        assertNotNull("Longitude should be present", savedRecord.getLongitude());
        assertEquals("Longitude should match input", longitude, savedRecord.getLongitude(), 0.0001);
        
        assertNotNull("QR Code Value should be present", savedRecord.getQrCodeValue());
        assertEquals("QR Code Value should match input", qrCodeValue, savedRecord.getQrCodeValue());
    }
    
    /**
     * Property: Returned DTO contains all required fields
     */
    @Property(trials = 100)
    public void returnedDtoComplete(@From(UserIdGenerator.class) Long userId,
                                    @From(QrCodeValueGenerator.class) String qrCodeValue) {
        LocalDate today = LocalDate.now();
        LocalDateTime timestamp = LocalDateTime.now();
        Double latitude = 40.7128;
        Double longitude = -74.0060;
        
        // Setup mocks
        QrCode qrCode = createQrCode(userId, today, qrCodeValue, 0);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue))
            .thenReturn(Optional.of(qrCode));
        when(qrCodeService.validateQrCode(qrCodeValue, userId))
            .thenReturn(new QrCodeValidationDto(true, "Valid", EntryExitType.ENTRY));
        when(entryExitRecordRepository.save(any(EntryExitRecord.class)))
            .thenAnswer(invocation -> {
                EntryExitRecord record = invocation.getArgument(0);
                record.setId(1L);
                return record;
            });
        
        // Record entry/exit
        EntryExitRecordDto result = entryExitService.recordEntryExit(
            userId, qrCodeValue, timestamp, latitude, longitude
        );
        
        // Verify DTO has all required fields
        assertNotNull("DTO ID should be present", result.id());
        assertNotNull("DTO Personnel ID should be present", result.userId());
        assertEquals("DTO Personnel ID should match input", userId, result.userId());
        
        assertNotNull("DTO Type should be present", result.type());
        
        assertNotNull("DTO Timestamp should be present", result.timestamp());
        assertEquals("DTO Timestamp should match input", timestamp, result.timestamp());
        
        assertNotNull("DTO Latitude should be present", result.latitude());
        assertEquals("DTO Latitude should match input", latitude, result.latitude(), 0.0001);
        
        assertNotNull("DTO Longitude should be present", result.longitude());
        assertEquals("DTO Longitude should match input", longitude, result.longitude(), 0.0001);
    }
    
    /**
     * Property: Record completeness is independent of entry/exit type
     */
    @Property(trials = 100)
    public void completenessIndependentOfType(@From(UserIdGenerator.class) Long userId,
                                               @From(QrCodeValueGenerator.class) String qrCodeValue) {
        LocalDate today = LocalDate.now();
        LocalDateTime timestamp = LocalDateTime.now();
        Double latitude = 40.7128;
        Double longitude = -74.0060;
        
        // Test with ENTRY type (usage count 0)
        QrCode qrCodeEntry = createQrCode(userId, today, qrCodeValue, 0);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue))
            .thenReturn(Optional.of(qrCodeEntry));
        when(qrCodeService.validateQrCode(qrCodeValue, userId))
            .thenReturn(new QrCodeValidationDto(true, "Valid", EntryExitType.ENTRY));
        
        ArgumentCaptor<EntryExitRecord> entryCaptor = ArgumentCaptor.forClass(EntryExitRecord.class);
        when(entryExitRecordRepository.save(entryCaptor.capture()))
            .thenAnswer(invocation -> {
                EntryExitRecord record = invocation.getArgument(0);
                record.setId(1L);
                return record;
            });
        
        entryExitService.recordEntryExit(userId, qrCodeValue, timestamp, latitude, longitude);
        
        EntryExitRecord entryRecord = entryCaptor.getValue();
        assertNotNull("ENTRY record should have all fields", entryRecord.getUserId());
        assertNotNull("ENTRY record should have type", entryRecord.getType());
        assertNotNull("ENTRY record should have timestamp", entryRecord.getTimestamp());
        assertNotNull("ENTRY record should have latitude", entryRecord.getLatitude());
        assertNotNull("ENTRY record should have longitude", entryRecord.getLongitude());
        assertNotNull("ENTRY record should have QR code value", entryRecord.getQrCodeValue());
        
        // Test with EXIT type (usage count 1)
        QrCode qrCodeExit = createQrCode(userId, today, qrCodeValue, 1);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue))
            .thenReturn(Optional.of(qrCodeExit));
        when(qrCodeService.validateQrCode(qrCodeValue, userId))
            .thenReturn(new QrCodeValidationDto(true, "Valid", EntryExitType.EXIT));
        
        // Reset and reconfigure mock for second test
        reset(entryExitRecordRepository);
        when(entryExitRecordRepository.save(any(EntryExitRecord.class)))
            .thenAnswer(invocation -> {
                EntryExitRecord record = invocation.getArgument(0);
                record.setId(2L);
                return record;
            });
        
        entryExitService.recordEntryExit(userId, qrCodeValue, timestamp, latitude, longitude);
        
        // Capture the saved record
        ArgumentCaptor<EntryExitRecord> exitCaptor = ArgumentCaptor.forClass(EntryExitRecord.class);
        verify(entryExitRecordRepository).save(exitCaptor.capture());
        
        EntryExitRecord exitRecord = exitCaptor.getValue();
        assertNotNull("EXIT record should have all fields", exitRecord.getUserId());
        assertNotNull("EXIT record should have type", exitRecord.getType());
        assertNotNull("EXIT record should have timestamp", exitRecord.getTimestamp());
        assertNotNull("EXIT record should have latitude", exitRecord.getLatitude());
        assertNotNull("EXIT record should have longitude", exitRecord.getLongitude());
        assertNotNull("EXIT record should have QR code value", exitRecord.getQrCodeValue());
    }
    
    private QrCode createQrCode(Long userId, LocalDate date, String value, int usageCount) {
        QrCode qrCode = new QrCode();
        qrCode.setId(1L);
        qrCode.setUserId(userId);
        qrCode.setQrCodeValue(value);
        qrCode.setValidDate(date);
        qrCode.setUsageCount(usageCount);
        return qrCode;
    }
}
