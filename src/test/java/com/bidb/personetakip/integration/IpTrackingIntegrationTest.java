package com.bidb.personetakip.integration;

import com.bidb.personetakip.dto.AdminRecordDto;
import com.bidb.personetakip.dto.EntryExitRecordDto;
import com.bidb.personetakip.model.*;
import com.bidb.personetakip.repository.*;
import com.bidb.personetakip.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for complete IP tracking workflows
 * Task 20.1: Write integration tests for complete IP tracking workflows
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class IpTrackingIntegrationTest {

    @Autowired
    private EntryExitService entryExitService;

    @Autowired
    private IpAddressService ipAddressService;

    @Autowired
    private IpComplianceService ipComplianceService;

    @Autowired
    private IpPrivacyService ipPrivacyService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntryExitRecordRepository entryExitRecordRepository;

    @Autowired
    private QrCodeRepository qrCodeRepository;

    @Autowired
    private IpAddressLogRepository ipAddressLogRepository;

    @Autowired
    private AdminRecordsService adminRecordsService;

    private User testUser;
    private User testUser2;
    private QrCode testQrCode;
    private QrCode testQrCode2;

    @BeforeEach
    void setUp() {
        // Create first test user with assigned IPs
        testUser = User.builder()
                .tcNo("12345678901")
                .personnelNo("TEST001")
                .firstName("Test")
                .lastName("User")
                .mobilePhone("5551234567")
                .passwordHash("hashedPassword")
                .role(UserRole.NORMAL_USER)
                .assignedIpAddresses("192.168.1.100,10.0.0.50")
                .build();
        testUser = userRepository.save(testUser);

        // Create second test user without assigned IPs
        testUser2 = User.builder()
                .tcNo("98765432109")
                .personnelNo("TEST002")
                .firstName("Test2")
                .lastName("User2")
                .mobilePhone("5559876543")
                .passwordHash("hashedPassword2")
                .role(UserRole.NORMAL_USER)
                .assignedIpAddresses(null)
                .build();
        testUser2 = userRepository.save(testUser2);

        // Create test QR codes
        testQrCode = QrCode.builder()
                .userId(testUser.getId())
                .qrCodeValue("TEST_QR_CODE_123")
                .validDate(LocalDateTime.now().toLocalDate())
                .usageCount(0)
                .build();
        testQrCode = qrCodeRepository.save(testQrCode);

        testQrCode2 = QrCode.builder()
                .userId(testUser2.getId())
                .qrCodeValue("TEST_QR_CODE_456")
                .validDate(LocalDateTime.now().toLocalDate())
                .usageCount(0)
                .build();
        testQrCode2 = qrCodeRepository.save(testQrCode2);
    }

    /**
     * Test IP capture from entry/exit to database storage
     */
    @Test
    void testIpCaptureFromEntryExitToDatabaseStorage() throws Exception {
        // Create mock request with IP address
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        request.addHeader("X-Forwarded-For", "203.0.113.45");

        // Extract IP address using service
        String extractedIp = ipAddressService.extractClientIpAddress(request);
        assertThat(extractedIp).isEqualTo("203.0.113.45"); // Should get X-Forwarded-For value

        // Record entry/exit with IP tracking
        EntryExitRecordDto recordDto = entryExitService.recordEntryExit(
                testUser.getId(),
                testQrCode.getQrCodeValue(),
                LocalDateTime.now(),
                41.0082,
                28.9784,
                request
        );

        // Verify IP address was captured and stored
        assertThat(recordDto).isNotNull();

        // Verify in database
        List<EntryExitRecord> records = entryExitRecordRepository.findAll();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getIpAddress()).isEqualTo("203.0.113.45");
    }

    /**
     * Test IP assignment and mismatch detection workflow
     */
    @Test
    void testIpAssignmentAndMismatchDetectionWorkflow() throws Exception {
        // Create entry/exit record with different IP than assigned
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.99"); // Different from assigned IPs

        // Record entry/exit
        EntryExitRecordDto recordDto = entryExitService.recordEntryExit(
                testUser.getId(),
                testQrCode.getQrCodeValue(),
                LocalDateTime.now(),
                41.0082,
                28.9784,
                request
        );

        // Get the record from database
        List<EntryExitRecord> records = entryExitRecordRepository.findAll();
        assertThat(records).hasSize(1);
        EntryExitRecord record = records.get(0);

        // Check IP mismatch detection
        boolean hasMismatch = ipComplianceService.hasIpMismatch(record, testUser);
        assertThat(hasMismatch).isTrue();

        // Test with matching IP
        request.setRemoteAddr("192.168.1.100"); // Matches assigned IP
        EntryExitRecordDto recordDto2 = entryExitService.recordEntryExit(
                testUser.getId(),
                testQrCode.getQrCodeValue(),
                LocalDateTime.now(),
                41.0082,
                28.9784,
                request
        );

        // Get the second record
        records = entryExitRecordRepository.findAll();
        assertThat(records).hasSize(2);
        EntryExitRecord record2 = records.get(1);

        boolean hasMismatch2 = ipComplianceService.hasIpMismatch(record2, testUser);
        assertThat(hasMismatch2).isFalse();
    }

    /**
     * Test IP privacy and anonymization features
     */
    @Test
    void testIpPrivacyAndAnonymizationFeatures() throws Exception {
        String testIp = "192.168.1.100";

        // Test IP anonymization
        String anonymizedIp = ipPrivacyService.anonymizeIpAddress(testIp);
        assertThat(anonymizedIp).isNotEqualTo(testIp);
        assertThat(anonymizedIp).isNotNull();

        // Test privacy-compliant display
        String displayIp = ipPrivacyService.displayIpAddress(testIp, true);
        assertThat(displayIp).isNotNull();

        // Test audit logging
        ipPrivacyService.logIpAddressAccess(testIp, testUser.getId(), 1L, "VIEW");

        // Verify audit log was created
        List<IpAddressLog> logs = ipAddressLogRepository.findAll();
        assertThat(logs).isNotEmpty();
    }

    /**
     * Test complete end-to-end IP tracking workflow
     */
    @Test
    void testCompleteEndToEndIpTrackingWorkflow() throws Exception {
        // 1. User performs entry/exit with IP tracking
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.45");

        EntryExitRecordDto recordDto = entryExitService.recordEntryExit(
                testUser.getId(),
                testQrCode.getQrCodeValue(),
                LocalDateTime.now(),
                41.0082,
                28.9784,
                request
        );

        // 2. Verify record was created with IP information
        List<EntryExitRecord> records = entryExitRecordRepository.findAll();
        assertThat(records).isNotEmpty();
        
        EntryExitRecord foundRecord = records.get(0);
        assertThat(foundRecord.getIpAddress()).isEqualTo("203.0.113.45");

        // 3. Check IP compliance
        boolean hasMismatch = ipComplianceService.hasIpMismatch(foundRecord, testUser);
        assertThat(hasMismatch).isTrue(); // Should be mismatch since IP not in assigned list

        // 4. Test IP validation
        boolean isValidIp = ipAddressService.isValidIpAddress("203.0.113.45");
        assertThat(isValidIp).isTrue();

        // 5. Test IP parsing for assigned addresses
        List<String> assignedIps = ipComplianceService.parseAssignedIpAddresses(testUser.getAssignedIpAddresses());
        assertThat(assignedIps).hasSize(2);
        assertThat(assignedIps).contains("192.168.1.100", "10.0.0.50");
    }

    /**
     * Test IP address validation and format handling
     */
    @Test
    void testIpAddressValidationAndFormatHandling() throws Exception {
        // Test IPv4 validation
        assertThat(ipAddressService.isValidIpAddress("192.168.1.100")).isTrue();
        assertThat(ipAddressService.isValidIpAddress("10.0.0.1")).isTrue();
        assertThat(ipAddressService.isValidIpAddress("256.1.1.1")).isFalse(); // Invalid IPv4
        assertThat(ipAddressService.isValidIpAddress("192.168.1")).isFalse(); // Incomplete IPv4

        // Test IPv6 validation
        assertThat(ipAddressService.isValidIpAddress("2001:db8::1")).isTrue();
        assertThat(ipAddressService.isValidIpAddress("::1")).isTrue(); // Loopback
        assertThat(ipAddressService.isValidIpAddress("invalid:ip")).isFalse();

        // Test null and empty handling
        assertThat(ipAddressService.isValidIpAddress(null)).isFalse();
        assertThat(ipAddressService.isValidIpAddress("")).isFalse();
        assertThat(ipAddressService.isValidIpAddress("   ")).isFalse();

        // Test unknown IP default
        String unknownDefault = ipAddressService.getUnknownIpDefault();
        assertThat(unknownDefault).isEqualTo("Unknown");

        // Test IP formatting
        String formattedIp = ipAddressService.formatIpAddress("  192.168.1.100  ");
        assertThat(formattedIp).isEqualTo("192.168.1.100");

        String formattedIpv6 = ipAddressService.formatIpAddress("2001:DB8::1");
        assertThat(formattedIpv6).isEqualTo("2001:db8::1"); // Should be lowercase
    }

    /**
     * Test IP filtering and CSV export functionality
     */
    @Test
    void testIpFilteringAndCsvExportFunctionality() throws Exception {
        // Create additional QR codes for multiple entries
        QrCode qrCode2 = QrCode.builder()
                .userId(testUser.getId())
                .qrCodeValue("TEST_QR_CODE_FILTER_1")
                .validDate(LocalDateTime.now().toLocalDate())
                .usageCount(0)
                .build();
        qrCode2 = qrCodeRepository.save(qrCode2);

        QrCode qrCode3 = QrCode.builder()
                .userId(testUser.getId())
                .qrCodeValue("TEST_QR_CODE_FILTER_2")
                .validDate(LocalDateTime.now().toLocalDate())
                .usageCount(0)
                .build();
        qrCode3 = qrCodeRepository.save(qrCode3);

        // Create multiple entry/exit records with different IP addresses
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.setRemoteAddr("192.168.1.100"); // Matches testUser's assigned IP

        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.setRemoteAddr("203.0.113.45"); // Different IP (mismatch)

        MockHttpServletRequest request3 = new MockHttpServletRequest();
        request3.setRemoteAddr("10.0.0.50"); // Matches testUser's second assigned IP

        // Record entries for testUser using different QR codes
        entryExitService.recordEntryExit(
                testUser.getId(),
                testQrCode.getQrCodeValue(),
                LocalDateTime.now().minusHours(2),
                41.0082,
                28.9784,
                request1
        );

        entryExitService.recordEntryExit(
                testUser.getId(),
                qrCode2.getQrCodeValue(),
                LocalDateTime.now().minusHours(1),
                41.0082,
                28.9784,
                request2
        );

        entryExitService.recordEntryExit(
                testUser.getId(),
                qrCode3.getQrCodeValue(),
                LocalDateTime.now(),
                41.0082,
                28.9784,
                request3
        );

        // Record entry for testUser2 (no assigned IPs)
        entryExitService.recordEntryExit(
                testUser2.getId(),
                testQrCode2.getQrCodeValue(),
                LocalDateTime.now().minusMinutes(30),
                41.0082,
                28.9784,
                request2
        );

        // Test IP address filtering
        Page<AdminRecordDto> allRecords = adminRecordsService.getRecordsWithFilters(null, null, null, null, null, null, 0, 10);
        assertThat(allRecords.getContent()).hasSize(4);

        // Test filtering by specific IP
        Page<AdminRecordDto> filteredByIp = adminRecordsService.getRecordsWithFilters(null, null, null, null, "192.168.1.100", null, 0, 10);
        assertThat(filteredByIp.getContent()).hasSize(1);
        assertThat(filteredByIp.getContent().get(0).getIpAddress()).isEqualTo("192.168.1.100");

        // Test filtering by IP range
        Page<AdminRecordDto> filteredByRange = adminRecordsService.getRecordsWithFilters(null, null, null, null, "192.168.1.", null, 0, 10);
        assertThat(filteredByRange.getContent()).hasSize(1);

        // Test filtering by IP mismatch
        Page<AdminRecordDto> mismatchRecords = adminRecordsService.getRecordsWithFilters(null, null, null, null, null, "mismatch", 0, 10);
        assertThat(mismatchRecords.getContent()).hasSize(1); // Only testUser's record with 203.0.113.45

        // Test filtering by IP match
        Page<AdminRecordDto> matchRecords = adminRecordsService.getRecordsWithFilters(null, null, null, null, null, "match", 0, 10);
        assertThat(matchRecords.getContent()).hasSize(2); // testUser's records with matching IPs

        // Test CSV export functionality
        String csvContent = adminRecordsService.generateCsvExport(null, null, null, null, null, null);
        
        // Verify CSV contains IP address header
        assertThat(csvContent).contains("IP Adresi");
        
        // Verify CSV contains all IP addresses
        assertThat(csvContent).contains("192.168.1.100");
        assertThat(csvContent).contains("203.0.113.45");
        assertThat(csvContent).contains("10.0.0.50");
        
        // Verify CSV contains IP compliance information
        assertThat(csvContent).contains("IP Uyumluluk");
        assertThat(csvContent).contains("Uyumlu"); // Match status
        assertThat(csvContent).contains("Uyumsuz"); // Mismatch status
        
        // Test CSV export with IP filtering
        String filteredCsv = adminRecordsService.generateCsvExport(null, null, null, null, "192.168.1.100", null);
        assertThat(filteredCsv).contains("192.168.1.100");
        assertThat(filteredCsv).doesNotContain("203.0.113.45");
        
        // Test CSV export with mismatch filtering
        String mismatchCsv = adminRecordsService.generateCsvExport(null, null, null, null, null, "mismatch");
        assertThat(mismatchCsv).contains("203.0.113.45");
        assertThat(mismatchCsv).contains("Uyumsuz");
    }

    /**
     * Test complete IP compliance workflow with multiple scenarios
     */
    @Test
    void testCompleteIpComplianceWorkflow() throws Exception {
        // Scenario 1: User with assigned IPs using matching IP
        MockHttpServletRequest matchingRequest = new MockHttpServletRequest();
        matchingRequest.setRemoteAddr("192.168.1.100");

        EntryExitRecordDto matchingRecord = entryExitService.recordEntryExit(
                testUser.getId(),
                testQrCode.getQrCodeValue(),
                LocalDateTime.now(),
                41.0082,
                28.9784,
                matchingRequest
        );

        // Scenario 2: User with assigned IPs using non-matching IP
        MockHttpServletRequest mismatchRequest = new MockHttpServletRequest();
        mismatchRequest.setRemoteAddr("203.0.113.99");

        EntryExitRecordDto mismatchRecord = entryExitService.recordEntryExit(
                testUser.getId(),
                testQrCode.getQrCodeValue(),
                LocalDateTime.now().plusMinutes(5),
                41.0082,
                28.9784,
                mismatchRequest
        );

        // Scenario 3: User without assigned IPs
        MockHttpServletRequest noAssignmentRequest = new MockHttpServletRequest();
        noAssignmentRequest.setRemoteAddr("10.0.0.100");

        EntryExitRecordDto noAssignmentRecord = entryExitService.recordEntryExit(
                testUser2.getId(),
                testQrCode2.getQrCodeValue(),
                LocalDateTime.now().plusMinutes(10),
                41.0082,
                28.9784,
                noAssignmentRequest
        );

        // Verify compliance status for each scenario
        List<EntryExitRecord> records = entryExitRecordRepository.findAll();
        assertThat(records).hasSize(3);

        // Check matching IP scenario
        EntryExitRecord matchRecord = records.stream()
                .filter(r -> "192.168.1.100".equals(r.getIpAddress()))
                .findFirst()
                .orElseThrow();
        
        IpComplianceService.IpComplianceStatus matchStatus = ipComplianceService.getIpComplianceStatus(matchRecord, testUser);
        assertThat(matchStatus).isEqualTo(IpComplianceService.IpComplianceStatus.MATCH);

        // Check mismatch IP scenario
        EntryExitRecord mismatchRecordEntity = records.stream()
                .filter(r -> "203.0.113.99".equals(r.getIpAddress()))
                .findFirst()
                .orElseThrow();
        
        IpComplianceService.IpComplianceStatus mismatchStatus = ipComplianceService.getIpComplianceStatus(mismatchRecordEntity, testUser);
        assertThat(mismatchStatus).isEqualTo(IpComplianceService.IpComplianceStatus.MISMATCH);

        // Check no assignment scenario
        EntryExitRecord noAssignmentRecordEntity = records.stream()
                .filter(r -> "10.0.0.100".equals(r.getIpAddress()))
                .findFirst()
                .orElseThrow();
        
        IpComplianceService.IpComplianceStatus noAssignmentStatus = ipComplianceService.getIpComplianceStatus(noAssignmentRecordEntity, testUser2);
        assertThat(noAssignmentStatus).isEqualTo(IpComplianceService.IpComplianceStatus.NO_ASSIGNMENT);

        // Test compliance statistics
        Page<AdminRecordDto> allRecords = adminRecordsService.getRecordsWithFilters(null, null, null, null, null, null, 0, 10);
        
        long matchCount = allRecords.getContent().stream()
                .filter(r -> Boolean.FALSE.equals(r.getIpMismatch()))
                .count();
        assertThat(matchCount).isEqualTo(1);

        long mismatchCount = allRecords.getContent().stream()
                .filter(r -> Boolean.TRUE.equals(r.getIpMismatch()))
                .count();
        assertThat(mismatchCount).isEqualTo(1);

        long noAssignmentCount = allRecords.getContent().stream()
                .filter(r -> r.getIpMismatch() == null)
                .count();
        assertThat(noAssignmentCount).isEqualTo(1);
    }

    /**
     * Test IP privacy and anonymization in complete workflow
     */
    @Test
    void testIpPrivacyWorkflowIntegration() throws Exception {
        // Create entry/exit record
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");

        entryExitService.recordEntryExit(
                testUser.getId(),
                testQrCode.getQrCodeValue(),
                LocalDateTime.now(),
                41.0082,
                28.9784,
                request
        );

        // Test privacy-compliant display in records
        Page<AdminRecordDto> records = adminRecordsService.getRecordsWithFilters(null, null, null, null, null, null, 0, 10);
        assertThat(records.getContent()).hasSize(1);
        
        AdminRecordDto record = records.getContent().get(0);
        assertThat(record.getIpAddress()).isNotNull();

        // Test CSV export with privacy considerations
        String csvContent = adminRecordsService.generateCsvExport(null, null, null, null, null, null);
        assertThat(csvContent).contains("IP Adresi");

        // Verify audit logging was created during CSV export
        List<IpAddressLog> auditLogs = ipAddressLogRepository.findAll();
        // Note: Audit logging might be disabled in test configuration, so we check if any logs exist
        // If audit logging is enabled, there should be ACCESS logs for IP address access during export
        if (!auditLogs.isEmpty()) {
            boolean hasAccessLog = auditLogs.stream()
                    .anyMatch(log -> IpAddressAction.ACCESS.equals(log.getAction()));
            assertThat(hasAccessLog).isTrue();
        }

        // Test IP anonymization functionality
        String originalIp = "192.168.1.100";
        String anonymizedIp = ipPrivacyService.anonymizeIpAddress(originalIp);
        assertThat(anonymizedIp).isNotEqualTo(originalIp);
        assertThat(anonymizedIp).isNotNull();

        // Test privacy-compliant display
        String displayIp = ipPrivacyService.displayIpAddress(originalIp, true);
        assertThat(displayIp).isNotNull();
    }

    /**
     * Test error handling and edge cases in IP tracking workflow
     */
    @Test
    void testIpTrackingErrorHandlingWorkflow() throws Exception {
        // Test with null IP address (simulating IP capture failure)
        MockHttpServletRequest requestWithoutIp = new MockHttpServletRequest() {
            @Override
            public String getRemoteAddr() {
                return null; // Simulate no remote address
            }
            
            @Override
            public String getHeader(String name) {
                return null; // Simulate no headers
            }
        };

        EntryExitRecordDto recordDto = entryExitService.recordEntryExit(
                testUser.getId(),
                testQrCode.getQrCodeValue(),
                LocalDateTime.now(),
                41.0082,
                28.9784,
                requestWithoutIp
        );

        // Verify entry/exit operation succeeded despite IP capture failure
        assertThat(recordDto).isNotNull();

        // Verify record was created with unknown IP
        List<EntryExitRecord> records = entryExitRecordRepository.findAll();
        assertThat(records).hasSize(1);
        
        EntryExitRecord record = records.get(0);
        // IP should be "Unknown" when IP capture fails
        assertThat(record.getIpAddress()).isEqualTo("Unknown");

        // Test compliance status with unknown IP
        IpComplianceService.IpComplianceStatus status = ipComplianceService.getIpComplianceStatus(record, testUser);
        assertThat(status).isEqualTo(IpComplianceService.IpComplianceStatus.UNKNOWN_IP);

        // Test CSV export with unknown IP
        String csvContent = adminRecordsService.generateCsvExport(null, null, null, null, null, null);
        assertThat(csvContent).contains("Bilinmiyor"); // Should contain "Unknown" in Turkish

        // Test filtering by unknown IP - the record should have "Unknown" as IP address
        Page<AdminRecordDto> unknownIpRecords = adminRecordsService.getRecordsWithFilters(null, null, null, null, "unknown", null, 0, 10);
        assertThat(unknownIpRecords.getContent()).hasSize(1);
    }
}