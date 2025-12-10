package com.bidb.personetakip.service;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import jakarta.servlet.http.HttpServletRequest;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * Generator for HttpServletRequest objects with various IP address scenarios.
 */
public class HttpServletRequestGenerator extends Generator<HttpServletRequest> {
    
    private static final List<String> VALID_IPV4_ADDRESSES = Arrays.asList(
        "192.168.1.1",
        "10.0.0.1", 
        "172.16.0.1",
        "203.0.113.1",
        "8.8.8.8",
        "127.0.0.1"
    );
    
    private static final List<String> VALID_IPV6_ADDRESSES = Arrays.asList(
        "2001:db8::1",
        "::1",
        "fe80::1",
        "2001:0db8:85a3:0000:0000:8a2e:0370:7334",
        "2001:db8:85a3::8a2e:370:7334"
    );
    
    private static final List<String> PROXY_HEADERS = Arrays.asList(
        "X-Forwarded-For",
        "X-Real-IP",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP"
    );
    
    public HttpServletRequestGenerator() {
        super(HttpServletRequest.class);
    }
    
    @Override
    public HttpServletRequest generate(SourceOfRandomness random, GenerationStatus status) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        
        // Randomly choose scenario
        int scenario = random.nextInt(0, 4);
        
        switch (scenario) {
            case 0: // Direct IP in remote address
                generateDirectIpRequest(request, random);
                break;
            case 1: // IP in proxy header
                generateProxyIpRequest(request, random);
                break;
            case 2: // Multiple IPs in X-Forwarded-For
                generateMultipleIpRequest(request, random);
                break;
            case 3: // No valid IP (should return unknown)
                generateNoIpRequest(request);
                break;
        }
        
        return request;
    }
    
    private void generateDirectIpRequest(HttpServletRequest request, SourceOfRandomness random) {
        String ip = getRandomValidIp(random);
        when(request.getRemoteAddr()).thenReturn(ip);
        
        // Clear proxy headers
        for (String header : PROXY_HEADERS) {
            when(request.getHeader(header)).thenReturn(null);
        }
    }
    
    private void generateProxyIpRequest(HttpServletRequest request, SourceOfRandomness random) {
        String ip = getRandomValidIp(random);
        String header = PROXY_HEADERS.get(random.nextInt(PROXY_HEADERS.size()));
        
        when(request.getHeader(header)).thenReturn(ip);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1"); // Proxy IP
        
        // Clear other headers
        for (String otherHeader : PROXY_HEADERS) {
            if (!otherHeader.equals(header)) {
                when(request.getHeader(otherHeader)).thenReturn(null);
            }
        }
    }
    
    private void generateMultipleIpRequest(HttpServletRequest request, SourceOfRandomness random) {
        String firstIp = getRandomValidIp(random);
        String secondIp = getRandomValidIp(random);
        String multipleIps = firstIp + ", " + secondIp + ", 127.0.0.1";
        
        when(request.getHeader("X-Forwarded-For")).thenReturn(multipleIps);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        // Clear other headers
        for (String header : PROXY_HEADERS) {
            if (!"X-Forwarded-For".equals(header)) {
                when(request.getHeader(header)).thenReturn(null);
            }
        }
    }
    
    private void generateNoIpRequest(HttpServletRequest request) {
        when(request.getRemoteAddr()).thenReturn(null);
        
        // Set invalid or null headers
        for (String header : PROXY_HEADERS) {
            when(request.getHeader(header)).thenReturn(null);
        }
    }
    
    private String getRandomValidIp(SourceOfRandomness random) {
        boolean useIpv6 = random.nextBoolean();
        
        if (useIpv6) {
            return VALID_IPV6_ADDRESSES.get(random.nextInt(VALID_IPV6_ADDRESSES.size()));
        } else {
            return VALID_IPV4_ADDRESSES.get(random.nextInt(VALID_IPV4_ADDRESSES.size()));
        }
    }
}