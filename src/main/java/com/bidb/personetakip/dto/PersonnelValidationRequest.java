package com.bidb.personetakip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for personnel validation request during registration
 */
public record PersonnelValidationRequest(
    @NotBlank(message = "TC No is required")
    @Pattern(regexp = "^[0-9]{11}$", message = "TC No must be 11 digits")
    String tcNo,
    
    @NotBlank(message = "Personnel No is required")
    String personnelNo
) {}
