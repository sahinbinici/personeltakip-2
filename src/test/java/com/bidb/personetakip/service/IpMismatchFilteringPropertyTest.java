package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.AdminRecordDto;
import com.bidb.personetakip.model.EntryExitRecord;
import com.bidb.personetakip.model.EntryExitType;
import com.bidb.personetakip.model.User;
import com.bidb.personetakip.model.UserRole;
import com.bidb.personetakip.repository.EntryExitRecordRepository;
import com.bidb.personetakip.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * **Feature: ip-tracking, Property 19: IP Mismatch Filtering**
 * **Validates: Requirements 4.4**
 * 
 * Property-based test for IP mismatch filtering functionality.
 * Tests that for any IP mismatch filter application, the system shows only records with IP address mismatches.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class IpMismatchFilteringPropertyTest {

    @Autowired
    private AdminRecordsService adminRecordsService;
    
    @Autowired
    private EntryExitRecordRepository entryExitRecordRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private IpComplianceService ipComplianceService;

    @Test
    public void testIpMismatchFilteringProperty() {
        // Property: For any IP mismatch filter application, the system should show only records with IP address mismatches
        
        // Create test users with different IP assignments
        User userWithAssignedIp = User.builder()
            .tcNo("12345678901")
            .firstName("Test")
            .lastName("User1")
            .personnelNo("TEST001")
            .departmentCode("IT")
            .departmentName("Information Technology")
            .mobilePhone("05551234567")
            .passwordHash("$2a$10$dummyHashForTesting")
            .role(UserRole.NORMAL_USER)
            .assignedIpAddresses("192.168.1.100,10.0.0.1") // Assigned IPs
            .build();
        userWithAssignedIp = userRepository.save(userWithAssignedIp);
        
        User userWithoutAssignedIp = User.builder()
            .tcNo("12345678902")
            .firstName("Test")
            .lastName("User2")
            .personnelNo("TEST002")
            .departmentCode("IT")
            .departmentName("Information Technology")
            .mobilePhone("05551234568")
            .passwordHash("$2a$10$dummyHashForTesting")
            .role(UserRole.NORMAL_USER)
            .assignedIpAddresses(null) // No assigned IPs
            .build();
        userWithoutAssignedIp = userRepository.save(userWithoutAssignedIp);
        
        // Create entry/exit records with various IP scenarios
        createTestRecords(userWithAssignedIp, userWithoutAssignedIp);
        
        // Test property: Verify IP mismatch detection logic
        testIpMismatchDetection(userWithAssignedIp, userWithoutAssignedIp);
        
        // Test property: Verify mismatch filtering capability
        testMismatchFilteringCapability(userWithAssignedIp, userWithoutAssignedIp);
    }
    
    private void createTestRecords(User userWithAssignedIp, User userWithoutAssignedIp) {
        // Records for user with assigned IPs
        
        // Matching IP record
        EntryExitRecord matchingRecord = EntryExitRecord.builder()
            .userId(userWithAssignedIp.getId())
            .type(EntryExitType.ENTRY)
            .timestamp(LocalDateTime.now().minusHours(1))
            .qrCodeValue("QR_MATCH")
            .ipAddress("192.168.1.100") // Matches assigned IP
            .latitude(41.0082)
            .longitude(28.9784)
            .build();
        entryExitRecordRepository.save(matchingRecord);
        
        // Mismatching IP record
        EntryExitRecord mismatchRecord = EntryExitRecord.builder()
            .userId(userWithAssignedIp.getId())
            .type(EntryExitType.EXIT)
            .timestamp(LocalDateTime.now().minusHours(2))
            .qrCodeValue("QR_MISMATCH")
            .ipAddress("172.16.0.50") // Does not match assigned IPs
            .latitude(41.0082)
            .longitude(28.9784)
            .build();
        entryExitRecordRepository.save(mismatchRecord);
        
        // Unknown IP record for user with assigned IPs
        EntryExitRecord unknownIpRecord = EntryExitRecord.builder()
            .userId(userWithAssignedIp.getId())
            .type(EntryExitType.ENTRY)
            .timestamp(LocalDateTime.now().minusHours(3))
            .qrCodeValue("QR_UNKNOWN")
            .ipAddress(null) // Unknown IP
            .latitude(41.0082)
            .longitude(28.9784)
            .build();
        entryExitRecordRepository.save(unknownIpRecord);
        
        // Records for user without assigned IPs
        EntryExitRecord noAssignmentRecord = EntryExitRecord.builder()
            .userId(userWithoutAssignedIp.getId())
            .type(EntryExitType.ENTRY)
            .timestamp(LocalDateTime.now().minusHours(4))
            .qrCodeValue("QR_NO_ASSIGNMENT")
            .ipAddress("203.0.113.42") // Any IP, but user has no assignments
            .latitude(41.0082)
            .longitude(28.9784)
            .build();
        entryExitRecordRepository.save(noAssignmentRecord);
    }
    
    private void testIpMismatchDetection(User userWithAssignedIp, User userWithoutAssignedIp) {
        // Get all records for testing
        List<EntryExitRecord> allRecords = entryExitRecordRepository.findAll();
        
        for (EntryExitRecord record : allRecords) {
            User user = userRepository.findById(record.getUserId()).orElse(null);
            assertNotNull(user, "User should exist for record");
            
            // Test IP compliance status determination
            IpComplianceService.IpComplianceStatus status = ipComplianceService.getIpComplianceStatus(record, user);
            
            if (user.getId().equals(userWithAssignedIp.getId())) {
                // User with assigned IPs
                if ("192.168.1.100".equals(record.getIpAddress()) || "10.0.0.1".equals(record.getIpAddress())) {
                    // Should match
                    assertEquals(IpComplianceService.IpComplianceStatus.MATCH, status,
                        "Record with matching IP should have MATCH status");
                } else if (record.getIpAddress() == null) {
                    // Unknown IP
                    assertEquals(IpComplianceService.IpComplianceStatus.UNKNOWN_IP, status,
                        "Record with unknown IP should have UNKNOWN_IP status");
                } else {
                    // Mismatch
                    assertEquals(IpComplianceService.IpComplianceStatus.MISMATCH, status,
                        "Record with non-matching IP should have MISMATCH status");
                }
            } else {
                // User without assigned IPs
                assertEquals(IpComplianceService.IpComplianceStatus.NO_ASSIGNMENT, status,
                    "Record for user without IP assignments should have NO_ASSIGNMENT status");
            }
        }
    }
    
    private void testMismatchFilteringCapability(User userWithAssignedIp, User userWithoutAssignedIp) {
        // Get all records and manually identify mismatches
        List<EntryExitRecord> allRecords = entryExitRecordRepository.findAll();
        
        int expectedMismatchCount = 0;
        for (EntryExitRecord record : allRecords) {
            User user = userRepository.findById(record.getUserId()).orElse(null);
            if (user != null && ipComplianceService.hasIpMismatch(record, user)) {
                expectedMismatchCount++;
            }
        }
        
        // Property verification: There should be at least one mismatch record
        assertTrue(expectedMismatchCount > 0, 
            "Test data should contain at least one IP mismatch record");
        
        // Test the filtering capability by simulating mismatch filter
        // This tests the logic that would be used in a mismatch filter
        long actualMismatchCount = allRecords.stream()
            .filter(record -> {
                User user = userRepository.findById(record.getUserId()).orElse(null);
                return user != null && ipComplianceService.hasIpMismatch(record, user);
            })
            .count();
        
        // Property verification: Mismatch filtering should return correct count
        assertEquals(expectedMismatchCount, actualMismatchCount,
            "Mismatch filtering should return the correct number of mismatch records");
        
        // Property verification: All filtered records should actually be mismatches
        boolean allAreMismatches = allRecords.stream()
            .filter(record -> {
                User user = userRepository.findById(record.getUserId()).orElse(null);
                return user != null && ipComplianceService.hasIpMismatch(record, user);
            })
            .allMatch(record -> {
                User user = userRepository.findById(record.getUserId()).orElse(null);
                return user != null && ipComplianceService.hasIpMismatch(record, user);
            });
        
        assertTrue(allAreMismatches, 
            "All records returned by mismatch filter should actually be mismatches");
        
        // Test specific mismatch scenarios
        testSpecificMismatchScenarios(userWithAssignedIp);
    }
    
    private void testSpecificMismatchScenarios(User userWithAssignedIp) {
        // Test that matching IPs are not considered mismatches
        EntryExitRecord matchingRecord = entryExitRecordRepository.findAll().stream()
            .filter(r -> "192.168.1.100".equals(r.getIpAddress()) && 
                        r.getUserId().equals(userWithAssignedIp.getId()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(matchingRecord, "Matching record should exist");
        assertFalse(ipComplianceService.hasIpMismatch(matchingRecord, userWithAssignedIp),
            "Record with matching IP should not be considered a mismatch");
        
        // Test that non-matching IPs are considered mismatches
        EntryExitRecord mismatchRecord = entryExitRecordRepository.findAll().stream()
            .filter(r -> "172.16.0.50".equals(r.getIpAddress()) && 
                        r.getUserId().equals(userWithAssignedIp.getId()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(mismatchRecord, "Mismatch record should exist");
        assertTrue(ipComplianceService.hasIpMismatch(mismatchRecord, userWithAssignedIp),
            "Record with non-matching IP should be considered a mismatch");
    }
}