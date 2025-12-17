package com.bidb.personetakip.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for authentication token response.
 * Contains JWT token, token type, expiration time, and user details.
 * Requirements: 4.3
 */
@Schema(description = "Authentication token response containing JWT and user information")
public record AuthTokenDto(
    @Schema(
        description = "JWT access token",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
    )
    String token,
    
    @Schema(
        description = "Token type",
        example = "Bearer",
        defaultValue = "Bearer"
    )
    String tokenType,
    
    @Schema(
        description = "Token expiration time in seconds",
        example = "3600"
    )
    Long expiresIn,
    
    @Schema(description = "Authenticated user information")
    UserDto user
) {}
