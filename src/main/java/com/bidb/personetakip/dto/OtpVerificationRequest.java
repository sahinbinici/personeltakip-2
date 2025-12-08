package com.bidb.personetakip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for OTP verification request
 */
public record OtpVerificationRequest(
    @NotBlank(message = "TC No is required")
    @Pattern(regexp = "^[0-9]{11}$", message = "TC No must be 11 digits")
    String tcNo,
    
    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP code must be 6 digits")
    String otpCode
) {}
