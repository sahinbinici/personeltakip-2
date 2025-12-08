package com.bidb.personetakip.controller;

import com.bidb.personetakip.dto.QrCodeDto;
import com.bidb.personetakip.service.QrCodeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for QR code operations.
 * Requires JWT authentication.
 * Requirements: 5.1, 5.5
 */
@RestController
@RequestMapping("/api/qrcode")
public class QrCodeController {
    
    private final QrCodeService qrCodeService;
    
    public QrCodeController(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }
    
    /**
     * Gets daily QR code for authenticated user.
     * GET /api/qrcode/daily
     * 
     * @return QrCodeDto containing QR code data
     * Requirements: 5.1 - Generate unique QR code for current date
     */
    @GetMapping("/daily")
    public ResponseEntity<QrCodeDto> getDailyQrCode() {
        Long userId = getAuthenticatedUserId();
        QrCodeDto qrCode = qrCodeService.getDailyQrCode(userId);
        return ResponseEntity.ok(qrCode);
    }
    
    /**
     * Gets QR code image as PNG for authenticated user.
     * GET /api/qrcode/image
     * 
     * @param qrCodeValue QR code value to generate image for
     * @return PNG image bytes
     * Requirements: 5.5 - Render QR code as scannable image
     */
    @GetMapping("/image")
    public ResponseEntity<byte[]> getQrCodeImage(@RequestParam String qrCodeValue) {
        byte[] imageBytes = qrCodeService.generateQrCodeImage(qrCodeValue);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentLength(imageBytes.length);
        
        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
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
        // This assumes JwtAuthenticationFilter sets userId as principal
        Object principal = authentication.getPrincipal();
        if (principal instanceof String) {
            return Long.parseLong((String) principal);
        }
        throw new IllegalStateException("Unable to extract user ID from authentication");
    }
}
