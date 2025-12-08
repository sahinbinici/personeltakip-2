package com.bidb.personetakip.dto;

/**
 * DTO for user response data
 */
public record UserDto(
    Long id,
    String tcNo,
    String personnelNo,
    String firstName,
    String lastName,
    String role
) {}
