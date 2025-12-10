package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.AuthTokenDto;
import com.bidb.personetakip.dto.UserDto;
import com.bidb.personetakip.exception.AuthenticationException;
import com.bidb.personetakip.exception.ValidationException;
import com.bidb.personetakip.model.User;
import com.bidb.personetakip.repository.UserRepository;
import com.bidb.personetakip.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of AuthenticationService.
 * Handles user authentication, password verification, and JWT token generation.
 * Requirements: 4.1, 4.2, 4.3, 4.5
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    /**
     * Authenticates a user with TC number and password.
     * 
     * @param tcNo Turkish Citizen ID number
     * @param password User password
     * @return AuthTokenDto containing JWT token and user details
     * @throws ValidationException if credentials are invalid
     * Requirements: 4.1 - Query user by tcNo from local database
     *               4.2 - Verify password using BCrypt
     *               4.3 - Generate JWT token with userId, tcNo, and role
     *               4.5 - Return token with 30-minute expiration
     */
    @Override
    @Transactional(readOnly = true)
    public AuthTokenDto login(String tcNo, String password) {
        log.info("Login attempt for TC No: {}", tcNo);
        
        // Requirement 4.1: Query user by tcNo from local database
        User user = userRepository.findByTcNo(tcNo)
                .orElseThrow(() -> {
                    log.warn("Login failed: User not found for TC No: {}", tcNo);
                    return new AuthenticationException("Invalid TC number or password");
                });
        
        // Requirement 4.2: Verify password using BCrypt
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Login failed: Invalid password for TC No: {}", tcNo);
            throw new AuthenticationException("Invalid TC number or password");
        }
        
        // Requirement 4.3: Generate JWT token with userId, tcNo, and role
        String token = jwtUtil.generateToken(user);
        
        // Requirement 4.5: Return token with 30-minute expiration time
        Long expiresIn = jwtUtil.getExpirationTime() / 1000; // convert ms to seconds for API contract
        
        UserDto userDto = new UserDto(
                user.getId(),
                user.getTcNo(),
                user.getPersonnelNo(),
                user.getFirstName(),
                user.getLastName(),
                user.getMobilePhone(),
                user.getDepartmentCode(),
                user.getTitleCode(),
                user.getRole()
        );
        
        log.info("Login successful for TC No: {}, User ID: {}", tcNo, user.getId());
        
        return new AuthTokenDto(token, "Bearer", expiresIn, userDto);
    }
    
    /**
     * Validates a JWT token and returns user details.
     * 
     * @param token JWT token to validate
     * @return UserDto containing user details from the token
     * @throws ValidationException if token is invalid
     * Requirements: 4.3 - Extract user details from token
     *               4.5 - Verify token expiration
     */
    @Override
    public UserDto validateToken(String token) {
        log.debug("Validating JWT token");
        
        // Requirement 4.5: Verify token expiration and signature
        if (!jwtUtil.validateToken(token)) {
            log.warn("Token validation failed: Invalid or expired token");
            throw new ValidationException("Invalid or expired token");
        }
        
        // Requirement 4.3: Extract user details from token
        String tcNo = jwtUtil.extractTcNo(token);
        Long userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);
        
        // Fetch user from database to get complete details
        User user = userRepository.findByTcNo(tcNo)
                .orElseThrow(() -> {
                    log.warn("Token validation failed: User not found for TC No: {}", tcNo);
                    return new ValidationException("User not found");
                });
        
        log.debug("Token validated successfully for TC No: {}", tcNo);
        
        return new UserDto(
                user.getId(),
                user.getTcNo(),
                user.getPersonnelNo(),
                user.getFirstName(),
                user.getLastName(),
                user.getMobilePhone(),
                user.getDepartmentCode(),
                user.getTitleCode(),
                user.getRole()
        );
    }
}
