package com.bidb.personetakip.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO for excuse submission response.
 * Requirements: 3.4, 3.5
 */
@Schema(description = "Response DTO for excuse submission")
public record ExcuseResponseDto(
    @Schema(description = "Unique identifier for the submitted excuse", example = "123")
    Long id,
    
    @Schema(description = "Success message", example = "Excuse submitted successfully")
    String message,
    
    @Schema(description = "Current status of the excuse", example = "PENDING")
    String status,
    
    @Schema(description = "Timestamp when the excuse was submitted", example = "2024-12-16T10:30:00")
    LocalDateTime submittedAt,
    
    @Schema(description = "User ID who submitted the excuse", example = "456")
    Long userId
) {
    /**
     * Creates a successful response for excuse submission.
     * 
     * @param id Excuse ID
     * @param userId User ID
     * @return ExcuseResponseDto
     */
    public static ExcuseResponseDto success(Long id, Long userId) {
        return new ExcuseResponseDto(
            id,
            "Mazeret bildiriminiz başarıyla gönderildi. Onay sürecinde size bilgi verilecektir.",
            "PENDING",
            LocalDateTime.now(),
            userId
        );
    }
    
    /**
     * Creates an error response for excuse submission.
     * 
     * @param message Error message
     * @return ExcuseResponseDto
     */
    public static ExcuseResponseDto error(String message) {
        return new ExcuseResponseDto(
            null,
            message,
            "ERROR",
            LocalDateTime.now(),
            null
        );
    }
}