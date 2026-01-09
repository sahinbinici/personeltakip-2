package com.bidb.personetakip.dto;

import com.bidb.personetakip.model.EntryExitType;
import java.time.LocalDateTime;

/**
 * DTO for user's current entry/exit status
 */
public record UserStatusDto(
    boolean isInside,
    EntryExitType lastActionType,
    LocalDateTime lastActionTime,
    String message
) {
    
    /**
     * Creates a status for a user who is currently inside (last action was ENTRY)
     */
    public static UserStatusDto inside(LocalDateTime lastEntryTime) {
        return new UserStatusDto(
            true,
            EntryExitType.ENTRY,
            lastEntryTime,
            "İçerisiniz"
        );
    }
    
    /**
     * Creates a status for a user who is currently outside (last action was EXIT)
     */
    public static UserStatusDto outside(LocalDateTime lastExitTime) {
        return new UserStatusDto(
            false,
            EntryExitType.EXIT,
            lastExitTime,
            "Dışarıdasınız"
        );
    }
    
    /**
     * Creates a status for a user with no records (assumed to be outside)
     */
    public static UserStatusDto noRecords() {
        return new UserStatusDto(
            false,
            null,
            null,
            "Henüz giriş/çıkış kaydı yok"
        );
    }
}