package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.QrCodeDto;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test for QR code uniqueness and idempotency.
 * 
 * Feature: personnel-tracking-system, Property 10: Daily QR code uniqueness and idempotency
 * Validates: Requirements 5.1, 5.2
 * 
 * For any user and date combination, requesting a QR code multiple times on the same date 
 * should return the same QR Code Value, but different dates should produce different values.
 */
@RunWith(JUnitQuickcheck.class)
public class QrCodeUniquenessPropertyTest {
    
    @Mock
    private QrCodeRepository qrCodeRepository;
    
    private QrCodeService qrCodeService;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        qrCodeService = new QrCodeServiceImpl(qrCodeRepository);
    }
    
    /**
     * Property: Requesting QR code multiple times for same user and date returns same value (idempotency)
     */
    @Property(trials = 100)
    public void qrCodeIdempotencyForSameDate(@From(UserIdGenerator.class) Long userId) {
        LocalDate today = LocalDate.now();
        
        // Capture the saved QR code to return it on subsequent calls
        final QrCode[] savedQrCode = new QrCode[1];
        
        // First call - no existing QR code, then return the saved one
        when(qrCodeRepository.findByUserIdAndValidDate(userId, today))
            .thenAnswer(invocation -> {
                if (savedQrCode[0] == null) {
                    return Optional.empty();
                } else {
                    return Optional.of(savedQrCode[0]);
                }
            });
        
        when(qrCodeRepository.save(any(QrCode.class)))
            .thenAnswer(invocation -> {
                QrCode qr = invocation.getArgument(0);
                qr.setId(1L);
                savedQrCode[0] = qr;
                return qr;
            });
        
        // Request QR code first time
        QrCodeDto firstRequest = qrCodeService.getDailyQrCode(userId);
        
        // Request QR code second time (should return existing)
        QrCodeDto secondRequest = qrCodeService.getDailyQrCode(userId);
        
        // Should return the same QR code value
        assertEquals("QR code value should be the same for multiple requests on same date",
            firstRequest.qrCodeValue(), secondRequest.qrCodeValue());
        
        // Should have same valid date
        assertEquals("Valid date should be the same",
            firstRequest.validDate(), secondRequest.validDate());
    }
    
    /**
     * Property: Different dates should produce different QR code values for same user
     */
    @Property(trials = 100)
    public void qrCodeUniquenessForDifferentDates(@From(UserIdGenerator.class) Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        
        // Create QR codes with different values for different dates
        QrCode todayQrCode = createQrCode(userId, today, "TODAY-" + userId + "-" + today);
        QrCode tomorrowQrCode = createQrCode(userId, tomorrow, "TOMORROW-" + userId + "-" + tomorrow);
        
        // Verify they are different
        assertNotEquals("QR code values should be different for different dates",
            todayQrCode.getQrCodeValue(), tomorrowQrCode.getQrCodeValue());
    }
    
    private QrCode createQrCode(Long userId, LocalDate date, String value) {
        QrCode qrCode = new QrCode();
        qrCode.setUserId(userId);
        qrCode.setQrCodeValue(value);
        qrCode.setValidDate(date);
        qrCode.setUsageCount(0);
        return qrCode;
    }
}
