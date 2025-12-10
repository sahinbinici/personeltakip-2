package com.bidb.personetakip.service;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Property-based test for multiple IP address support functionality.
 * 
 * Feature: ip-tracking, Property 14: Multiple IP Address Support
 * Validates: Requirements 3.4
 * 
 * For any user IP assignment, the system should support multiple IP addresses 
 * separated by commas or semicolons.
 */
@RunWith(JUnitQuickcheck.class)
public class MultipleIpAddressSupportPropertyTest {
    
    private IpComplianceService ipComplianceService;
    private IpAddressService ipAddressService;
    
    @Before
    public void setUp() {
        ipAddressService = new IpAddressServiceImpl();
        ipComplianceService = new IpComplianceServiceImpl();
        // Manually inject the dependency since we're not using Spring context
        ((IpComplianceServiceImpl) ipComplianceService).ipAddressService = ipAddressService;
    }
    
    /**
     * Property: Comma-separated IP addresses should be parsed correctly
     */
    @Property(trials = 100)
    public void commaSeparatedIpAddressesShouldBeParsedCorrectly() {
        String multipleIps = "192.168.1.100,10.0.0.50,172.16.0.10";
        
        List<String> parsedIps = ipComplianceService.parseAssignedIpAddresses(multipleIps);
        
        assertEquals("Should parse exactly 3 IP addresses", 3, parsedIps.size());
        assertTrue("Should contain first IP", parsedIps.contains("192.168.1.100"));
        assertTrue("Should contain second IP", parsedIps.contains("10.0.0.50"));
        assertTrue("Should contain third IP", parsedIps.contains("172.16.0.10"));
    }
    
    /**
     * Property: Semicolon-separated IP addresses should be parsed correctly
     */
    @Property(trials = 100)
    public void semicolonSeparatedIpAddressesShouldBeParsedCorrectly() {
        String multipleIps = "192.168.1.100;10.0.0.50;172.16.0.10";
        
        List<String> parsedIps = ipComplianceService.parseAssignedIpAddresses(multipleIps);
        
        assertEquals("Should parse exactly 3 IP addresses", 3, parsedIps.size());
        assertTrue("Should contain first IP", parsedIps.contains("192.168.1.100"));
        assertTrue("Should contain second IP", parsedIps.contains("10.0.0.50"));
        assertTrue("Should contain third IP", parsedIps.contains("172.16.0.10"));
    }
    
    /**
     * Property: Mixed comma and semicolon separators should be handled
     */
    @Property(trials = 100)
    public void mixedSeparatorsShouldBeHandled() {
        String multipleIps = "192.168.1.100,10.0.0.50;172.16.0.10";
        
        List<String> parsedIps = ipComplianceService.parseAssignedIpAddresses(multipleIps);
        
        assertEquals("Should parse exactly 3 IP addresses", 3, parsedIps.size());
        assertTrue("Should contain first IP", parsedIps.contains("192.168.1.100"));
        assertTrue("Should contain second IP", parsedIps.contains("10.0.0.50"));
        assertTrue("Should contain third IP", parsedIps.contains("172.16.0.10"));
    }
    
    /**
     * Property: Whitespace around IP addresses should be trimmed
     */
    @Property(trials = 100)
    public void whitespaceShouldBeTrimmed() {
        String multipleIps = " 192.168.1.100 , 10.0.0.50  ;  172.16.0.10 ";
        
        List<String> parsedIps = ipComplianceService.parseAssignedIpAddresses(multipleIps);
        
        assertEquals("Should parse exactly 3 IP addresses", 3, parsedIps.size());
        assertTrue("Should contain first IP without whitespace", parsedIps.contains("192.168.1.100"));
        assertTrue("Should contain second IP without whitespace", parsedIps.contains("10.0.0.50"));
        assertTrue("Should contain third IP without whitespace", parsedIps.contains("172.16.0.10"));
    }
    
    /**
     * Property: Empty strings and null should return empty list
     */
    @Property(trials = 100)
    public void emptyInputsShouldReturnEmptyList() {
        List<String> nullResult = ipComplianceService.parseAssignedIpAddresses(null);
        List<String> emptyResult = ipComplianceService.parseAssignedIpAddresses("");
        List<String> whitespaceResult = ipComplianceService.parseAssignedIpAddresses("   ");
        
        assertTrue("Null input should return empty list", nullResult.isEmpty());
        assertTrue("Empty string should return empty list", emptyResult.isEmpty());
        assertTrue("Whitespace-only string should return empty list", whitespaceResult.isEmpty());
    }
    
    /**
     * Property: Single IP address should be parsed as single-item list
     */
    @Property(trials = 100)
    public void singleIpAddressShouldBeParsedAsSingleItemList() {
        String singleIp = "192.168.1.100";
        
        List<String> parsedIps = ipComplianceService.parseAssignedIpAddresses(singleIp);
        
        assertEquals("Should parse exactly 1 IP address", 1, parsedIps.size());
        assertEquals("Should contain the single IP", "192.168.1.100", parsedIps.get(0));
    }
    
    /**
     * Property: IPv6 addresses should be supported in multiple IP assignments
     */
    @Property(trials = 100)
    public void ipv6AddressesShouldBeSupportedInMultipleAssignments() {
        String multipleIps = "192.168.1.100,2001:db8::1,::1";
        
        List<String> parsedIps = ipComplianceService.parseAssignedIpAddresses(multipleIps);
        
        assertEquals("Should parse exactly 3 IP addresses", 3, parsedIps.size());
        assertTrue("Should contain IPv4 address", parsedIps.contains("192.168.1.100"));
        assertTrue("Should contain IPv6 address", parsedIps.contains("2001:db8::1"));
        assertTrue("Should contain localhost IPv6", parsedIps.contains("::1"));
    }
    
    /**
     * Property: IP address matching should work with multiple assigned IPs
     */
    @Property(trials = 100)
    public void ipAddressMatchingShouldWorkWithMultipleAssignedIps() {
        String multipleIps = "192.168.1.100,10.0.0.50,172.16.0.10";
        
        // Test matching with first IP
        boolean match1 = ipComplianceService.isIpAddressMatch("192.168.1.100", multipleIps);
        assertTrue("Should match first assigned IP", match1);
        
        // Test matching with middle IP
        boolean match2 = ipComplianceService.isIpAddressMatch("10.0.0.50", multipleIps);
        assertTrue("Should match middle assigned IP", match2);
        
        // Test matching with last IP
        boolean match3 = ipComplianceService.isIpAddressMatch("172.16.0.10", multipleIps);
        assertTrue("Should match last assigned IP", match3);
        
        // Test non-matching IP
        boolean noMatch = ipComplianceService.isIpAddressMatch("203.0.113.1", multipleIps);
        assertFalse("Should not match non-assigned IP", noMatch);
    }
    
    /**
     * Property: Validation should work with multiple IP addresses
     */
    @Property(trials = 100)
    public void validationShouldWorkWithMultipleIpAddresses() {
        // All valid IPs
        String validMultipleIps = "192.168.1.100,10.0.0.50,2001:db8::1";
        boolean allValid = ipComplianceService.validateAssignedIpAddresses(validMultipleIps);
        assertTrue("All valid IPs should pass validation", allValid);
        
        // Mix of valid and invalid IPs
        String mixedIps = "192.168.1.100,invalid-ip,10.0.0.50";
        boolean mixedValid = ipComplianceService.validateAssignedIpAddresses(mixedIps);
        assertFalse("Mix of valid and invalid IPs should fail validation", mixedValid);
        
        // All invalid IPs
        String invalidIps = "invalid-ip1,not-an-ip,999.999.999.999";
        boolean allInvalid = ipComplianceService.validateAssignedIpAddresses(invalidIps);
        assertFalse("All invalid IPs should fail validation", allInvalid);
    }
    
    /**
     * Property: Empty segments should be filtered out
     */
    @Property(trials = 100)
    public void emptySegmentsShouldBeFilteredOut() {
        String ipsWithEmptySegments = "192.168.1.100,,10.0.0.50,;,172.16.0.10,";
        
        List<String> parsedIps = ipComplianceService.parseAssignedIpAddresses(ipsWithEmptySegments);
        
        assertEquals("Should parse exactly 3 IP addresses, filtering empty segments", 3, parsedIps.size());
        assertTrue("Should contain first IP", parsedIps.contains("192.168.1.100"));
        assertTrue("Should contain second IP", parsedIps.contains("10.0.0.50"));
        assertTrue("Should contain third IP", parsedIps.contains("172.16.0.10"));
        
        // Ensure no empty strings are in the result
        for (String ip : parsedIps) {
            assertFalse("No empty strings should be in result", ip.isEmpty());
        }
    }
}