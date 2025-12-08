package com.bidb.personetakip.dto;

/**
 * DTO for authentication token response.
 * Contains JWT token, token type, expiration time, and user details.
 * Requirements: 4.3
 */
public record AuthTokenDto(
    String token,
    String tokenType,
    Long expiresIn,
    UserDto user
) {}
