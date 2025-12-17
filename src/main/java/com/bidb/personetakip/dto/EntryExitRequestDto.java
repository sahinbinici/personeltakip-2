package com.bidb.personetakip.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Entry/exit recording request with QR code and location data")
public record EntryExitRequestDto(
    @Schema(
        description = "QR code value scanned by the mobile app",
        example = "USER_123_2024-12-16_abc123def456",
        required = true
    )
    String qrCodeValue,
    
    @Schema(
        description = "Timestamp when the QR code was scanned",
        example = "2024-12-16T08:30:00",
        required = true
    )
    LocalDateTime timestamp,
    
    @Schema(
        description = "GPS latitude coordinate",
        example = "41.0082",
        minimum = "-90.0",
        maximum = "90.0",
        required = true
    )
    Double latitude,
    
    @Schema(
        description = "GPS longitude coordinate", 
        example = "28.9784",
        minimum = "-180.0",
        maximum = "180.0",
        required = true
    )
    Double longitude
) {}
