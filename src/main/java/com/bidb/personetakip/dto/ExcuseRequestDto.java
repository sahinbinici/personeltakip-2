package com.bidb.personetakip.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for excuse submission requests from mobile application.
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5
 */
@Schema(description = "Request DTO for submitting excuse reports")
public record ExcuseRequestDto(
    @Schema(description = "Excuse type information", required = true)
    @NotNull(message = "Excuse type is required")
    ExcuseTypeDto excuseType,
    
    @Schema(description = "Detailed description of the excuse", example = "Hastalık nedeniyle işe gelemiyorum")
    @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters")
    String description,
    
    @Schema(description = "Date for which the excuse is being reported", example = "2024-12-16", required = true)
    @NotNull(message = "Date is required")
    LocalDate date,
    
    @Schema(description = "List of attachment file paths or URLs")
    List<String> attachments
) {
    /**
     * Validates the excuse request based on excuse type requirements.
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        // Check if description is required and provided
        if (excuseType.requiresDescription() && (description == null || description.trim().isEmpty())) {
            return false;
        }
        
        // Check if attachment is required and provided
        if (excuseType.requiresAttachment() && (attachments == null || attachments.isEmpty())) {
            return false;
        }
        
        // Check if date is not in the future
        if (date.isAfter(LocalDate.now())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets validation error message if request is invalid.
     * 
     * @return error message or null if valid
     */
    public String getValidationError() {
        if (excuseType == null) {
            return "Excuse type is required";
        }
        
        if (date == null) {
            return "Date is required";
        }
        
        if (date.isAfter(LocalDate.now())) {
            return "Excuse date cannot be in the future";
        }
        
        if (excuseType.requiresDescription() && (description == null || description.trim().isEmpty())) {
            return "Description is required for this excuse type";
        }
        
        if (excuseType.requiresDescription() && description.trim().length() < 10) {
            return "Description must be at least 10 characters long";
        }
        
        if (excuseType.requiresDescription() && description.trim().length() > 500) {
            return "Description cannot exceed 500 characters";
        }
        
        if (excuseType.requiresAttachment() && (attachments == null || attachments.isEmpty())) {
            return "Attachment is required for this excuse type";
        }
        
        return null;
    }
}