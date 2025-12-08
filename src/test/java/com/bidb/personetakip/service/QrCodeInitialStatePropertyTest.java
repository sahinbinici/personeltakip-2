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
 * Property-based test for QR code initial state.
 * 
 * Feature: personnel-tracking-system, Property 12: QR code initial state
 * Validates: Requirements 5.4
 * 
 * For any newly created QR Code Value, the usage counter should be initialized to 0 
 * and the maximum usage limit should be 2.
 */
@RunWith(JUnitQuickcheck.class)
public class QrCodeInitialStatePropertyTest {
    
    @Mock
    private QrCodeRepository qrCodeRepository;
    
    private QrCodeService qrCodeService;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        qrCodeService = new QrCodeServiceImpl(qrCodeRepository);
    }
    
    /**
     * Property: Newly created QR code has usage count of 0 and max usage of 2
     */
    @Property(trials = 100)
    public void newQrCodeHasCorrectInitialState(@From(UserIdGenerator.class) Long userId) {
        LocalDate today = LocalDate.now();
        
        // No existing QR code
        when(qrCodeRepository.findByUserIdAndValidDate(userId, today))
            .thenReturn(Optional.empty());
        
        // Capture the saved QR code
        when(qrCodeRepository.save(any(QrCode.class)))
            .thenAnswer(invocation -> {
                QrCode qr = invocation.getArgument(0);
                qr.setId(1L);
                return qr;
            });
        
        // Request new QR code
        QrCodeDto qrCodeDto = qrCodeService.getDailyQrCode(userId);
        
        // Verify initial state
        assertEquals("Usage count should be initialized to 0", 
            0, qrCodeDto.usageCount());
        assertEquals("Maximum usage should be 2", 
            2, qrCodeDto.maxUsage());
        assertNotNull("QR code value should not be null", qrCodeDto.qrCodeValue());
        assertEquals("Valid date should be today", today, qrCodeDto.validDate());
    }
}
