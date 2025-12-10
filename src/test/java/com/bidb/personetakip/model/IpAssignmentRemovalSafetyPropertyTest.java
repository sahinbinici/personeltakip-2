package com.bidb.personetakip.model;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Property-based test for IP assignment removal safety
 * **Feature: ip-tracking, Property 15: IP Assignment Removal Safety**
 * **Validates: Requirements 3.5**
 */
@RunWith(JUnitQuickcheck.class)
public class IpAssignmentRemovalSafetyPropertyTest {

    /**
     * Property 15: IP Assignment Removal Safety
     * For any IP assignment removal operation, the system should not affect other user account data
     */
    @Property(trials = 100)
    public void ipRemovalShouldNotAffectUserBasicData(
            int a, int b, int c, int d,
            String tcNo, String personnelNo, String firstName, String lastName, 
            String mobilePhone, String passwordHash) {
        
        // Generate valid IPv4 address
        String ipAddress = (Math.abs(a) % 256) + "." + (Math.abs(b) % 256) + "." + 
                          (Math.abs(c) % 256) + "." + (Math.abs(d) % 256);
        
        User user = createTestUser(tcNo, personnelNo, firstName, lastName, mobilePhone, passwordHash);
        
        // Capture original user data
        String originalTcNo = user.getTcNo();
        String originalPersonnelNo = user.getPersonnelNo();
        String originalFirstName = user.getFirstName();
        String originalLastName = user.getLastName();
        String originalMobilePhone = user.getMobilePhone();
        String originalPasswordHash = user.getPasswordHash();
        UserRole originalRole = user.getRole();
        String originalDepartmentCode = user.getDepartmentCode();
        String originalDepartmentName = user.getDepartmentName();
        String originalTitleCode = user.getTitleCode();
        LocalDateTime originalCreatedAt = user.getCreatedAt();
        LocalDateTime originalUpdatedAt = user.getUpdatedAt();
        LocalDateTime originalLastLoginAt = user.getLastLoginAt();
        
        // Add and then remove IP address
        user.addAssignedIpAddress(ipAddress);
        boolean removed = user.removeAssignedIpAddress(ipAddress);
        assertTrue("IP address should be removed successfully", removed);
        
        // Verify all other user data remains unchanged
        assertEquals("TC number should not be affected by IP removal", 
            originalTcNo, user.getTcNo());
        assertEquals("Personnel number should not be affected by IP removal", 
            originalPersonnelNo, user.getPersonnelNo());
        assertEquals("First name should not be affected by IP removal", 
            originalFirstName, user.getFirstName());
        assertEquals("Last name should not be affected by IP removal", 
            originalLastName, user.getLastName());
        assertEquals("Mobile phone should not be affected by IP removal", 
            originalMobilePhone, user.getMobilePhone());
        assertEquals("Password hash should not be affected by IP removal", 
            originalPasswordHash, user.getPasswordHash());
        assertEquals("User role should not be affected by IP removal", 
            originalRole, user.getRole());
        assertEquals("Department code should not be affected by IP removal", 
            originalDepartmentCode, user.getDepartmentCode());
        assertEquals("Department name should not be affected by IP removal", 
            originalDepartmentName, user.getDepartmentName());
        assertEquals("Title code should not be affected by IP removal", 
            originalTitleCode, user.getTitleCode());
        assertEquals("Created timestamp should not be affected by IP removal", 
            originalCreatedAt, user.getCreatedAt());
        assertEquals("Updated timestamp should not be affected by IP removal", 
            originalUpdatedAt, user.getUpdatedAt());
        assertEquals("Last login timestamp should not be affected by IP removal", 
            originalLastLoginAt, user.getLastLoginAt());
    }

    @Property(trials = 100)
    public void partialIpRemovalShouldNotAffectOtherIps(
            int a1, int b1, int c1, int d1,
            int a2, int b2, int c2, int d2,
            String tcNo, String personnelNo, String firstName, String lastName, 
            String mobilePhone, String passwordHash) {
        
        User user = createTestUser(tcNo, personnelNo, firstName, lastName, mobilePhone, passwordHash);
        
        // Generate two different valid IPv4 addresses
        String ip1 = (Math.abs(a1) % 256) + "." + (Math.abs(b1) % 256) + "." + 
                    (Math.abs(c1) % 256) + "." + (Math.abs(d1) % 256);
        String ip2 = (Math.abs(a2) % 256) + "." + (Math.abs(b2) % 256) + "." + 
                    (Math.abs(c2) % 256) + "." + (Math.abs(d2) % 256);
        
        // Only proceed if IPs are different
        if (!ip1.equals(ip2)) {
            // Add multiple IP addresses
            user.addAssignedIpAddress(ip1);
            user.addAssignedIpAddress(ip2);
            
            // Capture original state
            List<String> originalIps = user.getAssignedIpAddressesList();
            assertEquals("Should have 2 assigned IPs initially", 2, originalIps.size());
            
            // Remove one IP address
            boolean removed = user.removeAssignedIpAddress(ip1);
            assertTrue("IP address should be removed successfully: " + ip1, removed);
            
            // Verify the other IP is still there and unchanged
            List<String> remainingIps = user.getAssignedIpAddressesList();
            assertEquals("Should have 1 assigned IP after removal", 1, remainingIps.size());
            assertTrue("Other IP address should remain unchanged: " + ip2, 
                remainingIps.contains(ip2));
            assertFalse("Removed IP address should not be in the list: " + ip1, 
                remainingIps.contains(ip1));
            
            // Verify the remaining IP is exactly the same
            assertTrue("Other IP address should still be assigned: " + ip2, 
                user.hasAssignedIpAddress(ip2));
        }
    }

    @Property(trials = 100)
    public void removalOfNonExistentIpShouldBeSafe(
            int a1, int b1, int c1, int d1,
            int a2, int b2, int c2, int d2,
            String tcNo, String personnelNo, String firstName, String lastName, 
            String mobilePhone, String passwordHash) {
        
        User user = createTestUser(tcNo, personnelNo, firstName, lastName, mobilePhone, passwordHash);
        
        // Generate two different valid IPv4 addresses
        String existingIp = (Math.abs(a1) % 256) + "." + (Math.abs(b1) % 256) + "." + 
                           (Math.abs(c1) % 256) + "." + (Math.abs(d1) % 256);
        String nonExistentIp = (Math.abs(a2) % 256) + "." + (Math.abs(b2) % 256) + "." + 
                              (Math.abs(c2) % 256) + "." + (Math.abs(d2) % 256);
        
        // Only proceed if IPs are different
        if (!existingIp.equals(nonExistentIp)) {
            // Add one IP address
            user.addAssignedIpAddress(existingIp);
            
            // Capture original state
            String originalTcNo = user.getTcNo();
            String originalFirstName = user.getFirstName();
            List<String> originalIps = user.getAssignedIpAddressesList();
            
            // Try to remove non-existent IP
            boolean removed = user.removeAssignedIpAddress(nonExistentIp);
            assertFalse("Removing non-existent IP should return false", removed);
            
            // Verify user data is completely unchanged
            assertEquals("TC number should not be affected by failed IP removal", 
                originalTcNo, user.getTcNo());
            assertEquals("First name should not be affected by failed IP removal", 
                originalFirstName, user.getFirstName());
            assertEquals("Assigned IPs should not be affected by failed IP removal", 
                originalIps, user.getAssignedIpAddressesList());
            assertTrue("Existing IP should still be assigned after failed removal", 
                user.hasAssignedIpAddress(existingIp));
        }
    }

    @Property(trials = 100)
    public void clearAllIpsShouldBeSafe(
            int a1, int b1, int c1, int d1,
            int a2, int b2, int c2, int d2,
            String tcNo, String personnelNo, String firstName, String lastName, 
            String mobilePhone, String passwordHash) {
        
        User user = createTestUser(tcNo, personnelNo, firstName, lastName, mobilePhone, passwordHash);
        
        // Capture original user data
        String originalTcNo = user.getTcNo();
        String originalPersonnelNo = user.getPersonnelNo();
        String originalFirstName = user.getFirstName();
        String originalLastName = user.getLastName();
        UserRole originalRole = user.getRole();
        
        // Generate and add multiple IP addresses
        String ip1 = (Math.abs(a1) % 256) + "." + (Math.abs(b1) % 256) + "." + 
                    (Math.abs(c1) % 256) + "." + (Math.abs(d1) % 256);
        String ip2 = (Math.abs(a2) % 256) + "." + (Math.abs(b2) % 256) + "." + 
                    (Math.abs(c2) % 256) + "." + (Math.abs(d2) % 256);
        
        user.addAssignedIpAddress(ip1);
        if (!ip1.equals(ip2)) {
            user.addAssignedIpAddress(ip2);
        }
        
        // Clear all IP addresses
        user.clearAssignedIpAddresses();
        
        // Verify all IPs are cleared
        List<String> remainingIps = user.getAssignedIpAddressesList();
        assertTrue("All IP addresses should be cleared", remainingIps.isEmpty());
        
        // Verify all other user data remains unchanged
        assertEquals("TC number should not be affected by clearing IPs", 
            originalTcNo, user.getTcNo());
        assertEquals("Personnel number should not be affected by clearing IPs", 
            originalPersonnelNo, user.getPersonnelNo());
        assertEquals("First name should not be affected by clearing IPs", 
            originalFirstName, user.getFirstName());
        assertEquals("Last name should not be affected by clearing IPs", 
            originalLastName, user.getLastName());
        assertEquals("User role should not be affected by clearing IPs", 
            originalRole, user.getRole());
    }

    @Property(trials = 50)
    public void removalWithNullInputShouldBeSafe(
            int a, int b, int c, int d,
            String tcNo, String personnelNo, String firstName, String lastName, 
            String mobilePhone, String passwordHash) {
        
        User user = createTestUser(tcNo, personnelNo, firstName, lastName, mobilePhone, passwordHash);
        
        // Generate and add an IP address
        String existingIp = (Math.abs(a) % 256) + "." + (Math.abs(b) % 256) + "." + 
                           (Math.abs(c) % 256) + "." + (Math.abs(d) % 256);
        user.addAssignedIpAddress(existingIp);
        
        // Capture original state
        String originalTcNo = user.getTcNo();
        List<String> originalIps = user.getAssignedIpAddressesList();
        
        // Try to remove null IP
        boolean removed = user.removeAssignedIpAddress(null);
        assertFalse("Removing null IP should return false", removed);
        
        // Verify user data is completely unchanged
        assertEquals("TC number should not be affected by null IP removal", 
            originalTcNo, user.getTcNo());
        assertEquals("Assigned IPs should not be affected by null IP removal", 
            originalIps, user.getAssignedIpAddressesList());
        assertTrue("Existing IP should still be assigned after null removal attempt", 
            user.hasAssignedIpAddress(existingIp));
    }

    @Property(trials = 50)
    public void removalWithEmptyStringShouldBeSafe(
            int a, int b, int c, int d,
            String tcNo, String personnelNo, String firstName, String lastName, 
            String mobilePhone, String passwordHash) {
        
        User user = createTestUser(tcNo, personnelNo, firstName, lastName, mobilePhone, passwordHash);
        
        // Generate and add an IP address
        String existingIp = (Math.abs(a) % 256) + "." + (Math.abs(b) % 256) + "." + 
                           (Math.abs(c) % 256) + "." + (Math.abs(d) % 256);
        user.addAssignedIpAddress(existingIp);
        
        // Capture original state
        String originalTcNo = user.getTcNo();
        List<String> originalIps = user.getAssignedIpAddressesList();
        
        // Try to remove empty string IP
        boolean removed = user.removeAssignedIpAddress("");
        assertFalse("Removing empty string IP should return false", removed);
        
        // Verify user data is completely unchanged
        assertEquals("TC number should not be affected by empty string IP removal", 
            originalTcNo, user.getTcNo());
        assertEquals("Assigned IPs should not be affected by empty string IP removal", 
            originalIps, user.getAssignedIpAddressesList());
        assertTrue("Existing IP should still be assigned after empty string removal attempt", 
            user.hasAssignedIpAddress(existingIp));
    }

    private User createTestUser(String tcNo, String personnelNo, String firstName, 
                               String lastName, String mobilePhone, String passwordHash) {
        return User.builder()
                .tcNo(tcNo != null ? tcNo : "12345678901")
                .personnelNo(personnelNo != null ? personnelNo : "TEST001")
                .firstName(firstName != null ? firstName : "Test")
                .lastName(lastName != null ? lastName : "User")
                .mobilePhone(mobilePhone != null ? mobilePhone : "5551234567")
                .passwordHash(passwordHash != null ? passwordHash : "hashedPassword")
                .role(UserRole.NORMAL_USER)
                .build();
    }
}