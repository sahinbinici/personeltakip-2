package com.bidb.personetakip.service;

import com.bidb.personetakip.model.EntryExitRecord;
import com.bidb.personetakip.model.EntryExitType;
import com.bidb.personetakip.model.User;
import com.bidb.personetakip.model.UserRole;
import com.bidb.personetakip.repository.EntryExitRecordRepository;
import com.bidb.personetakip.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * **Feature: ip-tracking, Property 10: CSV Export IP Inclusion**
 * **Validates: Requirements 2.5**
 * 
 * Property-based test for CSV export IP inclusion functionality.
 * Tests that for any CSV export operation, the exported file includes IP address information for all records.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CsvExportIpInclusionPropertyTest {

    @Autowired
    private AdminRecordsService adminRecordsService;
    
    @Autowired
    private EntryExitRecordRepository entryExitRecordRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Test
    public void testCsvExportIpInclusionProperty() {
        // Property: For any CSV export operation, the exported file should include IP address information for all records
        
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
            .role(UserRole.NORMAL_USER)
            .build();
        testUser = userRepository.save(testUser);
        
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
        
        // Test property: Generate CSV export and verify IP address inclusion
        String csvContent = adminRecordsService.generateCsvExport(null, null, null, null, null);
        
        // Property verification: CSV should contain IP address header
        assertTrue(csvContent.contains("IP Adresi"), 
            "CSV export should include IP address header column");
        
        // Property verification: CSV should contain all IP addresses from test data
        for (String ipAddress : testIpAddresses) {
            if (ipAddress != null) {
                assertTrue(csvContent.contains(ipAddress), 
                    "CSV export should contain IP address: " + ipAddress);
            } else {
                // For null IP addresses, should contain "Bilinmiyor" (Unknown)
                assertTrue(csvContent.contains("Bilinmiyor"), 
                    "CSV export should contain 'Bilinmiyor' for unknown IP addresses");
            }
        }
        
        // Property verification: Count lines to ensure all records are exported
        String[] lines = csvContent.split("\n");
        // Header line + data lines
        int expectedLines = 1 + testIpAddresses.size();
        assertEquals(expectedLines, lines.length, 
            "CSV export should contain header plus one line per record");
        
        // Property verification: Each data line should have IP address field
        for (int i = 1; i < lines.length; i++) { // Skip header line
            String line = lines[i];
            String[] fields = line.split(",");
            
            // Verify that the IP address field exists (should be at index 8 based on header)
            assertTrue(fields.length >= 9, 
                "Each CSV line should have at least 9 fields including IP address");
            
            String ipField = fields[8]; // IP address field
            assertNotNull(ipField, "IP address field should not be null");
            
            // IP field should either contain an IP address or "Bilinmiyor"
            assertTrue(ipField.length() > 0, "IP address field should not be empty");
        }
        
        // Test property with specific IP filter
        testCsvExportWithIpFilter(testUser.getId(), testIpAddresses);
    }
    
    private void testCsvExportWithIpFilter(Long userId, List<String> testIpAddresses) {
        // Test CSV export with IP address filtering
        String targetIp = "192.168.1.100";
        
        String filteredCsvContent = adminRecordsService.generateCsvExport(null, null, userId, null, targetIp);
        
        // Property verification: Filtered CSV should still include IP address header
        assertTrue(filteredCsvContent.contains("IP Adresi"), 
            "Filtered CSV export should include IP address header column");
        
        // Property verification: Filtered CSV should only contain the target IP
        String[] lines = filteredCsvContent.split("\n");
        for (int i = 1; i < lines.length; i++) { // Skip header line
            String line = lines[i];
            String[] fields = line.split(",");
            
            if (fields.length >= 9) {
                String ipField = fields[8]; // IP address field
                assertEquals(targetIp, ipField, 
                    "Filtered CSV should only contain records with target IP: " + targetIp);
            }
        }
        
        // Test CSV export with unknown IP filter
        String unknownIpCsvContent = adminRecordsService.generateCsvExport(null, null, userId, null, "unknown");
        
        // Property verification: Unknown IP CSV should contain "Bilinmiyor"
        assertTrue(unknownIpCsvContent.contains("Bilinmiyor"), 
            "Unknown IP filtered CSV should contain 'Bilinmiyor' entries");
        
        String[] unknownLines = unknownIpCsvContent.split("\n");
        for (int i = 1; i < unknownLines.length; i++) { // Skip header line
            String line = unknownLines[i];
            String[] fields = line.split(",");
            
            if (fields.length >= 9) {
                String ipField = fields[8]; // IP address field
                assertEquals("Bilinmiyor", ipField, 
                    "Unknown IP filtered CSV should only contain 'Bilinmiyor' entries");
            }
        }
    }
}