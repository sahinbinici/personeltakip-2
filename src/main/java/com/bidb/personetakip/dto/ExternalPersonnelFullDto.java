package com.bidb.personetakip.dto;

/**
 * DTO for complete external personnel data with all JOIN results
 * Maps to the result of the full SQL query with person, telefo, brkodu, and unvkod tables
 */
public interface ExternalPersonnelFullDto {
    Long getEsicno();
    String getTckiml();
    String getPeradi();
    String getSoyadi();
    String getBrkodu();
    String getBrkdac();
    String getUnvkod();
    String getUnvack();
    String getTelefo();
}
