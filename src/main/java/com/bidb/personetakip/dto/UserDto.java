package com.bidb.personetakip.dto;

import com.bidb.personetakip.model.UserRole;

/**
 * DTO for user response data
 */
public record UserDto(
    Long id,
    String tcNo,
    String personnelNo,
    String firstName,
    String lastName,
    String mobilePhone,
    String departmentCode,
    String titleCode,
    UserRole role
) {}
