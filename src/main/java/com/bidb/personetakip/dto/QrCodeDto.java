package com.bidb.personetakip.dto;

import java.time.LocalDate;

public record QrCodeDto(
    String qrCodeValue,
    LocalDate validDate,
    int usageCount,
    int maxUsage
) {}
