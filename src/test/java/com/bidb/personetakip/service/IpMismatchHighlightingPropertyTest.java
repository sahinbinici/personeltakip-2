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
 * Property-based test for IP mismatch highlighting functionality.
 * 
 * Feature: ip-tracking, Property 16: IP Mismatch Highlighting
 * Validates: Requirements 4.1
 * 
 * For any entry/exit record view, the system should highlight mismatches between actual 
 * and assigned IP addresses.
 */
@RunWith(JUnitQuickcheck.class)
public class IpMismatchHighlightingPropertyTest {
    
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
     * Property: When actual IP matches assigned IP, there should be no mismatch
     */
    @Property(trials = 100)
    public void matchingIpAddressesShouldNotShowMismatch(
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
        
        boolean hasMismatch = ipComplianceService.hasIpMismatch(record, user);
        IpComplianceService.IpComplianceStatus status = ipComplianceService.getIpComplianceStatus(record, user);
        
        assertFalse("Matching IP addresses should not show mismatch", hasMismatch);
        assertEquals("Status should be MATCH for matching IPs", 
                     IpComplianceService.IpComplianceStatus.MATCH, status);
    }
    
    /**
     * Property: When actual IP doesn't match assigned IP, there should be a mismatch
     */
    @Property(trials = 100)
    public void nonMatchingIpAddressesShouldShowMismatch(
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
        
        boolean hasMismatch = ipComplianceService.hasIpMismatch(record, user);
        IpComplianceService.IpComplianceStatus status = ipComplianceService.getIpComplianceStatus(record, user);
        
        assertTrue("Non-matching IP addresses should show mismatch", hasMismatch);
        assertEquals("Status should be MISMATCH for non-matching IPs", 
                     IpComplianceService.IpComplianceStatus.MISMATCH, status);
    }
    
    /**
     * Property: When user has no assigned IP, there should be no mismatch
     */
    @Property(trials = 100)
    public void noAssignedIpShouldNotShowMismatch(
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
        
        // Create record with IP
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
        
        assertFalse("No assigned IP should not show mismatch", hasMismatch);
        assertEquals("Status should be NO_ASSIGNMENT when user has no assigned IP", 
                     IpComplianceService.IpComplianceStatus.NO_ASSIGNMENT, status);
    }
    
    /**
     * Property: When record IP is unknown, there should be no mismatch
     */
    @Property(trials = 100)
    public void unknownIpShouldNotShowMismatch(
            @InRange(min = "1", max = "999999") long userId,
            @InRange(min = "1", max = "999999") long recordId) {
        
        String assignedIp = "192.168.1.100";
        
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
        
        // Create record with unknown IP
        EntryExitRecord record = EntryExitRecord.builder()
                .id(recordId)
                .userId(userId)
                .type(EntryExitType.ENTRY)
                .timestamp(LocalDateTime.now())
                .ipAddress(ipAddressService.getUnknownIpDefault())
                .qrCodeValue("QR123")
                .build();
        
        boolean hasMismatch = ipComplianceService.hasIpMismatch(record, user);
        IpComplianceService.IpComplianceStatus status = ipComplianceService.getIpComplianceStatus(record, user);
        
        assertFalse("Unknown IP should not show mismatch", hasMismatch);
        assertEquals("Status should be UNKNOWN_IP when record IP is unknown", 
                     IpComplianceService.IpComplianceStatus.UNKNOWN_IP, status);
    }
    
    /**
     * Property: Null inputs should be handled gracefully
     */
    @Property(trials = 100)
    public void nullInputsShouldBeHandledGracefully() {
        boolean hasMismatch1 = ipComplianceService.hasIpMismatch(null, null);
        boolean hasMismatch2 = ipComplianceService.hasIpMismatch(null, new User());
        boolean hasMismatch3 = ipComplianceService.hasIpMismatch(new EntryExitRecord(), null);
        
        IpComplianceService.IpComplianceStatus status1 = ipComplianceService.getIpComplianceStatus(null, null);
        IpComplianceService.IpComplianceStatus status2 = ipComplianceService.getIpComplianceStatus(null, new User());
        IpComplianceService.IpComplianceStatus status3 = ipComplianceService.getIpComplianceStatus(new EntryExitRecord(), null);
        
        assertFalse("Null inputs should not show mismatch", hasMismatch1);
        assertFalse("Null record should not show mismatch", hasMismatch2);
        assertFalse("Null user should not show mismatch", hasMismatch3);
        
        assertEquals("Null inputs should return UNKNOWN_IP status", 
                     IpComplianceService.IpComplianceStatus.UNKNOWN_IP, status1);
        assertEquals("Null record should return UNKNOWN_IP status", 
                     IpComplianceService.IpComplianceStatus.UNKNOWN_IP, status2);
        assertEquals("Null user should return UNKNOWN_IP status", 
                     IpComplianceService.IpComplianceStatus.UNKNOWN_IP, status3);
    }
}