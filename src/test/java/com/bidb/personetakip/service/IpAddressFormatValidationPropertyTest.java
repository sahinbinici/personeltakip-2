package com.bidb.personetakip.service;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Before;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Property-based test for IP address format validation functionality.
 * 
 * Feature: ip-tracking, Property 4: IP Address Format Validation
 * Validates: Requirements 1.4
 * 
 * For any IP address before storage, the system should validate the format and reject invalid IP addresses.
 */
@RunWith(JUnitQuickcheck.class)
public class IpAddressFormatValidationPropertyTest {
    
    private IpAddressService ipAddressService;
    
    @Before
    public void setUp() {
        ipAddressService = new IpAddressServiceImpl();
    }
    
    /**
     * Property: Valid IPv4 addresses should be accepted
     */
    @Property(trials = 100)
    public void validIpv4AddressesShouldBeAccepted() {
        String[] validIpv4Addresses = {
            "0.0.0.0",
            "127.0.0.1",
            "192.168.1.1",
            "10.0.0.1",
            "172.16.0.1",
            "203.0.113.1",
            "8.8.8.8",
            "255.255.255.255",
            "1.1.1.1",
            "192.168.0.255"
        };
        
        for (String ipAddress : validIpv4Addresses) {
            assertTrue("Valid IPv4 address should be accepted: " + ipAddress, 
                       ipAddressService.isValidIpAddress(ipAddress));
        }
    }
    
    /**
     * Property: Valid IPv6 addresses should be accepted
     */
    @Property(trials = 100)
    public void validIpv6AddressesShouldBeAccepted() {
        String[] validIpv6Addresses = {
            "::1",
            "::0",
            "::",
            "2001:db8::1",
            "2001:0db8:85a3:0000:0000:8a2e:0370:7334",
            "2001:db8:85a3::8a2e:370:7334",
            "fe80::1",
            "2001:db8::8a2e:370:7334",
            "::ffff:192.0.2.1",
            "2001:db8:85a3:0:0:8a2e:370:7334"
        };
        
        for (String ipAddress : validIpv6Addresses) {
            assertTrue("Valid IPv6 address should be accepted: " + ipAddress, 
                       ipAddressService.isValidIpAddress(ipAddress));
        }
    }
    
    /**
     * Property: Invalid IPv4 addresses should be rejected
     */
    @Property(trials = 100)
    public void invalidIpv4AddressesShouldBeRejected() {
        String[] invalidIpv4Addresses = {
            "256.1.1.1",
            "1.256.1.1",
            "1.1.256.1",
            "1.1.1.256",
            "999.999.999.999",
            "192.168.1",
            "192.168.1.1.1",
            "192.168.-1.1",
            "192.168.1.-1",
            "a.b.c.d",
            "192.168.1.1a",
            "192.168.1.1.",
            ".192.168.1.1",
            "192..168.1.1",
            "192.168..1.1",
            "192.168.1..1"
        };
        
        for (String ipAddress : invalidIpv4Addresses) {
            assertFalse("Invalid IPv4 address should be rejected: " + ipAddress, 
                        ipAddressService.isValidIpAddress(ipAddress));
        }
    }
    
    /**
     * Property: Invalid IPv6 addresses should be rejected
     */
    @Property(trials = 100)
    public void invalidIpv6AddressesShouldBeRejected() {
        String[] invalidIpv6Addresses = {
            ":::",
            "2001:db8::1::2",
            "2001:db8:85a3::8a2e::370:7334",
            "2001:db8:85a3:0000:0000:8a2e:0370:7334:extra",
            "gggg::1",
            "2001:db8:85a3:0000:0000:8a2e:0370:gggg",
            "2001::db8::1",
            "2001:db8:85a3:0000:0000:8a2e:0370:7334:0000:0000",
            "::ffff:256.0.2.1",
            "::ffff:192.0.2.256"
        };
        
        for (String ipAddress : invalidIpv6Addresses) {
            assertFalse("Invalid IPv6 address should be rejected: " + ipAddress, 
                        ipAddressService.isValidIpAddress(ipAddress));
        }
    }
    
    /**
     * Property: Null and empty strings should be rejected
     */
    @Property(trials = 100)
    public void nullAndEmptyStringsShouldBeRejected() {
        assertFalse("Null should be rejected", 
                    ipAddressService.isValidIpAddress(null));
        assertFalse("Empty string should be rejected", 
                    ipAddressService.isValidIpAddress(""));
        assertFalse("Whitespace-only string should be rejected", 
                    ipAddressService.isValidIpAddress("   "));
        assertFalse("Tab-only string should be rejected", 
                    ipAddressService.isValidIpAddress("\t"));
        assertFalse("Newline-only string should be rejected", 
                    ipAddressService.isValidIpAddress("\n"));
    }
    
    /**
     * Property: Random invalid strings should be rejected
     */
    @Property(trials = 100)
    public void randomInvalidStringsShouldBeRejected() {
        String[] invalidStrings = {
            "not-an-ip",
            "192.168.1.1.extra",
            "192.168.1",
            "192.168",
            "192",
            "hello world",
            "123.456.789.012",
            "192.168.1.1/24",
            "192.168.1.1:8080",
            "http://192.168.1.1",
            "ftp://example.com",
            "localhost",
            "example.com",
            "www.google.com",
            "192.168.1.1 extra",
            "192,168,1,1",
            "192;168;1;1"
        };
        
        for (String invalidString : invalidStrings) {
            assertFalse("Invalid string should be rejected: " + invalidString, 
                        ipAddressService.isValidIpAddress(invalidString));
        }
    }
    
    /**
     * Property: IP validation should be consistent
     */
    @Property(trials = 100)
    public void ipValidationShouldBeConsistent() {
        String[] testIpAddresses = {
            "192.168.1.1",
            "2001:db8::1",
            "invalid-ip",
            null,
            "",
            "256.1.1.1"
        };
        
        for (String ipAddress : testIpAddresses) {
            boolean firstResult = ipAddressService.isValidIpAddress(ipAddress);
            boolean secondResult = ipAddressService.isValidIpAddress(ipAddress);
            
            assertEquals("IP validation should be consistent for: " + ipAddress, 
                         firstResult, secondResult);
        }
    }
    
    /**
     * Property: IP formatting should preserve valid addresses
     */
    @Property(trials = 100)
    public void ipFormattingShouldPreserveValidAddresses() {
        String[] validIpAddresses = {
            "192.168.1.1",
            "10.0.0.1",
            "2001:db8::1",
            "::1",
            "fe80::1"
        };
        
        for (String ipAddress : validIpAddresses) {
            String formatted = ipAddressService.formatIpAddress(ipAddress);
            
            assertNotNull("Formatted IP should not be null", formatted);
            assertNotEquals("Formatted IP should not be unknown default", 
                           ipAddressService.getUnknownIpDefault(), formatted);
            assertTrue("Formatted IP should still be valid", 
                      ipAddressService.isValidIpAddress(formatted));
        }
    }
    
    /**
     * Property: IP formatting should handle invalid addresses gracefully
     */
    @Property(trials = 100)
    public void ipFormattingShouldHandleInvalidAddressesGracefully() {
        String[] invalidInputs = {
            null,
            "",
            "   ",
            "invalid-ip",
            "256.1.1.1"
        };
        
        for (String invalidInput : invalidInputs) {
            String formatted = ipAddressService.formatIpAddress(invalidInput);
            
            assertNotNull("Formatted result should not be null", formatted);
            // For invalid inputs, should return either the unknown default or the trimmed input
            assertTrue("Formatted result should be either unknown default or the trimmed input", 
                      ipAddressService.getUnknownIpDefault().equals(formatted) || 
                      (invalidInput != null && invalidInput.trim().equals(formatted)));
        }
    }
    
    /**
     * Property: IP addresses with leading/trailing whitespace should be accepted after trimming
     */
    @Property(trials = 100)
    public void ipAddressesWithWhitespaceShouldBeAcceptedAfterTrimming() {
        String[] validIpsWithWhitespace = {
            " 192.168.1.1",
            "192.168.1.1 ",
            " 192.168.1.1 ",
            "\t192.168.1.1\t",
            " 2001:db8::1 ",
            "\t::1\n"
        };
        
        for (String ipWithWhitespace : validIpsWithWhitespace) {
            assertTrue("IP address with whitespace should be accepted after trimming: '" + ipWithWhitespace + "'", 
                       ipAddressService.isValidIpAddress(ipWithWhitespace));
        }
    }
}