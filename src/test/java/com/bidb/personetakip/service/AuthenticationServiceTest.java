package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.AuthTokenDto;
import com.bidb.personetakip.dto.UserDto;
import com.bidb.personetakip.exception.ValidationException;
import com.bidb.personetakip.model.User;
import com.bidb.personetakip.model.UserRole;
import com.bidb.personetakip.repository.UserRepository;
import com.bidb.personetakip.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthenticationService
 * Requirements: 4.1, 4.2, 4.3, 4.5
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtUtil jwtUtil;
    
    @InjectMocks
    private AuthenticationServiceImpl authenticationService;
    
    private User testUser;
    private String testTcNo = "12345678901";
    private String testPassword = "Password123!";
    private String testPasswordHash = "$2a$12$hashedpassword";
    private String testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .tcNo(testTcNo)
            .personnelNo("P12345")
            .firstName("Ahmet")
            .lastName("Yılmaz")
            .mobilePhone("05551234567")
            .passwordHash(testPasswordHash)
            .role(UserRole.NORMAL_USER)
            .build();
    }
    
    /**
     * Test successful login with valid credentials
     * Requirements: 4.1 - Query user by tcNo
     *               4.2 - Verify password using BCrypt
     *               4.3 - Generate JWT token with userId, tcNo, and role
     *               4.5 - Return token with 30-minute expiration
     */
    @Test
    void login_WithValidCredentials_ShouldReturnAuthToken() {
        // Arrange
        when(userRepository.findByTcNo(testTcNo)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(testPassword, testPasswordHash)).thenReturn(true);
        when(jwtUtil.generateToken(testUser)).thenReturn(testToken);
        when(jwtUtil.getExpirationTime()).thenReturn(1800000L); // 30 minutes in milliseconds
        
        // Act
        AuthTokenDto result = authenticationService.login(testTcNo, testPassword);
        
        // Assert
        assertNotNull(result);
        assertEquals(testToken, result.token());
        assertEquals("Bearer", result.tokenType());
        assertEquals(1800000L, result.expiresIn());
        
        assertNotNull(result.user());
        assertEquals(testUser.getId(), result.user().id());
        assertEquals(testUser.getTcNo(), result.user().tcNo());
        assertEquals(testUser.getPersonnelNo(), result.user().personnelNo());
        assertEquals(testUser.getFirstName(), result.user().firstName());
        assertEquals(testUser.getLastName(), result.user().lastName());
        assertEquals(testUser.getRole().name(), result.user().role());
        
        verify(userRepository).findByTcNo(testTcNo);
        verify(passwordEncoder).matches(testPassword, testPasswordHash);
        verify(jwtUtil).generateToken(testUser);
        verify(jwtUtil).getExpirationTime();
    }
    
    /**
     * Test login failure with invalid TC number (user not found)
     * Requirements: 4.1 - Query user by tcNo
     *               4.2 - Reject invalid credentials
     */
    @Test
    void login_WithInvalidTcNo_ShouldThrowValidationException() {
        // Arrange
        String invalidTcNo = "99999999999";
        when(userRepository.findByTcNo(invalidTcNo)).thenReturn(Optional.empty());
        
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authenticationService.login(invalidTcNo, testPassword);
        });
        
        assertEquals("Invalid TC number or password", exception.getMessage());
        verify(userRepository).findByTcNo(invalidTcNo);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(any());
    }
    
    /**
     * Test login failure with invalid password
     * Requirements: 4.2 - Verify password using BCrypt
     *               4.4 - Reject invalid credentials
     */
    @Test
    void login_WithInvalidPassword_ShouldThrowValidationException() {
        // Arrange
        String wrongPassword = "WrongPassword123!";
        when(userRepository.findByTcNo(testTcNo)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(wrongPassword, testPasswordHash)).thenReturn(false);
        
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authenticationService.login(testTcNo, wrongPassword);
        });
        
        assertEquals("Invalid TC number or password", exception.getMessage());
        verify(userRepository).findByTcNo(testTcNo);
        verify(passwordEncoder).matches(wrongPassword, testPasswordHash);
        verify(jwtUtil, never()).generateToken(any());
    }
    
    /**
     * Test JWT token generation includes all required fields
     * Requirements: 4.3 - Generate JWT token with userId, tcNo, and role
     */
    @Test
    void login_ShouldGenerateTokenWithUserDetails() {
        // Arrange
        when(userRepository.findByTcNo(testTcNo)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(testPassword, testPasswordHash)).thenReturn(true);
        when(jwtUtil.generateToken(testUser)).thenReturn(testToken);
        when(jwtUtil.getExpirationTime()).thenReturn(1800000L);
        
        // Act
        AuthTokenDto result = authenticationService.login(testTcNo, testPassword);
        
        // Assert
        verify(jwtUtil).generateToken(testUser);
        assertNotNull(result.token());
        assertNotNull(result.user());
    }
    
    /**
     * Test token validation with valid token
     * Requirements: 4.3 - Extract user details from token
     *               4.5 - Verify token expiration
     */
    @Test
    void validateToken_WithValidToken_ShouldReturnUserDto() {
        // Arrange
        when(jwtUtil.validateToken(testToken)).thenReturn(true);
        when(jwtUtil.extractTcNo(testToken)).thenReturn(testTcNo);
        when(jwtUtil.extractUserId(testToken)).thenReturn(1L);
        when(jwtUtil.extractRole(testToken)).thenReturn("NORMAL_USER");
        when(userRepository.findByTcNo(testTcNo)).thenReturn(Optional.of(testUser));
        
        // Act
        UserDto result = authenticationService.validateToken(testToken);
        
        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.id());
        assertEquals(testUser.getTcNo(), result.tcNo());
        assertEquals(testUser.getPersonnelNo(), result.personnelNo());
        assertEquals(testUser.getFirstName(), result.firstName());
        assertEquals(testUser.getLastName(), result.lastName());
        assertEquals(testUser.getRole().name(), result.role());
        
        verify(jwtUtil).validateToken(testToken);
        verify(jwtUtil).extractTcNo(testToken);
        verify(jwtUtil).extractUserId(testToken);
        verify(jwtUtil).extractRole(testToken);
        verify(userRepository).findByTcNo(testTcNo);
    }
    
    /**
     * Test token validation with invalid token
     * Requirements: 4.5 - Verify token expiration and signature
     */
    @Test
    void validateToken_WithInvalidToken_ShouldThrowValidationException() {
        // Arrange
        String invalidToken = "invalid.token.here";
        when(jwtUtil.validateToken(invalidToken)).thenReturn(false);
        
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authenticationService.validateToken(invalidToken);
        });
        
        assertEquals("Invalid or expired token", exception.getMessage());
        verify(jwtUtil).validateToken(invalidToken);
        verify(jwtUtil, never()).extractTcNo(anyString());
        verify(userRepository, never()).findByTcNo(anyString());
    }
    
    /**
     * Test token validation when user no longer exists
     * Requirements: 4.3 - Extract user details from token
     */
    @Test
    void validateToken_WithNonExistentUser_ShouldThrowValidationException() {
        // Arrange
        when(jwtUtil.validateToken(testToken)).thenReturn(true);
        when(jwtUtil.extractTcNo(testToken)).thenReturn(testTcNo);
        when(jwtUtil.extractUserId(testToken)).thenReturn(1L);
        when(jwtUtil.extractRole(testToken)).thenReturn("NORMAL_USER");
        when(userRepository.findByTcNo(testTcNo)).thenReturn(Optional.empty());
        
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authenticationService.validateToken(testToken);
        });
        
        assertEquals("User not found", exception.getMessage());
        verify(jwtUtil).validateToken(testToken);
        verify(userRepository).findByTcNo(testTcNo);
    }
    
    /**
     * Test login with different user roles
     * Requirements: 4.3 - Include role in JWT token
     */
    @Test
    void login_WithAdminUser_ShouldIncludeAdminRole() {
        // Arrange
        User adminUser = User.builder()
            .id(2L)
            .tcNo("98765432109")
            .personnelNo("A12345")
            .firstName("Admin")
            .lastName("User")
            .mobilePhone("05559876543")
            .passwordHash(testPasswordHash)
            .role(UserRole.ADMIN)
            .build();
        
        when(userRepository.findByTcNo(adminUser.getTcNo())).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches(testPassword, testPasswordHash)).thenReturn(true);
        when(jwtUtil.generateToken(adminUser)).thenReturn(testToken);
        when(jwtUtil.getExpirationTime()).thenReturn(1800000L);
        
        // Act
        AuthTokenDto result = authenticationService.login(adminUser.getTcNo(), testPassword);
        
        // Assert
        assertNotNull(result);
        assertEquals("ADMIN", result.user().role());
        verify(jwtUtil).generateToken(adminUser);
    }
    
    /**
     * Test that token expiration time is 30 minutes
     * Requirements: 4.5 - Return token with 30-minute expiration
     */
    @Test
    void login_ShouldReturnTokenWith30MinuteExpiration() {
        // Arrange
        Long expectedExpiration = 1800000L; // 30 minutes in milliseconds
        when(userRepository.findByTcNo(testTcNo)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(testPassword, testPasswordHash)).thenReturn(true);
        when(jwtUtil.generateToken(testUser)).thenReturn(testToken);
        when(jwtUtil.getExpirationTime()).thenReturn(expectedExpiration);
        
        // Act
        AuthTokenDto result = authenticationService.login(testTcNo, testPassword);
        
        // Assert
        assertEquals(expectedExpiration, result.expiresIn());
        verify(jwtUtil).getExpirationTime();
    }
}
