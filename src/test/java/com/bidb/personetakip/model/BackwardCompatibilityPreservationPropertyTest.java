package com.bidb.personetakip.model;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

import java.time.LocalDateTime;

import static org.junit.Assert.*;

/**
 * Property-based test for backward compatibility preservation in EntryExitRecord.
 * **Feature: ip-tracking, Property 5: Backward Compatibility Preservation**
 * **Validates: Requirements 1.5**
 */
@RunWith(JUnitQuickcheck.class)
public class BackwardCompatibilityPreservationPropertyTest {

    /**
     * Property 5: Backward Compatibility Preservation
     * For any entry/exit functionality, adding IP tracking should not affect the original behavior.
     * **Validates: Requirements 1.5**
     */
    @Property(trials = 100)
    public void backwardCompatibilityPreservation(
            long userId,
            EntryExitType type,
            double latitude,
            double longitude,
            String qrCodeValue) {
        
        // Skip invalid inputs
        if (userId <= 0 || qrCodeValue == null || qrCodeValue.trim().isEmpty()) {
            return;
        }
        
        // Constrain GPS coordinates to valid ranges
        latitude = Math.max(-90.0, Math.min(90.0, latitude));
        longitude = Math.max(-180.0, Math.min(180.0, longitude));
        
        LocalDateTime timestamp = LocalDateTime.now();
        
        // Create record without IP address (legacy behavior)
        EntryExitRecord recordWithoutIp = EntryExitRecord.builder()
                .userId(userId)
                .type(type)
                .timestamp(timestamp)
                .latitude(latitude)
                .longitude(longitude)
                .qrCodeValue(qrCodeValue.trim())
                .build();
        
        // Create record with IP address (new behavior)
        EntryExitRecord recordWithIp = EntryExitRecord.builder()
                .userId(userId)
                .type(type)
                .timestamp(timestamp)
                .latitude(latitude)
                .longitude(longitude)
                .qrCodeValue(qrCodeValue.trim())
                .ipAddress("192.168.1.100")
                .build();
        
        // Verify that all existing functionality works the same way
        // regardless of whether IP address is present or not
        
        // Core fields should be identical
        assertEquals("User ID should be identical", recordWithoutIp.getUserId(), recordWithIp.getUserId());
        assertEquals("Type should be identical", recordWithoutIp.getType(), recordWithIp.getType());
        assertEquals("Timestamp should be identical", recordWithoutIp.getTimestamp(), recordWithIp.getTimestamp());
        assertEquals("Latitude should be identical", recordWithoutIp.getLatitude(), recordWithIp.getLatitude());
        assertEquals("Longitude should be identical", recordWithoutIp.getLongitude(), recordWithIp.getLongitude());
        assertEquals("QR code should be identical", recordWithoutIp.getQrCodeValue(), recordWithIp.getQrCodeValue());
        
        // GPS coordinate methods should work identically
        assertEquals("GPS coordinates presence should be identical", 
                recordWithoutIp.hasGpsCoordinates(), recordWithIp.hasGpsCoordinates());
        assertEquals("GPS coordinates validity should be identical", 
                recordWithoutIp.hasValidGpsCoordinates(), recordWithIp.hasValidGpsCoordinates());
        
        // IP address should be null for legacy record and present for new record
        assertNull("Legacy record should have null IP address", recordWithoutIp.getIpAddress());
        assertEquals("New record should have IP address", "192.168.1.100", recordWithIp.getIpAddress());
        
        // Both records should be valid and functional
        assertNotNull("Legacy record should be valid", recordWithoutIp);
        assertNotNull("New record should be valid", recordWithIp);
    }
    
    /**
     * Property: Null IP address handling
     * For any record with null IP address, existing functionality should work normally.
     */
    @Property(trials = 100)
    public void nullIpAddressHandling(
            long userId,
            EntryExitType type,
            String qrCodeValue) {
        
        // Skip invalid inputs
        if (userId <= 0 || qrCodeValue == null || qrCodeValue.trim().isEmpty()) {
            return;
        }
        
        LocalDateTime timestamp = LocalDateTime.now();
        
        // Create record with explicitly null IP address
        EntryExitRecord record = EntryExitRecord.builder()
                .userId(userId)
                .type(type)
                .timestamp(timestamp)
                .qrCodeValue(qrCodeValue.trim())
                .ipAddress(null)
                .build();
        
        // Verify that null IP address doesn't break existing functionality
        assertNotNull("Record should be valid", record);
        assertEquals("User ID should be preserved", userId, (long) record.getUserId());
        assertEquals("Type should be preserved", type, record.getType());
        assertEquals("Timestamp should be preserved", timestamp, record.getTimestamp());
        assertEquals("QR code should be preserved", qrCodeValue.trim(), record.getQrCodeValue());
        assertNull("IP address should be null", record.getIpAddress());
        
        // GPS methods should still work
        assertTrue("GPS coordinates should be valid when not present", record.hasValidGpsCoordinates());
        assertFalse("GPS coordinates should not be present", record.hasGpsCoordinates());
    }
    
    /**
     * Property: Empty IP address handling
     * For any record with empty IP address, existing functionality should work normally.
     */
    @Property(trials = 100)
    public void emptyIpAddressHandling(
            long userId,
            EntryExitType type,
            String qrCodeValue) {
        
        // Skip invalid inputs
        if (userId <= 0 || qrCodeValue == null || qrCodeValue.trim().isEmpty()) {
            return;
        }
        
        LocalDateTime timestamp = LocalDateTime.now();
        
        // Create record with empty IP address
        EntryExitRecord record = EntryExitRecord.builder()
                .userId(userId)
                .type(type)
                .timestamp(timestamp)
                .qrCodeValue(qrCodeValue.trim())
                .ipAddress("")
                .build();
        
        // Verify that empty IP address doesn't break existing functionality
        assertNotNull("Record should be valid", record);
        assertEquals("User ID should be preserved", userId, (long) record.getUserId());
        assertEquals("Type should be preserved", type, record.getType());
        assertEquals("Timestamp should be preserved", timestamp, record.getTimestamp());
        assertEquals("QR code should be preserved", qrCodeValue.trim(), record.getQrCodeValue());
        assertEquals("IP address should be empty", "", record.getIpAddress());
        
        // GPS methods should still work
        assertTrue("GPS coordinates should be valid when not present", record.hasValidGpsCoordinates());
        assertFalse("GPS coordinates should not be present", record.hasGpsCoordinates());
    }
}