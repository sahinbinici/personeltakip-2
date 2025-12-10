package com.bidb.personetakip.model;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Property-based test for assigned IP display in user details
 * **Feature: ip-tracking, Property 13: Assigned IP Display in User Details**
 * **Validates: Requirements 3.3**
 */
@RunWith(JUnitQuickcheck.class)
public class AssignedIpDisplayPropertyTest {

    /**
     * Property 13: Assigned IP Display in User Details
     * For any user with assigned IP addresses, the system should display this information in user details
     */
    @Property(trials = 100)
    public void singleAssignedIpShouldBeDisplayedInUserDetails(
            int a, int b, int c, int d,
            String tcNo, String personnelNo, String firstName, String lastName, 
            String mobilePhone, String passwordHash) {
        
        // Generate valid IPv4 address
        String ipAddress = (Math.abs(a) % 256) + "." + (Math.abs(b) % 256) + "." + 
                          (Math.abs(c) % 256) + "." + (Math.abs(d) % 256);
        
        User user = createTestUser(tcNo, personnelNo, firstName, lastName, mobilePhone, passwordHash);
        
        // Assign IP address
        user.addAssignedIpAddress(ipAddress);
        
        // Test that assigned IP addresses are retrievable
        List<String> assignedIps = user.getAssignedIpAddressesList();
        assertFalse("User should have assigned IP addresses in details", assignedIps.isEmpty());
        assertTrue("Assigned IP should be displayed in user details: " + ipAddress, 
            assignedIps.contains(ipAddress));
        
        // Test that the raw string format is also available for display
        String rawAssignedIps = user.getAssignedIpAddresses();
        assertNotNull("Raw assigned IP addresses should be available for display", rawAssignedIps);
        assertTrue("Raw assigned IP string should contain the assigned IP: " + ipAddress, 
            rawAssignedIps.contains(ipAddress));
    }

    @Property(trials = 100)
    public void multipleAssignedIpsShouldAllBeDisplayedInUserDetails(
            int a1, int b1, int c1, int d1,
            int a2, int b2, int c2, int d2,
            String tcNo, String personnelNo, String firstName, String lastName, 
            String mobilePhone, String passwordHash) {
        
        User user = createTestUser(tcNo, personnelNo, firstName, lastName, mobilePhone, passwordHash);
        
        // Generate two valid IPv4 addresses
        String ip1 = (Math.abs(a1) % 256) + "." + (Math.abs(b1) % 256) + "." + 
                    (Math.abs(c1) % 256) + "." + (Math.abs(d1) % 256);
        String ip2 = (Math.abs(a2) % 256) + "." + (Math.abs(b2) % 256) + "." + 
                    (Math.abs(c2) % 256) + "." + (Math.abs(d2) % 256);
        
        // Assign multiple IP addresses
        user.addAssignedIpAddress(ip1);
        if (!ip1.equals(ip2)) {
            user.addAssignedIpAddress(ip2);
        }
        
        // Test that all assigned IP addresses are retrievable
        List<String> assignedIps = user.getAssignedIpAddressesList();
        assertTrue("User should have assigned IP addresses", !assignedIps.isEmpty());
        assertTrue("First assigned IP should be displayed in user details: " + ip1, 
            assignedIps.contains(ip1));
        if (!ip1.equals(ip2)) {
            assertTrue("Second assigned IP should be displayed in user details: " + ip2, 
                assignedIps.contains(ip2));
        }
        
        // Test that the raw string format contains all IPs
        String rawAssignedIps = user.getAssignedIpAddresses();
        assertNotNull("Raw assigned IP addresses should be available for display", rawAssignedIps);
        assertTrue("Raw assigned IP string should contain first assigned IP: " + ip1, 
            rawAssignedIps.contains(ip1));
        if (!ip1.equals(ip2)) {
            assertTrue("Raw assigned IP string should contain second assigned IP: " + ip2, 
                rawAssignedIps.contains(ip2));
        }
    }

    @Property(trials = 50)
    public void userWithNoAssignedIpsShouldShowEmptyDetails(
            String tcNo, String personnelNo, String firstName, String lastName, 
            String mobilePhone, String passwordHash) {
        
        User user = createTestUser(tcNo, personnelNo, firstName, lastName, mobilePhone, passwordHash);
        
        // Ensure no IP addresses are assigned
        user.clearAssignedIpAddresses();
        
        // Test that no assigned IP addresses are shown
        List<String> assignedIps = user.getAssignedIpAddressesList();
        assertTrue("User with no assigned IPs should show empty list in details", 
            assignedIps.isEmpty());
        
        // Test that raw string is null or empty
        String rawAssignedIps = user.getAssignedIpAddresses();
        assertTrue("User with no assigned IPs should have null or empty raw string", 
            rawAssignedIps == null || rawAssignedIps.trim().isEmpty());
    }

    @Property(trials = 100)
    public void assignedIpDisplayShouldUpdateAfterRemoval(
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
            // Assign two IP addresses
            user.addAssignedIpAddress(ip1);
            user.addAssignedIpAddress(ip2);
            
            // Remove one IP address
            boolean removed = user.removeAssignedIpAddress(ip1);
            assertTrue("IP address should be removed successfully: " + ip1, removed);
            
            // Test that only remaining IP is displayed
            List<String> assignedIps = user.getAssignedIpAddressesList();
            assertEquals("User details should show only remaining IP after removal", 1, assignedIps.size());
            assertTrue("Remaining IP should be displayed in user details: " + ip2, 
                assignedIps.contains(ip2));
            assertFalse("Removed IP should not be displayed in user details: " + ip1, 
                assignedIps.contains(ip1));
            
            // Test raw string format
            String rawAssignedIps = user.getAssignedIpAddresses();
            assertNotNull("Raw assigned IP addresses should be available after removal", rawAssignedIps);
            assertTrue("Raw string should contain remaining IP: " + ip2, 
                rawAssignedIps.contains(ip2));
            assertFalse("Raw string should not contain removed IP: " + ip1, 
                rawAssignedIps.contains(ip1));
        }
    }

    @Property(trials = 100)
    public void assignedIpDisplayShouldBeConsistentAcrossMethods(
            int a, int b, int c, int d,
            String tcNo, String personnelNo, String firstName, String lastName, 
            String mobilePhone, String passwordHash) {
        
        // Generate valid IPv4 address
        String ipAddress = (Math.abs(a) % 256) + "." + (Math.abs(b) % 256) + "." + 
                          (Math.abs(c) % 256) + "." + (Math.abs(d) % 256);
        
        User user1 = createTestUser(tcNo, personnelNo, firstName, lastName, mobilePhone, passwordHash);
        User user2 = createTestUser(tcNo, personnelNo, firstName, lastName, mobilePhone, passwordHash);
        
        // Add IP using add method
        user1.addAssignedIpAddress(ipAddress);
        List<String> ipsFromAdd = user1.getAssignedIpAddressesList();
        
        // Set using list method
        user2.setAssignedIpAddressesList(Arrays.asList(ipAddress));
        List<String> ipsFromList = user2.getAssignedIpAddressesList();
        
        // Both methods should produce consistent display
        assertEquals("IP display should be consistent regardless of assignment method", 
            ipsFromAdd, ipsFromList);
        
        assertTrue("IP should be displayed consistently: " + ipAddress, 
            ipsFromList.contains(ipAddress));
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