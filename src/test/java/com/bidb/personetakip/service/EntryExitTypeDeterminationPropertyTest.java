package com.bidb.personetakip.service;

import com.bidb.personetakip.model.EntryExitType;
import com.bidb.personetakip.model.QrCode;
import com.bidb.personetakip.repository.QrCodeRepository;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Property-based test for entry/exit type determination.
 * 
 * Feature: personnel-tracking-system, Property 15: Entry/exit type determination
 * Validates: Requirements 6.5, 8.4
 * 
 * For any QR Code Value, when usage counter is 0, the next usage should be classified as ENTRY; 
 * when usage counter is 1, the next usage should be classified as EXIT.
 */
@RunWith(JUnitQuickcheck.class)
public class EntryExitTypeDeterminationPropertyTest {
    
    @Mock
    private QrCodeRepository qrCodeRepository;
    
    private EntryExitService entryExitService;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        QrCodeService qrCodeService = new QrCodeServiceImpl(qrCodeRepository);
        entryExitService = new EntryExitServiceImpl(null, qrCodeRepository, qrCodeService);
    }
    
    /**
     * Property: When usage count is 0, next usage is ENTRY
     */
    @Property(trials = 100)
    public void usageCountZeroMeansEntry(@From(QrCodeValueGenerator.class) String qrCodeValue,
                                         @From(UserIdGenerator.class) Long userId) {
        LocalDate today = LocalDate.now();
        
        // Create QR code with usage count 0
        QrCode qrCode = createQrCode(userId, today, qrCodeValue, 0);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue))
            .thenReturn(Optional.of(qrCode));
        
        // Determine type
        EntryExitType type = entryExitService.determineEntryExitType(qrCodeValue);
        
        assertEquals("When usage count is 0, type should be ENTRY", 
            EntryExitType.ENTRY, type);
    }
    
    /**
     * Property: When usage count is 1, next usage is EXIT
     */
    @Property(trials = 100)
    public void usageCountOneMeansExit(@From(QrCodeValueGenerator.class) String qrCodeValue,
                                       @From(UserIdGenerator.class) Long userId) {
        LocalDate today = LocalDate.now();
        
        // Create QR code with usage count 1
        QrCode qrCode = createQrCode(userId, today, qrCodeValue, 1);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue))
            .thenReturn(Optional.of(qrCode));
        
        // Determine type
        EntryExitType type = entryExitService.determineEntryExitType(qrCodeValue);
        
        assertEquals("When usage count is 1, type should be EXIT", 
            EntryExitType.EXIT, type);
    }
    
    /**
     * Property: Type determination is consistent for same QR code state
     */
    @Property(trials = 100)
    public void typeDeterminationIsConsistent(@From(QrCodeValueGenerator.class) String qrCodeValue,
                                               @From(UserIdGenerator.class) Long userId) {
        LocalDate today = LocalDate.now();
        
        // Test with usage count 0
        QrCode qrCode0 = createQrCode(userId, today, qrCodeValue, 0);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue))
            .thenReturn(Optional.of(qrCode0));
        
        EntryExitType type1 = entryExitService.determineEntryExitType(qrCodeValue);
        EntryExitType type2 = entryExitService.determineEntryExitType(qrCodeValue);
        
        assertEquals("Multiple calls with same state should return same type", type1, type2);
        assertEquals("Type should be ENTRY for usage count 0", EntryExitType.ENTRY, type1);
        
        // Test with usage count 1
        QrCode qrCode1 = createQrCode(userId, today, qrCodeValue, 1);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue))
            .thenReturn(Optional.of(qrCode1));
        
        EntryExitType type3 = entryExitService.determineEntryExitType(qrCodeValue);
        EntryExitType type4 = entryExitService.determineEntryExitType(qrCodeValue);
        
        assertEquals("Multiple calls with same state should return same type", type3, type4);
        assertEquals("Type should be EXIT for usage count 1", EntryExitType.EXIT, type3);
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
