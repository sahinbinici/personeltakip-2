package com.bidb.personetakip.model;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Property-based test for IP assignment format validation
 * **Feature: ip-tracking, Property 12: IP Assignment Format Validation**
 * **Validates: Requirements 3.2**
 */
@RunWith(JUnitQuickcheck.class)
public class IpAssignmentFormatValidationPropertyTest {

    /**
     * Property 12: IP Assignment Format Validation
     * For any IP address assignment, the system should validate both IPv4 and IPv6 formats before saving
     */
    @Property(trials = 100)
    public void validIpv4AddressesShouldBeAccepted(
            int a, int b, int c, int d,
            String tcNo, String personnelNo, String firstName, String lastName, 
            String mobilePhone, String passwordHash) {
        
        // Generate valid IPv4 address (0-255 for each octet)
        int validA = Math.abs(a) % 256;
        int validB = Math.abs(b) % 256;
        int validC = Math.abs(c) % 256;
        int validD = Math.abs(d) % 256;
        String ipAddress = validA + "." + validB + "." + validC + "." + validD;
        
        User user = createTestUser(tcNo, personnelNo, firstName, lastName, mobilePhone, passwordHash);
        
        // Test individual IP validation
        assertTrue("Valid IPv4 address should be accepted: " + ipAddress, 
            User.isValidIpAddress(ipAddress));
        
        // Test adding to user
        boolean added = user.addAssignedIpAddress(ipAddress);
        assertTrue("Valid IPv4 address should be added successfully: " + ipAddress, added);
        
        // Test that it's in the list
        assertTrue("Added IPv4 address should be found in assigned list: " + ipAddress, 
            user.hasAssignedIpAddress(ipAddress));
        
        // Test validation of all assigned IPs
        assertTrue("User should have valid assigned IP addresses after adding valid IPv4: " + ipAddress, 
            user.hasValidAssignedIpAddresses());
    }

    @Property(trials = 50)
    public void validIpv6AddressesShouldBeAccepted(
            String tcNo, String personnelNo, String firstName, String lastName, 
            String mobilePhone, String passwordHash) {
        
        // Test with common valid IPv6 addresses
        String[] validIpv6Addresses = {
            "2001:0db8:85a3:0000:0000:8a2e:0370:7334",
            "2001:db8:85a3::8a2e:370:7334",
            "::1",
            "fe80::1",
            "2001:db8::1"
        };
        
        for (String ipAddress : validIpv6Addresses) {
            User user = createTestUser(tcNo, personnelNo, firstName, lastName, mobilePhone, passwordHash);
            
            // Test individual IP validation
            assertTrue("Valid IPv6 address should be accepted: " + ipAddress, 
                User.isValidIpAddress(ipAddress));
            
            // Test adding to user
            boolean added = user.addAssignedIpAddress(ipAddress);
            assertTrue("Valid IPv6 address should be added successfully: " + ipAddress, added);
            
            // Test that it's in the list
            assertTrue("Added IPv6 address should be found in assigned list: " + ipAddress, 
                user.hasAssignedIpAddress(ipAddress));
            
            // Test validation of all assigned IPs
            assertTrue("User should have valid assigned IP addresses after adding valid IPv6: " + ipAddress, 
                user.hasValidAssignedIpAddresses());
        }
    }

    @Property(trials = 50)
    public void invalidIpAddressesShouldBeRejected(
            String tcNo, String personnelNo, String firstName, String lastName, 
            String mobilePhone, String passwordHash) {
        
        // Test with common invalid IP addresses
        String[] invalidIpAddresses = {
            "256.1.1.1",           // Invalid IPv4 - octet > 255
            "1.1.1",               // Invalid IPv4 - missing octet
            "1.1.1.1.1",           // Invalid IPv4 - too many octets
            "abc.def.ghi.jkl",     // Invalid IPv4 - non-numeric
            "192.168.1.-1",        // Invalid IPv4 - negative number
            "",                    // Empty string
            "   ",                 // Whitespace only
            "not-an-ip",           // Random text
            "192.168.1",           // Incomplete IPv4
            "gggg::1",             // Invalid IPv6 - invalid hex
            "2001:db8::1::2"       // Invalid IPv6 - double ::
        };
        
        for (String invalidIp : invalidIpAddresses) {
            User user = createTestUser(tcNo, personnelNo, firstName, lastName, mobilePhone, passwordHash);
            
            // Test individual IP validation
            assertFalse("Invalid IP address should be rejected: " + invalidIp, 
                User.isValidIpAddress(invalidIp));
            
            // Test adding to user should fail
            boolean added = user.addAssignedIpAddress(invalidIp);
            assertFalse("Invalid IP address should not be added: " + invalidIp, added);
            
            // Test that it's not in the list
            assertFalse("Invalid IP address should not be found in assigned list: " + invalidIp, 
                user.hasAssignedIpAddress(invalidIp));
        }
    }

    @Property(trials = 100)
    public void multipleValidIpAddressesShouldAllBeValid(
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
        
        // Add both IPs
        boolean added1 = user.addAssignedIpAddress(ip1);
        assertTrue("First valid IP should be added: " + ip1, added1);
        
        boolean added2 = user.addAssignedIpAddress(ip2);
        // Only add second IP if it's different from first
        if (!ip1.equals(ip2)) {
            assertTrue("Second valid IP should be added: " + ip2, added2);
        }
        
        // Test that all assigned IPs are valid
        assertTrue("All assigned IP addresses should be valid", 
            user.hasValidAssignedIpAddresses());
        
        // Test that both IPs are in the list
        assertTrue("First IP should be found in assigned list: " + ip1, 
            user.hasAssignedIpAddress(ip1));
        if (!ip1.equals(ip2)) {
            assertTrue("Second IP should be found in assigned list: " + ip2, 
                user.hasAssignedIpAddress(ip2));
        }
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