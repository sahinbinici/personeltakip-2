package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.AuthTokenDto;
import com.bidb.personetakip.dto.UserDto;

/**
 * Service interface for user authentication operations.
 * Handles login, token generation, and token validation.
 * Requirements: 4.1, 4.2, 4.3, 4.5
 */
public interface AuthenticationService {
    
    /**
     * Authenticates a user with TC number and password.
     * Validates credentials and generates JWT token on success.
     * 
     * @param tcNo Turkish Citizen ID number
     * @param password User password
     * @return AuthTokenDto containing JWT token and user details
     * @throws com.bidb.personetakip.exception.ValidationException if credentials are invalid
     * Requirements: 4.1 - Query user by tcNo
     *               4.2 - Verify password using BCrypt
     *               4.3 - Generate JWT token with userId, tcNo, and role
     *               4.5 - Return token with 30-minute expiration
     */
    AuthTokenDto login(String tcNo, String password);
    
    /**
     * Validates a JWT token and returns user details.
     * 
     * @param token JWT token to validate
     * @return UserDto containing user details from the token
     * @throws com.bidb.personetakip.exception.ValidationException if token is invalid
     * Requirements: 4.3 - Extract user details from token
     *               4.5 - Verify token expiration
     */
    UserDto validateToken(String token);
}
