package com.bidb.personetakip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for login request containing TC number and password.
 * Requirements: 4.3
 */
public record LoginRequest(
    @NotBlank(message = "TC number is required")
    @Pattern(regexp = "\\d{11}", message = "TC number must be exactly 11 digits")
    String tcNo,
    
    @NotBlank(message = "Password is required")
    String password
) {}
