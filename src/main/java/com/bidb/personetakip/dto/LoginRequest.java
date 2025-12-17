package com.bidb.personetakip.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for login request containing TC number and password.
 * Requirements: 4.3
 */
@Schema(description = "Login request containing user credentials")
public record LoginRequest(
    @Schema(
        description = "Turkish Citizenship Number (TC No)",
        example = "12345678901",
        pattern = "\\d{11}",
        minLength = 11,
        maxLength = 11
    )
    @NotBlank(message = "TC number is required")
    @Pattern(regexp = "\\d{11}", message = "TC number must be exactly 11 digits")
    String tcNo,
    
    @Schema(
        description = "User password",
        example = "mySecurePassword123",
        format = "password"
    )
    @NotBlank(message = "Password is required")
    String password
) {}
