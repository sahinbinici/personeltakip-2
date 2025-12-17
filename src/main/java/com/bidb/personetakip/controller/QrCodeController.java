package com.bidb.personetakip.controller;

import com.bidb.personetakip.dto.QrCodeDto;
import com.bidb.personetakip.service.QrCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "QR Code", description = "QR code generation and management endpoints")
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
    @Operation(
        summary = "Get daily QR code",
        description = "Generates and returns the daily QR code for the authenticated user",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "QR code generated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = QrCodeDto.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                    {
                        "qrCodeValue": "USER_123_2024-12-16_abc123def456",
                        "userId": 123,
                        "generatedDate": "2024-12-16",
                        "expiresAt": "2024-12-16T23:59:59Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Unauthorized",
                    value = """
                    {
                        "error": "Unauthorized",
                        "message": "Authentication required"
                    }
                    """
                )
            )
        )
    })
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
    @Operation(
        summary = "Get QR code image",
        description = "Generates and returns QR code as a PNG image for scanning",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "QR code image generated successfully",
            content = @Content(
                mediaType = "image/png",
                schema = @Schema(type = "string", format = "binary")
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid QR code value",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Invalid QR Code",
                    value = """
                    {
                        "message": "Invalid QR code value"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Unauthorized",
                    value = """
                    {
                        "error": "Unauthorized",
                        "message": "Authentication required"
                    }
                    """
                )
            )
        )
    })
    @GetMapping("/image")
    public ResponseEntity<byte[]> getQrCodeImage(
        @Parameter(description = "QR code value to generate image for", required = true, example = "USER_123_2024-12-16_abc123def456")
        @RequestParam String qrCodeValue) {
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
