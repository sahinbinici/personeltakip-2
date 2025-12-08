package com.bidb.personetakip.model;

/**
 * Enum representing the type of entry/exit event.
 */
public enum EntryExitType {
    /**
     * Entry event - first usage of daily QR code
     */
    ENTRY,
    
    /**
     * Exit event - second usage of daily QR code
     */
    EXIT
}
