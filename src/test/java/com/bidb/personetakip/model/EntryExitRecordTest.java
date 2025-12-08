package com.bidb.personetakip.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EntryExitRecord entity.
 */
class EntryExitRecordTest {
    
    @Test
    void testEntryExitRecordCreation() {
        LocalDateTime timestamp = LocalDateTime.now();
        
        EntryExitRecord record = EntryExitRecord.builder()
                .userId(1L)
                .type(EntryExitType.ENTRY)
                .timestamp(timestamp)
                .latitude(41.0082)
                .longitude(28.9784)
                .qrCodeValue("ABC123XYZ")
                .build();
        
        assertNotNull(record);
        assertEquals(1L, record.getUserId());
        assertEquals(EntryExitType.ENTRY, record.getType());
        assertEquals(timestamp, record.getTimestamp());
        assertEquals(41.0082, record.getLatitude());
        assertEquals(28.9784, record.getLongitude());
        assertEquals("ABC123XYZ", record.getQrCodeValue());
    }
    
    @Test
    void testHasGpsCoordinatesReturnsTrueWhenBothPresent() {
        EntryExitRecord record = EntryExitRecord.builder()
                .userId(1L)
                .type(EntryExitType.ENTRY)
                .timestamp(LocalDateTime.now())
                .latitude(41.0082)
                .longitude(28.9784)
                .qrCodeValue("ABC123XYZ")
                .build();
        
        assertTrue(record.hasGpsCoordinates());
    }
    
    @Test
    void testHasGpsCoordinatesReturnsFalseWhenLatitudeNull() {
        EntryExitRecord record = EntryExitRecord.builder()
                .userId(1L)
                .type(EntryExitType.ENTRY)
                .timestamp(LocalDateTime.now())
                .longitude(28.9784)
                .qrCodeValue("ABC123XYZ")
                .build();
        
        assertFalse(record.hasGpsCoordinates());
    }
    
    @Test
    void testHasGpsCoordinatesReturnsFalseWhenLongitudeNull() {
        EntryExitRecord record = EntryExitRecord.builder()
                .userId(1L)
                .type(EntryExitType.ENTRY)
                .timestamp(LocalDateTime.now())
                .latitude(41.0082)
                .qrCodeValue("ABC123XYZ")
                .build();
        
        assertFalse(record.hasGpsCoordinates());
    }
    
    @Test
    void testHasValidGpsCoordinatesReturnsTrueForValidCoordinates() {
        EntryExitRecord record = EntryExitRecord.builder()
                .userId(1L)
                .type(EntryExitType.ENTRY)
                .timestamp(LocalDateTime.now())
                .latitude(41.0082)
                .longitude(28.9784)
                .qrCodeValue("ABC123XYZ")
                .build();
        
        assertTrue(record.hasValidGpsCoordinates());
    }
    
    @Test
    void testHasValidGpsCoordinatesReturnsTrueWhenNoCoordinates() {
        EntryExitRecord record = EntryExitRecord.builder()
                .userId(1L)
                .type(EntryExitType.ENTRY)
                .timestamp(LocalDateTime.now())
                .qrCodeValue("ABC123XYZ")
                .build();
        
        assertTrue(record.hasValidGpsCoordinates());
    }
    
    @Test
    void testHasValidGpsCoordinatesReturnsFalseForInvalidLatitude() {
        EntryExitRecord record = EntryExitRecord.builder()
                .userId(1L)
                .type(EntryExitType.ENTRY)
                .timestamp(LocalDateTime.now())
                .latitude(91.0)
                .longitude(28.9784)
                .qrCodeValue("ABC123XYZ")
                .build();
        
        assertFalse(record.hasValidGpsCoordinates());
    }
    
    @Test
    void testHasValidGpsCoordinatesReturnsFalseForInvalidLongitude() {
        EntryExitRecord record = EntryExitRecord.builder()
                .userId(1L)
                .type(EntryExitType.ENTRY)
                .timestamp(LocalDateTime.now())
                .latitude(41.0082)
                .longitude(181.0)
                .qrCodeValue("ABC123XYZ")
                .build();
        
        assertFalse(record.hasValidGpsCoordinates());
    }
    
    @Test
    void testBoundaryLatitudeValues() {
        // Test minimum latitude
        EntryExitRecord minLat = EntryExitRecord.builder()
                .userId(1L)
                .type(EntryExitType.ENTRY)
                .timestamp(LocalDateTime.now())
                .latitude(-90.0)
                .longitude(0.0)
                .qrCodeValue("ABC123XYZ")
                .build();
        assertTrue(minLat.hasValidGpsCoordinates());
        
        // Test maximum latitude
        EntryExitRecord maxLat = EntryExitRecord.builder()
                .userId(1L)
                .type(EntryExitType.ENTRY)
                .timestamp(LocalDateTime.now())
                .latitude(90.0)
                .longitude(0.0)
                .qrCodeValue("ABC123XYZ")
                .build();
        assertTrue(maxLat.hasValidGpsCoordinates());
    }
    
    @Test
    void testBoundaryLongitudeValues() {
        // Test minimum longitude
        EntryExitRecord minLon = EntryExitRecord.builder()
                .userId(1L)
                .type(EntryExitType.ENTRY)
                .timestamp(LocalDateTime.now())
                .latitude(0.0)
                .longitude(-180.0)
                .qrCodeValue("ABC123XYZ")
                .build();
        assertTrue(minLon.hasValidGpsCoordinates());
        
        // Test maximum longitude
        EntryExitRecord maxLon = EntryExitRecord.builder()
                .userId(1L)
                .type(EntryExitType.ENTRY)
                .timestamp(LocalDateTime.now())
                .latitude(0.0)
                .longitude(180.0)
                .qrCodeValue("ABC123XYZ")
                .build();
        assertTrue(maxLon.hasValidGpsCoordinates());
    }
}
