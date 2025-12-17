package com.bidb.personetakip.service;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Property-based test for proxy-aware IP extraction functionality.
 * 
 * Feature: ip-tracking, Property 2: Proxy-Aware IP Extraction
 * Validates: Requirements 1.2
 * 
 * For any HTTP request with proxy headers, the system should extract the real client IP address correctly.
 */
@RunWith(JUnitQuickcheck.class)
public class ProxyAwareIpExtractionPropertyTest {
    
    private IpAddressService ipAddressService;
    
    @Before
    public void setUp() {
        ipAddressService = TestConfigurationHelper.createIpAddressService();
    }
    
    /**
     * Property: X-Forwarded-For header should be preferred over remote address
     */
    @Property(trials = 100)
    public void xForwardedForHeaderPreferredOverRemoteAddress() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        
        String realClientIp = "203.0.113.1";
        String proxyIp = "127.0.0.1";
        
        when(request.getHeader("X-Forwarded-For")).thenReturn(realClientIp);
        when(request.getRemoteAddr()).thenReturn(proxyIp);
        
        // Clear other headers
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        
        String extractedIp = ipAddressService.extractClientIpAddress(request);
        
        assertEquals("Should extract real client IP from X-Forwarded-For header", 
                     realClientIp, extractedIp);
    }
    
    /**
     * Property: X-Real-IP header should be used when available
     */
    @Property(trials = 100)
    public void xRealIpHeaderShouldBeUsed() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        
        String realClientIp = "192.168.1.100";
        String proxyIp = "10.0.0.1";
        
        when(request.getHeader("X-Real-IP")).thenReturn(realClientIp);
        when(request.getRemoteAddr()).thenReturn(proxyIp);
        
        // Clear other headers
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        
        String extractedIp = ipAddressService.extractClientIpAddress(request);
        
        assertEquals("Should extract real client IP from X-Real-IP header", 
                     realClientIp, extractedIp);
    }
    
    /**
     * Property: Multiple IPs in X-Forwarded-For should return the first valid IP
     */
    @Property(trials = 100)
    public void multipleIpsInXForwardedForReturnsFirst() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        
        String firstIp = "203.0.113.1";
        String secondIp = "198.51.100.1";
        String multipleIps = firstIp + ", " + secondIp + ", 127.0.0.1";
        
        when(request.getHeader("X-Forwarded-For")).thenReturn(multipleIps);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        // Clear other headers
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        
        String extractedIp = ipAddressService.extractClientIpAddress(request);
        
        assertEquals("Should extract first IP from comma-separated X-Forwarded-For header", 
                     firstIp, extractedIp);
    }
    
    /**
     * Property: Proxy-Client-IP header should be used when other headers are not available
     */
    @Property(trials = 100)
    public void proxyClientIpHeaderShouldBeUsed() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        
        String realClientIp = "172.16.0.100";
        String proxyIp = "10.0.0.1";
        
        when(request.getHeader("Proxy-Client-IP")).thenReturn(realClientIp);
        when(request.getRemoteAddr()).thenReturn(proxyIp);
        
        // Clear other headers
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        
        String extractedIp = ipAddressService.extractClientIpAddress(request);
        
        assertEquals("Should extract real client IP from Proxy-Client-IP header", 
                     realClientIp, extractedIp);
    }
    
    /**
     * Property: Invalid proxy header values should be ignored
     */
    @Property(trials = 100)
    public void invalidProxyHeaderValuesShouldBeIgnored() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        
        String validRemoteAddr = "192.168.1.1";
        
        // Set invalid proxy header values
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getHeader("X-Real-IP")).thenReturn("null");
        when(request.getHeader("Proxy-Client-IP")).thenReturn("-");
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn("");
        
        when(request.getRemoteAddr()).thenReturn(validRemoteAddr);
        
        String extractedIp = ipAddressService.extractClientIpAddress(request);
        
        assertEquals("Should fall back to remote address when proxy headers contain invalid values", 
                     validRemoteAddr, extractedIp);
    }
    
    /**
     * Property: IPv6 addresses in proxy headers should be handled correctly
     */
    @Property(trials = 100)
    public void ipv6AddressesInProxyHeadersHandledCorrectly() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        
        String ipv6Address = "2001:db8::1";
        String proxyIp = "127.0.0.1";
        
        when(request.getHeader("X-Forwarded-For")).thenReturn(ipv6Address);
        when(request.getRemoteAddr()).thenReturn(proxyIp);
        
        // Clear other headers
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        
        String extractedIp = ipAddressService.extractClientIpAddress(request);
        
        assertEquals("Should extract IPv6 address from proxy header", 
                     ipv6Address, extractedIp);
        assertTrue("Extracted IP should be valid", 
                   ipAddressService.isValidIpAddress(extractedIp));
    }
}