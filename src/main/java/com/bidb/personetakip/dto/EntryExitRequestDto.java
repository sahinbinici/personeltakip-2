package com.bidb.personetakip.dto;

import java.time.LocalDateTime;

public record EntryExitRequestDto(
    String qrCodeValue,
    LocalDateTime timestamp,
    Double latitude,
    Double longitude
) {}
