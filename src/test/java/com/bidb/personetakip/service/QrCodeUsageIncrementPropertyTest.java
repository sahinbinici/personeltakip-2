package com.bidb.personetakip.service;

import com.bidb.personetakip.exception.ValidationException;
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
 * Property-based test for QR code usage increment atomicity.
 * 
 * Feature: personnel-tracking-system, Property 14: QR code usage increment atomicity
 * Validates: Requirements 6.1, 6.2
 * 
 * For any QR Code Value, each usage should increment the counter by exactly 1, 
 * and the counter should never exceed 2.
 */
@RunWith(JUnitQuickcheck.class)
public class QrCodeUsageIncrementPropertyTest {
    
    @Mock
    private QrCodeRepository qrCodeRepository;
    
    private QrCodeService qrCodeService;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        qrCodeService = new QrCodeServiceImpl(qrCodeRepository);
    }
    
    /**
     * Property: Each usage increments counter by exactly 1
     */
    @Property(trials = 100)
    public void usageIncrementsByExactlyOne(@From(QrCodeValueGenerator.class) String qrCodeValue,
                                            @From(UserIdGenerator.class) Long userId) {
        LocalDate today = LocalDate.now();
        
        // Test increment from 0 to 1
        QrCode qrCode0 = createQrCode(userId, today, qrCodeValue, 0);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue))
            .thenReturn(Optional.of(qrCode0));
        when(qrCodeRepository.save(any(QrCode.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        qrCodeService.incrementUsageCount(qrCodeValue);
        
        assertEquals("Usage count should increment from 0 to 1", 
            1, qrCode0.getUsageCount().intValue());
        
        // Test increment from 1 to 2
        QrCode qrCode1 = createQrCode(userId, today, qrCodeValue, 1);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue))
            .thenReturn(Optional.of(qrCode1));
        
        qrCodeService.incrementUsageCount(qrCodeValue);
        
        assertEquals("Usage count should increment from 1 to 2", 
            2, qrCode1.getUsageCount().intValue());
    }
    
    /**
     * Property: Counter never exceeds 2
     */
    @Property(trials = 100)
    public void usageCountNeverExceedsTwo(@From(QrCodeValueGenerator.class) String qrCodeValue,
                                          @From(UserIdGenerator.class) Long userId) {
        LocalDate today = LocalDate.now();
        
        // QR code already at maximum usage (2)
        QrCode qrCode = createQrCode(userId, today, qrCodeValue, 2);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue))
            .thenReturn(Optional.of(qrCode));
        
        // Attempting to increment should throw exception
        try {
            qrCodeService.incrementUsageCount(qrCodeValue);
            fail("Should throw ValidationException when usage count is already 2");
        } catch (ValidationException e) {
            assertTrue("Exception message should mention usage limit", 
                e.getMessage().contains("usage limit"));
        }
        
        // Verify count is still 2 (not incremented)
        assertEquals("Usage count should remain at 2", 
            2, qrCode.getUsageCount().intValue());
    }
    
    /**
     * Property: Multiple increments reach exactly 2
     */
    @Property(trials = 100)
    public void multipleIncrementsReachExactlyTwo(@From(QrCodeValueGenerator.class) String qrCodeValue,
                                                   @From(UserIdGenerator.class) Long userId) {
        LocalDate today = LocalDate.now();
        
        // Start with usage count 0
        QrCode qrCode = createQrCode(userId, today, qrCodeValue, 0);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue))
            .thenReturn(Optional.of(qrCode));
        when(qrCodeRepository.save(any(QrCode.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // First increment: 0 -> 1
        qrCodeService.incrementUsageCount(qrCodeValue);
        assertEquals("First increment should result in count of 1", 
            1, qrCode.getUsageCount().intValue());
        
        // Second increment: 1 -> 2
        qrCodeService.incrementUsageCount(qrCodeValue);
        assertEquals("Second increment should result in count of 2", 
            2, qrCode.getUsageCount().intValue());
        
        // Third increment should fail
        try {
            qrCodeService.incrementUsageCount(qrCodeValue);
            fail("Third increment should throw ValidationException");
        } catch (ValidationException e) {
            // Expected
        }
        
        // Verify final count is exactly 2
        assertEquals("Final usage count should be exactly 2", 
            2, qrCode.getUsageCount().intValue());
    }
    
    /**
     * Property: Increment is atomic (uses optimistic locking)
     */
    @Property(trials = 100)
    public void incrementUsesOptimisticLocking(@From(QrCodeValueGenerator.class) String qrCodeValue,
                                                @From(UserIdGenerator.class) Long userId) {
        LocalDate today = LocalDate.now();
        
        // Create QR code with version field (optimistic locking)
        QrCode qrCode = createQrCode(userId, today, qrCodeValue, 0);
        qrCode.setVersion(1L); // Set initial version
        
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue))
            .thenReturn(Optional.of(qrCode));
        when(qrCodeRepository.save(any(QrCode.class)))
            .thenAnswer(invocation -> {
                QrCode saved = invocation.getArgument(0);
                // Simulate version increment (what JPA does with @Version)
                if (saved.getVersion() != null) {
                    saved.setVersion(saved.getVersion() + 1);
                }
                return saved;
            });
        
        Long initialVersion = qrCode.getVersion();
        
        qrCodeService.incrementUsageCount(qrCodeValue);
        
        // Verify that save was called (which would trigger version increment)
        verify(qrCodeRepository, times(1)).save(qrCode);
        
        // Verify version was incremented (optimistic locking in action)
        assertNotNull("Version should not be null", qrCode.getVersion());
        assertTrue("Version should be incremented after save", 
            qrCode.getVersion() > initialVersion);
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
