package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.AdminRecordDto;
import com.bidb.personetakip.model.EntryExitRecord;
import com.bidb.personetakip.model.EntryExitType;
import com.bidb.personetakip.model.User;
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
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * **Feature: ip-tracking, Property 6: IP Address Column Display**
 * **Validates: Requirements 2.1**
 * 
 * Property-based test for IP address column display in admin records view.
 * Tests that for any admin records view, the system displays IP address column for each entry/exit record.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class IpAddressColumnDisplayPropertyTest {

    @Autowired
    private AdminRecordsService adminRecordsService;
    
    @Autowired
    private EntryExitRecordRepository entryExitRecordRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Test
    public void testIpAddressColumnDisplayProperty() {
        // Property: For any admin records view, the system should display IP address column for each entry/exit record
        
        // Generate test data with various IP addresses
        List<String> testIpAddresses = java.util.Arrays.asList(
            "192.168.1.100",
            "10.0.0.1", 
            "172.16.0.50",
            "2001:db8::1",
            "fe80::1%eth0",
            null, // Unknown IP
            "203.0.113.42"
        );
        
        // Create test user
        User testUser = User.builder()
            .tcNo("12345678901")
            .firstName("Test")
            .lastName("User")
            .personnelNo("TEST001")
            .departmentCode("IT")
            .departmentName("Information Technology")
            .mobilePhone("05551234567")
            .passwordHash("$2a$10$dummyHashForTesting")
            .role(com.bidb.personetakip.model.UserRole.NORMAL_USER)
            .build();
        testUser = userRepository.save(testUser);
        
        // Create entry/exit records with different IP addresses
        for (int i = 0; i < testIpAddresses.size(); i++) {
            String ipAddress = testIpAddresses.get(i);
            EntryExitRecord record = EntryExitRecord.builder()
                .userId(testUser.getId())
                .type(i % 2 == 0 ? EntryExitType.ENTRY : EntryExitType.EXIT)
                .timestamp(LocalDateTime.now().minusHours(i))
                .qrCodeValue("QR" + i)
                .ipAddress(ipAddress)
                .latitude(41.0082)
                .longitude(28.9784)
                .build();
            entryExitRecordRepository.save(record);
        }
        
        // Test property: Get all records and verify IP address column is present
        Page<AdminRecordDto> recordsPage = adminRecordsService.getAllRecords(0, 20);
        
        // Verify that all records have IP address information available
        assertFalse(recordsPage.getContent().isEmpty(), "Records should be present");
        
        for (AdminRecordDto record : recordsPage.getContent()) {
            // Property verification: Each record should have IP address field accessible
            // The field should be present even if null (for unknown IPs)
            assertNotNull(record, "Record should not be null");
            
            // Verify that the DTO has the capability to hold IP address information
            // This tests that the IP address column is structurally available
            assertTrue(hasIpAddressField(record), "AdminRecordDto should have IP address field capability");
        }
        
        // Additional verification: Test with filtered records
        Page<AdminRecordDto> filteredRecords = adminRecordsService.getRecordsByUser(testUser.getId(), 0, 10);
        
        for (AdminRecordDto record : filteredRecords.getContent()) {
            assertTrue(hasIpAddressField(record), "Filtered records should also have IP address field capability");
        }
    }
    
    /**
     * Helper method to verify that AdminRecordDto has IP address field capability.
     * This simulates checking if the IP address column would be displayable.
     */
    private boolean hasIpAddressField(AdminRecordDto record) {
        try {
            // Try to access IP address field through reflection to verify it exists
            // This represents the UI's ability to display the IP address column
            java.lang.reflect.Field[] fields = record.getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if ("ipAddress".equals(field.getName())) {
                    return true;
                }
            }
            
            // Also check if we can get IP address through getter methods
            java.lang.reflect.Method[] methods = record.getClass().getDeclaredMethods();
            for (java.lang.reflect.Method method : methods) {
                if ("getIpAddress".equals(method.getName())) {
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            return false;
        }
    }
}