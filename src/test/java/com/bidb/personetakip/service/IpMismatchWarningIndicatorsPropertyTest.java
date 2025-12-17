package com.bidb.personetakip.service;

import com.bidb.personetakip.model.EntryExitRecord;
import com.bidb.personetakip.model.EntryExitType;
import com.bidb.personetakip.model.User;
import com.bidb.personetakip.model.UserRole;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.time.LocalDateTime;

import static org.junit.Assert.*;

/**
 * Property-based test for IP mismatch warning indicators functionality.
 * 
 * Feature: ip-tracking, Property 17: IP Mismatch Warning Indicators
 * Validates: Requirements 4.2
 * 
 * For any user with assigned IPs, the system should show warning indicators for 
 * entry/exit from different IPs.
 */
@RunWith(JUnitQuickcheck.class)
public class IpMismatchWarningIndicatorsPropertyTest {
    
    private IpComplianceService ipComplianceService;
    private IpAddressService ipAddressService;
    
    @Before
    public void setUp() {
        ipAddressService = TestConfigurationHelper.createIpAddressService();
        ipComplianceService = new IpComplianceServiceImpl();
        // Manually inject the dependency since we're not using Spring context
        ((IpComplianceServiceImpl) ipComplianceService).ipAddressService = ipAddressService;
    }
    
    /**
     * Property: Users with assigned IPs should show warning indicators for mismatched IPs
     */
    @Property(trials = 100)
    public void usersWithAssignedIpsShouldShowWarningForMismatch(
            @InRange(min = "1", max = "999999") long userId,
            @InRange(min = "1", max = "999999") long recordId) {
        
        // Create different valid IP addresses
        String assignedIp = "192.168.1.100";
        String actualIp = "10.0.0.50";
        
        // Create user with assigned IP
        User user = User.builder()
                .id(userId)
                .tcNo("12345678901")
                .personnelNo("P" + userId)
                .firstName("Test")
                .lastName("User")
                .mobilePhone("5551234567")
                .assignedIpAddresses(assignedIp)
                .role(UserRole.NORMAL_USER)
                .build();
        
        // Create record with different IP
        EntryExitRecord record = EntryExitRecord.builder()
                .id(recordId)
                .userId(userId)
                .type(EntryExitType.ENTRY)
                .timestamp(LocalDateTime.now())
                .ipAddress(actualIp)
                .qrCodeValue("QR123")
                .build();
        
        IpComplianceService.IpComplianceStatus status = ipComplianceService.getIpComplianceStatus(record, user);
        boolean hasMismatch = ipComplianceService.hasIpMismatch(record, user);
        
        assertTrue("User with assigned IP should show warning for different IP", hasMismatch);
        assertEquals("Status should indicate MISMATCH for warning display", 
                     IpComplianceService.IpComplianceStatus.MISMATCH, status);
    }
    
    /**
     * Property: Users with assigned IPs should not show warning for matching IPs
     */
    @Property(trials = 100)
    public void usersWithAssignedIpsShouldNotShowWarningForMatch(
            @InRange(min = "1", max = "999999") long userId,
            @InRange(min = "1", max = "999999") long recordId) {
        
        // Create a valid IP address
        String validIp = "192.168.1.100";
        
        // Create user with assigned IP
        User user = User.builder()
                .id(userId)
                .tcNo("12345678901")
                .personnelNo("P" + userId)
                .firstName("Test")
                .lastName("User")
                .mobilePhone("5551234567")
                .assignedIpAddresses(validIp)
                .role(UserRole.NORMAL_USER)
                .build();
        
        // Create record with same IP
        EntryExitRecord record = EntryExitRecord.builder()
                .id(recordId)
                .userId(userId)
                .type(EntryExitType.ENTRY)
                .timestamp(LocalDateTime.now())
                .ipAddress(validIp)
                .qrCodeValue("QR123")
                .build();
        
        IpComplianceService.IpComplianceStatus status = ipComplianceService.getIpComplianceStatus(record, user);
        boolean hasMismatch = ipComplianceService.hasIpMismatch(record, user);
        
        assertFalse("User with matching IP should not show warning", hasMismatch);
        assertEquals("Status should indicate MATCH for no warning display", 
                     IpComplianceService.IpComplianceStatus.MATCH, status);
    }
    
    /**
     * Property: Users with multiple assigned IPs should show warning only when none match
     */
    @Property(trials = 100)
    public void usersWithMultipleAssignedIpsShouldShowWarningOnlyWhenNoneMatch(
            @InRange(min = "1", max = "999999") long userId,
            @InRange(min = "1", max = "999999") long recordId) {
        
        // Create multiple assigned IPs
        String assignedIps = "192.168.1.100,10.0.0.50,172.16.0.10";
        String matchingIp = "10.0.0.50"; // One of the assigned IPs
        String nonMatchingIp = "203.0.113.1"; // Not in assigned IPs
        
        // Create user with multiple assigned IPs
        User user = User.builder()
                .id(userId)
                .tcNo("12345678901")
                .personnelNo("P" + userId)
                .firstName("Test")
                .lastName("User")
                .mobilePhone("5551234567")
                .assignedIpAddresses(assignedIps)
                .role(UserRole.NORMAL_USER)
                .build();
        
        // Test with matching IP - should not show warning
        EntryExitRecord matchingRecord = EntryExitRecord.builder()
                .id(recordId)
                .userId(userId)
                .type(EntryExitType.ENTRY)
                .timestamp(LocalDateTime.now())
                .ipAddress(matchingIp)
                .qrCodeValue("QR123")
                .build();
        
        boolean hasMismatchForMatching = ipComplianceService.hasIpMismatch(matchingRecord, user);
        IpComplianceService.IpComplianceStatus statusForMatching = ipComplianceService.getIpComplianceStatus(matchingRecord, user);
        
        assertFalse("User should not show warning when IP matches one of assigned IPs", hasMismatchForMatching);
        assertEquals("Status should be MATCH when IP matches one of assigned IPs", 
                     IpComplianceService.IpComplianceStatus.MATCH, statusForMatching);
        
        // Test with non-matching IP - should show warning
        EntryExitRecord nonMatchingRecord = EntryExitRecord.builder()
                .id(recordId + 1)
                .userId(userId)
                .type(EntryExitType.EXIT)
                .timestamp(LocalDateTime.now())
                .ipAddress(nonMatchingIp)
                .qrCodeValue("QR124")
                .build();
        
        boolean hasMismatchForNonMatching = ipComplianceService.hasIpMismatch(nonMatchingRecord, user);
        IpComplianceService.IpComplianceStatus statusForNonMatching = ipComplianceService.getIpComplianceStatus(nonMatchingRecord, user);
        
        assertTrue("User should show warning when IP doesn't match any assigned IP", hasMismatchForNonMatching);
        assertEquals("Status should be MISMATCH when IP doesn't match any assigned IP", 
                     IpComplianceService.IpComplianceStatus.MISMATCH, statusForNonMatching);
    }
    
    /**
     * Property: Warning indicators should be consistent for the same user and IP combination
     */
    @Property(trials = 100)
    public void warningIndicatorsShouldBeConsistent(
            @InRange(min = "1", max = "999999") long userId,
            @InRange(min = "1", max = "999999") long recordId) {
        
        String assignedIp = "192.168.1.100";
        String actualIp = "10.0.0.50";
        
        // Create user with assigned IP
        User user = User.builder()
                .id(userId)
                .tcNo("12345678901")
                .personnelNo("P" + userId)
                .firstName("Test")
                .lastName("User")
                .mobilePhone("5551234567")
                .assignedIpAddresses(assignedIp)
                .role(UserRole.NORMAL_USER)
                .build();
        
        // Create two identical records
        EntryExitRecord record1 = EntryExitRecord.builder()
                .id(recordId)
                .userId(userId)
                .type(EntryExitType.ENTRY)
                .timestamp(LocalDateTime.now())
                .ipAddress(actualIp)
                .qrCodeValue("QR123")
                .build();
        
        EntryExitRecord record2 = EntryExitRecord.builder()
                .id(recordId + 1)
                .userId(userId)
                .type(EntryExitType.EXIT)
                .timestamp(LocalDateTime.now().plusMinutes(30))
                .ipAddress(actualIp)
                .qrCodeValue("QR124")
                .build();
        
        boolean hasMismatch1 = ipComplianceService.hasIpMismatch(record1, user);
        boolean hasMismatch2 = ipComplianceService.hasIpMismatch(record2, user);
        
        IpComplianceService.IpComplianceStatus status1 = ipComplianceService.getIpComplianceStatus(record1, user);
        IpComplianceService.IpComplianceStatus status2 = ipComplianceService.getIpComplianceStatus(record2, user);
        
        assertEquals("Warning indicators should be consistent for same user and IP", hasMismatch1, hasMismatch2);
        assertEquals("Compliance status should be consistent for same user and IP", status1, status2);
    }
    
    /**
     * Property: Users without assigned IPs should never show warning indicators
     */
    @Property(trials = 100)
    public void usersWithoutAssignedIpsShouldNeverShowWarning(
            @InRange(min = "1", max = "999999") long userId,
            @InRange(min = "1", max = "999999") long recordId) {
        
        String actualIp = "192.168.1.100";
        
        // Create user without assigned IP
        User user = User.builder()
                .id(userId)
                .tcNo("12345678901")
                .personnelNo("P" + userId)
                .firstName("Test")
                .lastName("User")
                .mobilePhone("5551234567")
                .assignedIpAddresses(null) // No assigned IP
                .role(UserRole.NORMAL_USER)
                .build();
        
        // Create record with any IP
        EntryExitRecord record = EntryExitRecord.builder()
                .id(recordId)
                .userId(userId)
                .type(EntryExitType.ENTRY)
                .timestamp(LocalDateTime.now())
                .ipAddress(actualIp)
                .qrCodeValue("QR123")
                .build();
        
        boolean hasMismatch = ipComplianceService.hasIpMismatch(record, user);
        IpComplianceService.IpComplianceStatus status = ipComplianceService.getIpComplianceStatus(record, user);
        
        assertFalse("Users without assigned IPs should never show warning", hasMismatch);
        assertEquals("Status should be NO_ASSIGNMENT for users without assigned IPs", 
                     IpComplianceService.IpComplianceStatus.NO_ASSIGNMENT, status);
    }
}