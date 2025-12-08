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
 * Property-based test for QR code validation against user ownership.
 * 
 * Feature: personnel-tracking-system, Property 16: QR code validation against user ownership
 * Validates: Requirements 8.2
 * 
 * For any entry/exit request, QR code validation should verify that the QR Code Value 
 * belongs to the user identified in the JWT token.
 */
@RunWith(JUnitQuickcheck.class)
public class QrCodeOwnershipValidationPropertyTest {
    
    @Mock
    private QrCodeRepository qrCodeRepository;
    
    private QrCodeService qrCodeService;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        qrCodeService = new QrCodeServiceImpl(qrCodeRepository);
    }
    
    /**
     * Property: QR code validation succeeds when QR code belongs to the user
     */
    @Property(trials = 100)
    public void validationSucceedsForOwner(@From(QrCodeValueGenerator.class) String qrCodeValue,
                                           @From(UserIdGenerator.class) Long userId) {
        LocalDate today = LocalDate.now();
        
        // Create QR code owned by the user
        QrCode qrCode = createQrCode(userId, today, qrCodeValue, 0);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue))
            .thenReturn(Optional.of(qrCode));
        
        // Validate with the same user ID
        QrCodeValidationDto validation = qrCodeService.validateQrCode(qrCodeValue, userId);
        
        assertTrue("QR code should be valid for the owner", validation.valid());
    }
    
    /**
     * Property: QR code validation fails when QR code belongs to a different user
     */
    @Property(trials = 100)
    public void validationFailsForNonOwner(@From(QrCodeValueGenerator.class) String qrCodeValue,
                                           @From(UserIdGenerator.class) Long ownerId,
                                           @From(UserIdGenerator.class) Long requesterId) {
        // Ensure owner and requester are different
        if (ownerId.equals(requesterId)) {
            requesterId = ownerId + 1;
        }
        
        LocalDate today = LocalDate.now();
        
        // Create QR code owned by ownerId
        QrCode qrCode = createQrCode(ownerId, today, qrCodeValue, 0);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue))
            .thenReturn(Optional.of(qrCode));
        
        // Validate with different user ID (requesterId)
        QrCodeValidationDto validation = qrCodeService.validateQrCode(qrCodeValue, requesterId);
        
        assertFalse("QR code should not be valid for non-owner", validation.valid());
        assertTrue("Error message should mention ownership", 
            validation.message().toLowerCase().contains("does not belong"));
    }
    
    /**
     * Property: Ownership validation is consistent
     */
    @Property(trials = 100)
    public void ownershipValidationIsConsistent(@From(QrCodeValueGenerator.class) String qrCodeValue,
                                                 @From(UserIdGenerator.class) Long ownerId,
                                                 @From(UserIdGenerator.class) Long nonOwnerId) {
        // Ensure owner and non-owner are different
        if (ownerId.equals(nonOwnerId)) {
            nonOwnerId = ownerId + 1;
        }
        
        LocalDate today = LocalDate.now();
        
        // Create QR code owned by ownerId
        QrCode qrCode = createQrCode(ownerId, today, qrCodeValue, 0);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue))
            .thenReturn(Optional.of(qrCode));
        
        // Multiple validations with owner should all succeed
        QrCodeValidationDto validation1 = qrCodeService.validateQrCode(qrCodeValue, ownerId);
        QrCodeValidationDto validation2 = qrCodeService.validateQrCode(qrCodeValue, ownerId);
        
        assertTrue("First validation for owner should succeed", validation1.valid());
        assertTrue("Second validation for owner should succeed", validation2.valid());
        
        // Multiple validations with non-owner should all fail
        QrCodeValidationDto validation3 = qrCodeService.validateQrCode(qrCodeValue, nonOwnerId);
        QrCodeValidationDto validation4 = qrCodeService.validateQrCode(qrCodeValue, nonOwnerId);
        
        assertFalse("First validation for non-owner should fail", validation3.valid());
        assertFalse("Second validation for non-owner should fail", validation4.valid());
    }
    
    /**
     * Property: Ownership check is independent of usage count
     */
    @Property(trials = 100)
    public void ownershipCheckIndependentOfUsageCount(@From(QrCodeValueGenerator.class) String qrCodeValue,
                                                       @From(UserIdGenerator.class) Long ownerId,
                                                       @From(UserIdGenerator.class) Long nonOwnerId) {
        // Ensure owner and non-owner are different
        if (ownerId.equals(nonOwnerId)) {
            nonOwnerId = ownerId + 1;
        }
        
        LocalDate today = LocalDate.now();
        
        // Test with usage count 0
        QrCode qrCode0 = createQrCode(ownerId, today, qrCodeValue, 0);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue))
            .thenReturn(Optional.of(qrCode0));
        
        QrCodeValidationDto validation0Owner = qrCodeService.validateQrCode(qrCodeValue, ownerId);
        QrCodeValidationDto validation0NonOwner = qrCodeService.validateQrCode(qrCodeValue, nonOwnerId);
        
        assertTrue("Owner should be valid with usage count 0", validation0Owner.valid());
        assertFalse("Non-owner should be invalid with usage count 0", validation0NonOwner.valid());
        
        // Test with usage count 1
        QrCode qrCode1 = createQrCode(ownerId, today, qrCodeValue, 1);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue))
            .thenReturn(Optional.of(qrCode1));
        
        QrCodeValidationDto validation1Owner = qrCodeService.validateQrCode(qrCodeValue, ownerId);
        QrCodeValidationDto validation1NonOwner = qrCodeService.validateQrCode(qrCodeValue, nonOwnerId);
        
        assertTrue("Owner should be valid with usage count 1", validation1Owner.valid());
        assertFalse("Non-owner should be invalid with usage count 1", validation1NonOwner.valid());
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
