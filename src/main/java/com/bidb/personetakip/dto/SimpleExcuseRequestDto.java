package com.bidb.personetakip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for simple excuse submission from mobile app.
 * Creates an entry/exit record with excuse text instead of QR code.
 */
public record SimpleExcuseRequestDto(
    @NotBlank(message = "Mazeret metni gereklidir")
    @Size(min = 10, max = 500, message = "Mazeret metni 10-500 karakter arasında olmalıdır")
    String excuse,
    
    String type // Optional: "ENTRY" or "EXIT", defaults to ENTRY if not provided
) {}
