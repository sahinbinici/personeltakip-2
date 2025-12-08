package com.bidb.personetakip.controller;

import com.bidb.personetakip.dto.AuthTokenDto;
import com.bidb.personetakip.dto.LoginRequest;
import com.bidb.personetakip.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
    
    // Simple in-memory rate limiting (10 requests per minute per IP)
    private final Map<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000; // 1 minute
    
    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
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
        
        // Check rate limit
        if (!checkRateLimit(clientIp)) {
            return ResponseEntity.status(429)
                .body(Map.of("message", "Too many login attempts. Please try again later."));
        }
        
        AuthTokenDto authToken = authenticationService.login(
            request.tcNo(), 
            request.password()
        );
        
        return ResponseEntity.ok(authToken);
    }
    
    /**
     * Checks if the client IP has exceeded the rate limit.
     * 
     * @param clientIp Client IP address
     * @return true if request is allowed, false if rate limit exceeded
     */
    private boolean checkRateLimit(String clientIp) {
        long currentTime = System.currentTimeMillis();
        
        rateLimitMap.compute(clientIp, (ip, info) -> {
            if (info == null || currentTime - info.windowStart > RATE_LIMIT_WINDOW_MS) {
                // New window
                return new RateLimitInfo(currentTime, 1);
            } else {
                // Within current window
                info.count.incrementAndGet();
                return info;
            }
        });
        
        RateLimitInfo info = rateLimitMap.get(clientIp);
        return info.count.get() <= MAX_REQUESTS_PER_MINUTE;
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
