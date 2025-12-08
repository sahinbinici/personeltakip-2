package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.QrCodeValidationDto;
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
 * Property-based test for QR code validity period enforcement.
 * 
 * Feature: personnel-tracking-system, Property 11: QR code validity period enforcement
 * Validates: Requirements 5.3, 6.4
 * 
 * For any QR Code Value, validation should succeed only when the current date matches 
 * the valid date of the code.
 */
@RunWith(JUnitQuickcheck.class)
public class QrCodeValidityPropertyTest {
    
    @Mock
    private QrCodeRepository qrCodeRepository;
    
    private QrCodeService qrCodeService;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        qrCodeService = new QrCodeServiceImpl(qrCodeRepository);
    }
    
    /**
     * Property: QR code validation succeeds only when current date matches valid date
     */
    @Property(trials = 100)
    public void qrCodeValidOnlyForCorrectDate(@From(UserIdGenerator.class) Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);
        
        String qrCodeValue = "QR-" + userId + "-" + today;
        
        // Test 1: QR code valid for today should succeed
        QrCode todayQrCode = createQrCode(userId, today, qrCodeValue, 0);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue))
            .thenReturn(Optional.of(todayQrCode));
        
        QrCodeValidationDto todayValidation = qrCodeService.validateQrCode(qrCodeValue, userId);
        assertTrue("QR code should be valid for today", todayValidation.valid());
        
        // Test 2: QR code valid for yesterday should fail
        String yesterdayQrValue = "QR-" + userId + "-" + yesterday;
        QrCode yesterdayQrCode = createQrCode(userId, yesterday, yesterdayQrValue, 0);
        when(qrCodeRepository.findByQrCodeValue(yesterdayQrValue))
            .thenReturn(Optional.of(yesterdayQrCode));
        
        QrCodeValidationDto yesterdayValidation = qrCodeService.validateQrCode(yesterdayQrValue, userId);
        assertFalse("QR code should not be valid for yesterday", yesterdayValidation.valid());
        assertTrue("Error message should mention date validity", 
            yesterdayValidation.message().contains("not valid for today"));
        
        // Test 3: QR code valid for tomorrow should fail
        String tomorrowQrValue = "QR-" + userId + "-" + tomorrow;
        QrCode tomorrowQrCode = createQrCode(userId, tomorrow, tomorrowQrValue, 0);
        when(qrCodeRepository.findByQrCodeValue(tomorrowQrValue))
            .thenReturn(Optional.of(tomorrowQrCode));
        
        QrCodeValidationDto tomorrowValidation = qrCodeService.validateQrCode(tomorrowQrValue, userId);
        assertFalse("QR code should not be valid for tomorrow", tomorrowValidation.valid());
        assertTrue("Error message should mention date validity", 
            tomorrowValidation.message().contains("not valid for today"));
    }
    
    /**
     * Property: QR code with usage count >= 2 should be invalid
     */
    @Property(trials = 100)
    public void qrCodeInvalidWhenUsageLimitReached(@From(UserIdGenerator.class) Long userId) {
        LocalDate today = LocalDate.now();
        String qrCodeValue = "QR-" + userId + "-" + today;
        
        // QR code with usage count = 2 should be invalid
        QrCode qrCode = createQrCode(userId, today, qrCodeValue, 2);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue))
            .thenReturn(Optional.of(qrCode));
        
        QrCodeValidationDto validation = qrCodeService.validateQrCode(qrCodeValue, userId);
        assertFalse("QR code should be invalid when usage limit reached", validation.valid());
        assertTrue("Error message should mention usage limit", 
            validation.message().contains("already been used twice"));
    }
    
    private QrCode createQrCode(Long userId, LocalDate date, String value, int usageCount) {
        QrCode qrCode = new QrCode();
        qrCode.setUserId(userId);
        qrCode.setQrCodeValue(value);
        qrCode.setValidDate(date);
        qrCode.setUsageCount(usageCount);
        return qrCode;
    }
}
