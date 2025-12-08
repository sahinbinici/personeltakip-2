package com.bidb.personetakip.controller;

import com.bidb.personetakip.dto.AuthTokenDto;
import com.bidb.personetakip.dto.EntryExitRecordDto;
import com.bidb.personetakip.dto.EntryExitRequestDto;
import com.bidb.personetakip.dto.LoginRequest;
import com.bidb.personetakip.dto.QrCodeValidationDto;
import com.bidb.personetakip.service.AuthenticationService;
import com.bidb.personetakip.service.EntryExitService;
import com.bidb.personetakip.service.QrCodeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * REST controller for mobile application API endpoints.
 * Requirements: 7.1, 7.2, 8.1, 8.2, 8.5
 */
@RestController
@RequestMapping("/api/mobil")
public class MobileApiController {
    
    private final AuthenticationService authenticationService;
    private final EntryExitService entryExitService;
    private final QrCodeService qrCodeService;
    
    // Simple in-memory rate limiting (20 requests per minute per user)
    private final Map<Long, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 20;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000; // 1 minute
    
    public MobileApiController(
            AuthenticationService authenticationService,
            EntryExitService entryExitService,
            QrCodeService qrCodeService) {
        this.authenticationService = authenticationService;
        this.entryExitService = entryExitService;
        this.qrCodeService = qrCodeService;
    }
    
    /**
     * Mobile authentication endpoint.
     * POST /api/mobil/login
     * 
     * @param request Login request with TC No and password
     * @return AuthTokenDto containing JWT token
     * Requirements: 7.1 - Validate credentials against local database
     *               7.2 - Generate JWT token with personnel ID and role
     */
    @PostMapping("/login")
    public ResponseEntity<AuthTokenDto> login(@Valid @RequestBody LoginRequest request) {
        AuthTokenDto authToken = authenticationService.login(
            request.tcNo(), 
            request.password()
        );
        return ResponseEntity.ok(authToken);
    }
    
    /**
     * Records entry/exit event with QR code data.
     * POST /api/mobil/giris-cikis-kaydet
     * 
     * @param request Entry/exit request with QR code, timestamp, and GPS coordinates
     * @return EntryExitRecordDto containing recorded event data
     * Requirements: 8.1 - Extract JWT token from request headers
     *               8.2 - Validate QR code against current date and personnel ID
     *               8.5 - Store entry/exit record with all required fields
     */
    @PostMapping("/giris-cikis-kaydet")
    public ResponseEntity<?> recordEntryExit(@Valid @RequestBody EntryExitRequestDto request) {
        Long userId = getAuthenticatedUserId();
        
        // Check rate limit
        if (!checkRateLimit(userId)) {
            return ResponseEntity.status(429)
                .body(Map.of("message", "Too many requests. Please try again later."));
        }
        
        // Validate QR code
        QrCodeValidationDto validation = qrCodeService.validateQrCode(
            request.qrCodeValue(), 
            userId
        );
        
        if (!validation.valid()) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", validation.message()));
        }
        
        // Validate GPS coordinates
        if (!isValidGpsCoordinate(request.latitude(), request.longitude())) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Invalid GPS coordinates"));
        }
        
        // Record entry/exit
        EntryExitRecordDto record = entryExitService.recordEntryExit(
            userId,
            request.qrCodeValue(),
            request.timestamp(),
            request.latitude(),
            request.longitude()
        );
        
        return ResponseEntity.ok(record);
    }
    
    /**
     * Validates GPS coordinates are within valid ranges.
     * 
     * @param latitude Latitude value
     * @param longitude Longitude value
     * @return true if coordinates are valid
     * Requirements: 12.2 - Validate latitude range [-90, 90]
     *               12.3 - Validate longitude range [-180, 180]
     */
    private boolean isValidGpsCoordinate(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }
        return latitude >= -90.0 && latitude <= 90.0 
            && longitude >= -180.0 && longitude <= 180.0;
    }
    
    /**
     * Checks if the user has exceeded the rate limit.
     * 
     * @param userId User ID
     * @return true if request is allowed, false if rate limit exceeded
     */
    private boolean checkRateLimit(Long userId) {
        long currentTime = System.currentTimeMillis();
        
        rateLimitMap.compute(userId, (id, info) -> {
            if (info == null || currentTime - info.windowStart > RATE_LIMIT_WINDOW_MS) {
                // New window
                return new RateLimitInfo(currentTime, 1);
            } else {
                // Within current window
                info.count.incrementAndGet();
                return info;
            }
        });
        
        RateLimitInfo info = rateLimitMap.get(userId);
        return info.count.get() <= MAX_REQUESTS_PER_MINUTE;
    }
    
    /**
     * Extracts authenticated user ID from security context.
     * 
     * @return User ID from JWT token
     */
    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        // If principal is stored as a different type, extract accordingly
        Object principal = authentication.getPrincipal();
        if (principal instanceof String) {
            return Long.parseLong((String) principal);
        }
        throw new IllegalStateException("Unable to extract user ID from authentication");
    }
    
    /**
     * Inner class to track rate limit information per user.
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
