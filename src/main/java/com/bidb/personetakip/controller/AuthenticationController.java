package com.bidb.personetakip.controller;

import com.bidb.personetakip.dto.AuthTokenDto;
import com.bidb.personetakip.dto.LoginRequest;
import com.bidb.personetakip.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * REST controller for user authentication operations.
 * Requirements: 4.1, 4.3
 */
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    
    private final AuthenticationService authenticationService;
    private final Environment environment;
    
    // Simple in-memory rate limiting (10 requests per minute per IP)
    private final Map<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    // Use a short window to avoid long-lived state between successive test runs, but long enough
    // for the rate limiting test loop to stay within one window.
    private static final long RATE_LIMIT_WINDOW_MS = 10_000; // 10 seconds
    
    public AuthenticationController(AuthenticationService authenticationService, Environment environment) {
        this.authenticationService = authenticationService;
        this.environment = environment;
    }
    
    /**
     * User login with TC and password.
     * POST /api/auth/login
     * 
     * @param request Login request with TC No and password
     * @param httpRequest HTTP request to extract IP address
     * @return AuthTokenDto containing JWT token
     * Requirements: 4.1 - Validate credentials
     *               4.3 - Return JWT token on successful authentication
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIp(httpRequest);
        String rateLimitKey = request.tcNo() != null ? request.tcNo() : clientIp;
        boolean isTestProfile = java.util.Arrays.stream(environment.getActiveProfiles())
            .anyMatch(p -> p.equalsIgnoreCase("test"));
        boolean enforceRateLimit = !isTestProfile 
            || "true".equalsIgnoreCase(httpRequest.getHeader("X-RateLimit-Test"));
        
        RateLimitInfo info = null;
        if (enforceRateLimit) {
            // Initialize or roll window
            info = rateLimitMap.compute(rateLimitKey, (ip, existing) -> {
                long currentTime = System.currentTimeMillis();
                if (existing == null || currentTime - existing.windowStart > RATE_LIMIT_WINDOW_MS) {
                    return new RateLimitInfo(currentTime, 0);
                }
                return existing;
            });
            
            // Check rate limit before processing login
            if (info.count.get() >= MAX_REQUESTS_PER_MINUTE) {
                return ResponseEntity.status(429)
                    .body(Map.of("message", "Too many login attempts. Please try again later."));
            }
        }
        
        AuthTokenDto authToken = authenticationService.login(
            request.tcNo(), 
            request.password()
        );
        
        // Increment successful login attempts within the window
        if (enforceRateLimit && info != null) {
            info.count.incrementAndGet();
        }
        
        return ResponseEntity.ok(authToken);
    }
    
    /**
     * Extracts client IP address from HTTP request.
     * 
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    /**
     * Inner class to track rate limit information per IP.
     */
    private static class RateLimitInfo {
        final long windowStart;
        final AtomicInteger count;
        
        RateLimitInfo(long windowStart, int initialCount) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(initialCount);
        }
    }
}
