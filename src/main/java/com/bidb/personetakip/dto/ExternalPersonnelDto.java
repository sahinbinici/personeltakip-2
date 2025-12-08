package com.bidb.personetakip.dto;

/**
 * DTO for external personnel data retrieved from external database
 */
public record ExternalPersonnelDto(
    Long userId,
    String tcNo,
    String personnelNo,
    String firstName,
    String lastName,
    String mobilePhone
) {}
