package com.bidb.personetakip.service;

import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Property-based test for IP address capture and storage functionality.
 * 
 * Feature: ip-tracking, Property 1: IP Address Capture and Storage
 * Validates: Requirements 1.1
 * 
 * For any entry/exit operation, the system should capture and store the client IP address
 * in the database record.
 */
@RunWith(JUnitQuickcheck.class)
public class IpAddressCapturePropertyTest {
    
    private IpAddressService ipAddressService;
    
    @Before
    public void setUp() {
        ipAddressService = TestConfigurationHelper.createIpAddressService();
    }
    
    /**
     * Property: IP address extraction should always return a non-null, non-empty result
     */
    @Property(trials = 100)
    public void ipAddressExtractionAlwaysReturnsResult(@From(HttpServletRequestGenerator.class) HttpServletRequest request) {
        String extractedIp = ipAddressService.extractClientIpAddress(request);
        
        assertNotNull("Extracted IP address should never be null", extractedIp);
        assertFalse("Extracted IP address should never be empty", extractedIp.trim().isEmpty());
    }
    
    /**
     * Property: When a valid IP is extractable, it should be returned; otherwise unknown default
     */
    @Property(trials = 100)
    public void ipAddressExtractionReturnsValidIpOrDefault(@From(HttpServletRequestGenerator.class) HttpServletRequest request) {
        String extractedIp = ipAddressService.extractClientIpAddress(request);
        
        // Either the extracted IP is valid, or it's the unknown default
        boolean isValidIp = ipAddressService.isValidIpAddress(extractedIp);
        boolean isUnknownDefault = ipAddressService.getUnknownIpDefault().equals(extractedIp);
        
        assertTrue("Extracted IP should be either valid or the unknown default", 
                   isValidIp || isUnknownDefault);
    }
    
    /**
     * Property: IP address extraction should be consistent for the same request
     */
    @Property(trials = 100)
    public void ipAddressExtractionIsConsistent(@From(HttpServletRequestGenerator.class) HttpServletRequest request) {
        String firstExtraction = ipAddressService.extractClientIpAddress(request);
        String secondExtraction = ipAddressService.extractClientIpAddress(request);
        
        assertEquals("IP address extraction should be consistent for the same request", 
                     firstExtraction, secondExtraction);
    }
    
    /**
     * Property: Null request should always return unknown default
     */
    @Property(trials = 100)
    public void nullRequestReturnsUnknownDefault() {
        String extractedIp = ipAddressService.extractClientIpAddress(null);
        
        assertEquals("Null request should return unknown default", 
                     ipAddressService.getUnknownIpDefault(), extractedIp);
    }
}