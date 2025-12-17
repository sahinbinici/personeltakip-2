package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.IpComplianceReportDto;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * **Feature: ip-tracking, Property 18: IP Compliance Statistics in Reports**
 * **Validates: Requirements 4.3**
 * 
 * Property-based test for IP compliance statistics in reports functionality.
 * Tests that for any generated report, the system includes IP compliance statistics and mismatch information.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class IpComplianceStatisticsPropertyTest {

    @Autowired
    private AdminReportsService adminReportsService;
    
    @Autowired
    private EntryExitRecordRepository entryExitRecordRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private IpComplianceService ipComplianceService;

    @Test
    public void testIpComplianceStatisticsProperty() {
        // Property: For any generated report, the system should include IP compliance statistics and mismatch information
        
        // Create test users with different IP assignment scenarios
        User userWithMatchingIp = createUserWithIpAssignment("12345678901", "TEST001", "192.168.1.100,10.0.0.1");
        User userWithoutAssignment = createUserWithIpAssignment("12345678902", "TEST002", null);
        User userWithMultipleIps = createUserWithIpAssignment("12345678903", "TEST003", "172.16.0.1;172.16.0.2;172.16.0.3");
        
        // Create entry/exit records with various IP compliance scenarios
        LocalDate testDate = LocalDate.now();
        createTestRecordsForComplianceReport(userWithMatchingIp, userWithoutAssignment, userWithMultipleIps, testDate);
        
        // Test property: Generate report and verify statistics
        testReportStatisticsAccuracy(testDate);
        
        // Test property: Verify user mismatch information
        testUserMismatchInformation(testDate, userWithMatchingIp, userWithMultipleIps);
        
        // Test property: Verify department statistics
        testDepartmentStatistics(testDate);
        
        // Test property: Verify IP usage statistics
        testIpUsageStatistics(testDate);
    }
    
    private User createUserWithIpAssignment(String tcNo, String personnelNo, String assignedIps) {
        User user = User.builder()
            .tcNo(tcNo)
            .firstName("Test")
            .lastName("User")
            .personnelNo(personnelNo)
            .departmentCode("IT")
            .departmentName("Information Technology")
            .mobilePhone("05551234567")
            .passwordHash("$2a$10$dummyHashForTesting")
            .role(UserRole.NORMAL_USER)
            .assignedIpAddresses(assignedIps)
            .build();
        return userRepository.save(user);
    }
    
    private void createTestRecordsForComplianceReport(User userWithMatchingIp, User userWithoutAssignment, 
                                                     User userWithMultipleIps, LocalDate testDate) {
        LocalDateTime baseTime = testDate.atTime(10, 0);
        
        // Records for user with matching IP assignment
        // Matching record
        entryExitRecordRepository.save(EntryExitRecord.builder()
            .userId(userWithMatchingIp.getId())
            .type(EntryExitType.ENTRY)
            .timestamp(baseTime.minusHours(1))
            .qrCodeValue("QR_MATCH_1")
            .ipAddress("192.168.1.100") // Matches assigned IP
            .latitude(41.0082)
            .longitude(28.9784)
            .build());
        
        // Another matching record with different assigned IP
        entryExitRecordRepository.save(EntryExitRecord.builder()
            .userId(userWithMatchingIp.getId())
            .type(EntryExitType.EXIT)
            .timestamp(baseTime.minusHours(2))
            .qrCodeValue("QR_MATCH_2")
            .ipAddress("10.0.0.1") // Matches second assigned IP
            .latitude(41.0082)
            .longitude(28.9784)
            .build());
        
        // Mismatch record
        entryExitRecordRepository.save(EntryExitRecord.builder()
            .userId(userWithMatchingIp.getId())
            .type(EntryExitType.ENTRY)
            .timestamp(baseTime.minusHours(3))
            .qrCodeValue("QR_MISMATCH_1")
            .ipAddress("203.0.113.42") // Does not match any assigned IP
            .latitude(41.0082)
            .longitude(28.9784)
            .build());
        
        // Unknown IP record
        entryExitRecordRepository.save(EntryExitRecord.builder()
            .userId(userWithMatchingIp.getId())
            .type(EntryExitType.EXIT)
            .timestamp(baseTime.minusHours(4))
            .qrCodeValue("QR_UNKNOWN_1")
            .ipAddress(null) // Unknown IP
            .latitude(41.0082)
            .longitude(28.9784)
            .build());
        
        // Records for user without IP assignment
        entryExitRecordRepository.save(EntryExitRecord.builder()
            .userId(userWithoutAssignment.getId())
            .type(EntryExitType.ENTRY)
            .timestamp(baseTime.minusHours(5))
            .qrCodeValue("QR_NO_ASSIGNMENT_1")
            .ipAddress("192.168.1.200") // Any IP, but user has no assignment
            .latitude(41.0082)
            .longitude(28.9784)
            .build());
        
        entryExitRecordRepository.save(EntryExitRecord.builder()
            .userId(userWithoutAssignment.getId())
            .type(EntryExitType.EXIT)
            .timestamp(baseTime.minusHours(6))
            .qrCodeValue("QR_NO_ASSIGNMENT_2")
            .ipAddress("10.0.0.50") // Another IP, but user has no assignment
            .latitude(41.0082)
            .longitude(28.9784)
            .build());
        
        // Records for user with multiple IPs
        // Matching record
        entryExitRecordRepository.save(EntryExitRecord.builder()
            .userId(userWithMultipleIps.getId())
            .type(EntryExitType.ENTRY)
            .timestamp(baseTime.minusHours(7))
            .qrCodeValue("QR_MULTI_MATCH_1")
            .ipAddress("172.16.0.2") // Matches one of the assigned IPs
            .latitude(41.0082)
            .longitude(28.9784)
            .build());
        
        // Mismatch record
        entryExitRecordRepository.save(EntryExitRecord.builder()
            .userId(userWithMultipleIps.getId())
            .type(EntryExitType.EXIT)
            .timestamp(baseTime.minusHours(8))
            .qrCodeValue("QR_MULTI_MISMATCH_1")
            .ipAddress("198.51.100.10") // Does not match any assigned IP
            .latitude(41.0082)
            .longitude(28.9784)
            .build());
    }
    
    private void testReportStatisticsAccuracy(LocalDate testDate) {
        // Generate IP compliance report
        IpComplianceReportDto report = adminReportsService.generateIpComplianceReport(testDate, testDate);
        
        // Property verification: Report should contain basic statistics
        assertNotNull(report, "Report should not be null");
        assertEquals(testDate, report.getStartDate(), "Report start date should match requested date");
        assertEquals(testDate, report.getEndDate(), "Report end date should match requested date");
        assertEquals(LocalDate.now(), report.getReportDate(), "Report generation date should be today");
        
        // Verify total records count
        assertTrue(report.getTotalRecords() > 0, "Report should contain records");
        
        // Calculate expected statistics manually
        List<EntryExitRecord> allRecords = entryExitRecordRepository.findByTimestampBetween(
            testDate.atStartOfDay(), testDate.atTime(23, 59, 59));
        
        long expectedTotal = allRecords.size();
        long expectedMatching = 0;
        long expectedMismatch = 0;
        long expectedNoAssignment = 0;
        long expectedUnknownIp = 0;
        
        for (EntryExitRecord record : allRecords) {
            User user = userRepository.findById(record.getUserId()).orElse(null);
            if (user != null) {
                IpComplianceService.IpComplianceStatus status = ipComplianceService.getIpComplianceStatus(record, user);
                switch (status) {
                    case MATCH:
                        expectedMatching++;
                        break;
                    case MISMATCH:
                        expectedMismatch++;
                        break;
                    case NO_ASSIGNMENT:
                        expectedNoAssignment++;
                        break;
                    case UNKNOWN_IP:
                        expectedUnknownIp++;
                        break;
                }
            }
        }
        
        // Property verification: Statistics should match calculated values
        assertEquals(expectedTotal, report.getTotalRecords(), 
            "Total records count should match actual records");
        assertEquals(expectedMatching, report.getMatchingRecords(), 
            "Matching records count should match calculated value");
        assertEquals(expectedMismatch, report.getMismatchRecords(), 
            "Mismatch records count should match calculated value");
        assertEquals(expectedNoAssignment, report.getNoAssignmentRecords(), 
            "No assignment records count should match calculated value");
        assertEquals(expectedUnknownIp, report.getUnknownIpRecords(), 
            "Unknown IP records count should match calculated value");
        
        // Property verification: Compliance percentage calculation
        long recordsWithAssignments = expectedMatching + expectedMismatch;
        double expectedCompliancePercentage = recordsWithAssignments > 0 ? 
            (double) expectedMatching / recordsWithAssignments * 100.0 : 0.0;
        
        assertEquals(expectedCompliancePercentage, report.getCompliancePercentage(), 0.01,
            "Compliance percentage should be calculated correctly");
        
        // Property verification: Sum of all categories should equal total
        long sumOfCategories = report.getMatchingRecords() + report.getMismatchRecords() + 
                              report.getNoAssignmentRecords() + report.getUnknownIpRecords();
        assertEquals(report.getTotalRecords(), sumOfCategories,
            "Sum of all compliance categories should equal total records");
    }
    
    private void testUserMismatchInformation(LocalDate testDate, User userWithMatchingIp, User userWithMultipleIps) {
        IpComplianceReportDto report = adminReportsService.generateIpComplianceReport(testDate, testDate);
        
        // Property verification: User mismatch list should not be null
        assertNotNull(report.getUserMismatches(), "User mismatches list should not be null");
        
        // Property verification: Users with mismatches should be included
        boolean foundUserWithMismatch = report.getUserMismatches().stream()
            .anyMatch(mismatch -> mismatch.getUserId().equals(userWithMatchingIp.getId()) ||
                                 mismatch.getUserId().equals(userWithMultipleIps.getId()));
        
        assertTrue(foundUserWithMismatch, 
            "Report should include users who have IP mismatches");
        
        // Property verification: Mismatch information should be accurate
        for (IpComplianceReportDto.UserIpMismatchDto mismatch : report.getUserMismatches()) {
            assertTrue(mismatch.getMismatchCount() > 0, 
                "Users in mismatch list should have mismatch count > 0");
            assertNotNull(mismatch.getUserFullName(), 
                "User full name should be provided in mismatch information");
            assertNotNull(mismatch.getActualIpAddresses(), 
                "Actual IP addresses should be provided in mismatch information");
            assertFalse(mismatch.getActualIpAddresses().isEmpty(), 
                "Actual IP addresses list should not be empty for mismatch users");
        }
    }
    
    private void testDepartmentStatistics(LocalDate testDate) {
        IpComplianceReportDto report = adminReportsService.generateIpComplianceReport(testDate, testDate);
        
        // Property verification: Department statistics should not be null
        assertNotNull(report.getDepartmentStats(), "Department statistics should not be null");
        
        // Property verification: Department statistics should be accurate
        for (IpComplianceReportDto.DepartmentIpComplianceDto deptStat : report.getDepartmentStats()) {
            // Each department should have valid statistics
            assertTrue(deptStat.getTotalRecords() >= 0, 
                "Department total records should be non-negative");
            
            // Sum of compliance categories should equal total for department
            long deptSum = deptStat.getMatchingRecords() + deptStat.getMismatchRecords() + 
                          deptStat.getNoAssignmentRecords() + deptStat.getUnknownIpRecords();
            assertEquals(deptStat.getTotalRecords(), deptSum,
                "Sum of department compliance categories should equal department total");
            
            // Compliance percentage should be calculated correctly
            long deptRecordsWithAssignments = deptStat.getMatchingRecords() + deptStat.getMismatchRecords();
            double expectedDeptCompliance = deptRecordsWithAssignments > 0 ? 
                (double) deptStat.getMatchingRecords() / deptRecordsWithAssignments * 100.0 : 0.0;
            
            assertEquals(expectedDeptCompliance, deptStat.getCompliancePercentage(), 0.01,
                "Department compliance percentage should be calculated correctly");
        }
    }
    
    private void testIpUsageStatistics(LocalDate testDate) {
        IpComplianceReportDto report = adminReportsService.generateIpComplianceReport(testDate, testDate);
        
        // Property verification: IP usage statistics should not be null
        assertNotNull(report.getTopIpAddresses(), "IP usage statistics should not be null");
        
        // Property verification: IP usage statistics should be accurate
        for (IpComplianceReportDto.IpUsageDto ipUsage : report.getTopIpAddresses()) {
            assertTrue(ipUsage.getUsageCount() > 0, 
                "IP addresses in usage statistics should have usage count > 0");
            assertTrue(ipUsage.getUniqueUsers() > 0, 
                "IP addresses should have unique users count > 0");
            assertNotNull(ipUsage.getIpAddress(), 
                "IP address should not be null in usage statistics");
            assertNotNull(ipUsage.getUserNames(), 
                "User names list should not be null in IP usage statistics");
            
            // Usage count should be >= unique users (one user can use same IP multiple times)
            assertTrue(ipUsage.getUsageCount() >= ipUsage.getUniqueUsers(),
                "Usage count should be greater than or equal to unique users count");
        }
        
        // Property verification: IP usage list should be sorted by usage count (descending)
        List<IpComplianceReportDto.IpUsageDto> ipUsageList = report.getTopIpAddresses();
        for (int i = 0; i < ipUsageList.size() - 1; i++) {
            assertTrue(ipUsageList.get(i).getUsageCount() >= ipUsageList.get(i + 1).getUsageCount(),
                "IP usage list should be sorted by usage count in descending order");
        }
    }
}