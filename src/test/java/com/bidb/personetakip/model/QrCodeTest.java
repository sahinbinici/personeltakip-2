package com.bidb.personetakip.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QrCode entity.
 */
class QrCodeTest {
    
    @Test
    void testQrCodeCreation() {
        LocalDate today = LocalDate.now();
        
        QrCode qrCode = QrCode.builder()
                .userId(1L)
                .qrCodeValue("ABC123XYZ")
                .validDate(today)
                .usageCount(0)
                .build();
        
        assertNotNull(qrCode);
        assertEquals(1L, qrCode.getUserId());
        assertEquals("ABC123XYZ", qrCode.getQrCodeValue());
        assertEquals(today, qrCode.getValidDate());
        assertEquals(0, qrCode.getUsageCount());
    }
    
    @Test
    void testIsValidForTodayReturnsTrueForCurrentDate() {
        QrCode qrCode = QrCode.builder()
                .userId(1L)
                .qrCodeValue("ABC123XYZ")
                .validDate(LocalDate.now())
                .usageCount(0)
                .build();
        
        assertTrue(qrCode.isValidForToday());
    }
    
    @Test
    void testIsValidForTodayReturnsFalseForPastDate() {
        QrCode qrCode = QrCode.builder()
                .userId(1L)
                .qrCodeValue("ABC123XYZ")
                .validDate(LocalDate.now().minusDays(1))
                .usageCount(0)
                .build();
        
        assertFalse(qrCode.isValidForToday());
    }
    
    @Test
    void testCanBeUsedReturnsTrueWhenUsageCountBelowMax() {
        QrCode qrCode = QrCode.builder()
                .userId(1L)
                .qrCodeValue("ABC123XYZ")
                .validDate(LocalDate.now())
                .usageCount(1)
                .build();
        
        assertTrue(qrCode.canBeUsed());
    }
    
    @Test
    void testCanBeUsedReturnsFalseWhenUsageCountAtMax() {
        QrCode qrCode = QrCode.builder()
                .userId(1L)
                .qrCodeValue("ABC123XYZ")
                .validDate(LocalDate.now())
                .usageCount(2)
                .build();
        
        assertFalse(qrCode.canBeUsed());
    }
    
    @Test
    void testIsValidReturnsTrueForValidQrCode() {
        QrCode qrCode = QrCode.builder()
                .userId(1L)
                .qrCodeValue("ABC123XYZ")
                .validDate(LocalDate.now())
                .usageCount(0)
                .build();
        
        assertTrue(qrCode.isValid());
    }
    
    @Test
    void testIsValidReturnsFalseForExpiredQrCode() {
        QrCode qrCode = QrCode.builder()
                .userId(1L)
                .qrCodeValue("ABC123XYZ")
                .validDate(LocalDate.now().minusDays(1))
                .usageCount(0)
                .build();
        
        assertFalse(qrCode.isValid());
    }
    
    @Test
    void testIsValidReturnsFalseForMaxUsedQrCode() {
        QrCode qrCode = QrCode.builder()
                .userId(1L)
                .qrCodeValue("ABC123XYZ")
                .validDate(LocalDate.now())
                .usageCount(2)
                .build();
        
        assertFalse(qrCode.isValid());
    }
    
    @Test
    void testIncrementUsageIncrementsCount() {
        QrCode qrCode = QrCode.builder()
                .userId(1L)
                .qrCodeValue("ABC123XYZ")
                .validDate(LocalDate.now())
                .usageCount(0)
                .build();
        
        qrCode.incrementUsage();
        assertEquals(1, qrCode.getUsageCount());
        
        qrCode.incrementUsage();
        assertEquals(2, qrCode.getUsageCount());
    }
    
    @Test
    void testIncrementUsageThrowsExceptionWhenAtMax() {
        QrCode qrCode = QrCode.builder()
                .userId(1L)
                .qrCodeValue("ABC123XYZ")
                .validDate(LocalDate.now())
                .usageCount(2)
                .build();
        
        assertThrows(IllegalStateException.class, qrCode::incrementUsage);
    }
}
