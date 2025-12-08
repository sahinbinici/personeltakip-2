package com.bidb.personetakip.dto;

import com.bidb.personetakip.model.EntryExitType;

public record QrCodeValidationDto(
    boolean valid,
    String message,
    EntryExitType nextType
) {}
