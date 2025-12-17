package com.bidb.personetakip.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for excuse type information.
 * Requirements: 3.1, 3.2
 */
@Schema(description = "Excuse type information")
public record ExcuseTypeDto(
    @Schema(description = "Unique identifier for the excuse type", example = "1", required = true)
    @NotNull(message = "Excuse type ID is required")
    Long id,
    
    @Schema(description = "Name of the excuse type", example = "Hastalık", required = true)
    @NotBlank(message = "Excuse type name is required")
    String name,
    
    @Schema(description = "Whether this excuse type requires a description", example = "true")
    boolean requiresDescription,
    
    @Schema(description = "Whether this excuse type requires an attachment", example = "false")
    boolean requiresAttachment
) {}