package com.bidb.personetakip.service;

import com.bidb.personetakip.model.IpAddressAction;
import com.bidb.personetakip.model.IpAddressLog;
import com.bidb.personetakip.repository.IpAddressLogRepository;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test for IP access audit logging functionality.
 * **Feature: ip-tracking, Property 22: IP Access Audit Logging**
 * **Validates: Requirements 5.3**
 */
@RunWith(JUnitQuickcheck.class)
public class IpAccessAuditLoggingPropertyTest {

    @Mock
    private IpAddressLogRepository ipAddressLogRepository;
    
    @Mock
    private IpAddressService ipAddressService;
    
    private IpPrivacyServiceImpl ipPrivacyService;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        ipPrivacyService = new IpPrivacyServiceImpl();
        ReflectionTestUtils.setField(ipPrivacyService, "ipAddressLogRepository", ipAddressLogRepository);
        ReflectionTestUtils.setField(ipPrivacyService, "ipAddressService", ipAddressService);
        
        // Mock IpAddressService methods
        when(ipAddressService.getUnknownIpDefault()).thenReturn("Unknown");
        when(ipAddressService.formatIpAddress(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Mock repository save method
        when(ipAddressLogRepository.save(any(IpAddressLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
    
    /**
     * Property: For any IP address access operation, the system should create an audit log entry
     * with the correct IP address, user ID, admin user ID, and action
     */
    @Property(trials = 100)
    public void testIpAccessAuditLogging(String ipAddress) {
        // Use a valid action from the enum
        String validAction = "VIEW";
        
        Long userId = 123L;
        Long adminUserId = 456L;
        
        // Call the method under test
        ipPrivacyService.logIpAddressAccess(ipAddress, userId, adminUserId, validAction);
        
        // Verify that save was called
        ArgumentCaptor<IpAddressLog> logCaptor = ArgumentCaptor.forClass(IpAddressLog.class);
        verify(ipAddressLogRepository, times(1)).save(logCaptor.capture());
        
        IpAddressLog savedLog = logCaptor.getValue();
        
        // Verify the log entry properties
        assertEquals("User ID should match", userId, savedLog.getUserId());
        assertEquals("IP address should match", ipAddress, savedLog.getIpAddress());
        assertEquals("Admin user ID should match", adminUserId, savedLog.getAdminUserId());
        assertEquals("Action should match", IpAddressAction.valueOf(validAction), savedLog.getAction());
        assertNotNull("Timestamp should be set", savedLog.getTimestamp());
        assertNotNull("Details should be set", savedLog.getDetails());
        
        // Reset mock for next iteration
        reset(ipAddressLogRepository);
        when(ipAddressLogRepository.save(any(IpAddressLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
    
    /**
     * Property: For any IP address modification operation, the system should create an audit log entry
     * with the correct old IP, new IP, user ID, admin user ID, and action
     */
    @Property(trials = 100)
    public void testIpModificationAuditLogging(String oldIpAddress, String newIpAddress) {
        // Use a valid action from the enum
        String validAction = "ASSIGN";
        
        Long userId = 789L;
        Long adminUserId = 101L;
        
        // Call the method under test
        ipPrivacyService.logIpAddressModification(oldIpAddress, newIpAddress, userId, adminUserId, validAction);
        
        // Verify that save was called
        ArgumentCaptor<IpAddressLog> logCaptor = ArgumentCaptor.forClass(IpAddressLog.class);
        verify(ipAddressLogRepository, times(1)).save(logCaptor.capture());
        
        IpAddressLog savedLog = logCaptor.getValue();
        
        // Verify the log entry properties
        assertEquals("User ID should match", userId, savedLog.getUserId());
        assertEquals("Admin user ID should match", adminUserId, savedLog.getAdminUserId());
        assertEquals("Action should match", IpAddressAction.valueOf(validAction), savedLog.getAction());
        assertNotNull("Timestamp should be set", savedLog.getTimestamp());
        assertNotNull("Details should be set", savedLog.getDetails());
        
        // The IP address field should contain the new IP if available, otherwise the old IP
        String expectedIpAddress = newIpAddress != null ? newIpAddress : oldIpAddress;
        assertEquals("IP address should match expected", expectedIpAddress, savedLog.getIpAddress());
        
        // Reset mock for next iteration
        reset(ipAddressLogRepository);
        when(ipAddressLogRepository.save(any(IpAddressLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
    
    /**
     * Property: For any audit logging operation, the system should handle null user IDs gracefully
     */
    @Property(trials = 50)
    public void testAuditLoggingWithNullUserIds(String ipAddress) {
        String action = "VIEW";
        
        // Test with null user ID and admin user ID
        ipPrivacyService.logIpAddressAccess(ipAddress, null, null, action);
        
        // Verify that save was called
        ArgumentCaptor<IpAddressLog> logCaptor = ArgumentCaptor.forClass(IpAddressLog.class);
        verify(ipAddressLogRepository, times(1)).save(logCaptor.capture());
        
        IpAddressLog savedLog = logCaptor.getValue();
        
        // Verify the log entry properties
        assertNull("User ID should be null", savedLog.getUserId());
        assertEquals("IP address should match", ipAddress, savedLog.getIpAddress());
        assertNull("Admin user ID should be null", savedLog.getAdminUserId());
        assertEquals("Action should match", IpAddressAction.VIEW, savedLog.getAction());
        assertNotNull("Timestamp should be set", savedLog.getTimestamp());
        assertNotNull("Details should be set", savedLog.getDetails());
        
        // Reset mock for next iteration
        reset(ipAddressLogRepository);
        when(ipAddressLogRepository.save(any(IpAddressLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
    
    /**
     * Property: For any audit logging operation, the system should continue functioning
     * even if the repository save operation fails
     */
    @Property(trials = 50)
    public void testAuditLoggingResilience(String ipAddress) {
        // Configure repository to throw exception
        when(ipAddressLogRepository.save(any(IpAddressLog.class))).thenThrow(new RuntimeException("Database error"));
        
        String action = "VIEW";
        Long userId = 123L;
        Long adminUserId = 456L;
        
        // This should not throw an exception - the service should handle it gracefully
        try {
            ipPrivacyService.logIpAddressAccess(ipAddress, userId, adminUserId, action);
            // If we reach here, the method handled the exception gracefully
            assertTrue("Method should handle repository exceptions gracefully", true);
        } catch (Exception e) {
            fail("Method should not propagate repository exceptions: " + e.getMessage());
        }
        
        // Verify that save was attempted
        verify(ipAddressLogRepository, times(1)).save(any(IpAddressLog.class));
        
        // Reset mock for next iteration
        reset(ipAddressLogRepository);
        when(ipAddressLogRepository.save(any(IpAddressLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
    
    /**
     * Property: For any audit logging operation, the details field should contain
     * anonymized IP addresses for privacy protection
     */
    @Property(trials = 50)
    public void testAuditLoggingPrivacyInDetails(String ipAddress) {
        // Mock anonymization
        when(ipAddressService.getUnknownIpDefault()).thenReturn("Unknown");
        
        String action = "VIEW";
        Long userId = 123L;
        Long adminUserId = 456L;
        
        // Call the method under test
        ipPrivacyService.logIpAddressAccess(ipAddress, userId, adminUserId, action);
        
        // Verify that save was called
        ArgumentCaptor<IpAddressLog> logCaptor = ArgumentCaptor.forClass(IpAddressLog.class);
        verify(ipAddressLogRepository, times(1)).save(logCaptor.capture());
        
        IpAddressLog savedLog = logCaptor.getValue();
        
        // Verify that details contain information but IP is anonymized
        String details = savedLog.getDetails();
        assertNotNull("Details should not be null", details);
        assertTrue("Details should contain access type", details.contains("accessType"));
        assertTrue("Details should contain IP address field", details.contains("ipAddress"));
        
        // The details should not contain the original IP address if it's sensitive
        if (ipAddress != null && !ipAddress.equals("Unknown") && !ipAddress.trim().isEmpty()) {
            // Details should contain anonymized version, not the original
            String anonymizedIp = ipPrivacyService.anonymizeIpAddress(ipAddress);
            assertTrue("Details should contain anonymized IP", details.contains(anonymizedIp));
        }
        
        // Reset mock for next iteration
        reset(ipAddressLogRepository);
        when(ipAddressLogRepository.save(any(IpAddressLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}