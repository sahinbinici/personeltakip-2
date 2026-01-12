package com.bidb.personetakip.dto;

import com.bidb.personetakip.model.EntryExitType;
import java.time.LocalDateTime;

public record EntryExitRecordDto(
    Long id,
    Long userId,
    EntryExitType type,
    LocalDateTime timestamp,
    Double latitude,
    Double longitude,
    String excuse
) {
    // Constructor without excuse for backward compatibility
    public EntryExitRecordDto(Long id, Long userId, EntryExitType type, 
                              LocalDateTime timestamp, Double latitude, Double longitude) {
        this(id, userId, type, timestamp, latitude, longitude, null);
    }
}
