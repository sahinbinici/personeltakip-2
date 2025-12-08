package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.AuthTokenDto;
import com.bidb.personetakip.model.User;
import com.bidb.personetakip.model.UserRole;
import com.bidb.personetakip.repository.UserRepository;
import com.bidb.personetakip.security.JwtUtil;
import com.bidb.personetakip.security.UserGenerator;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for role inclusion in JWT tokens.
 * 
 * **Feature: personnel-tracking-system, Property 19: Role inclusion in JWT**
 * **Validates: Requirements 7.4, 11.2**
 * 
 * Property: For any JWT token generated for authentication, the token payload 
 * should include the user's role field.
 */
@RunWith(JUnitQuickcheck.class)
public class RoleInclusionPropertyTest {
    
    private AuthenticationService authenticationService;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    
    @Before
    public void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder(12);
        jwtUtil = new JwtUtil();
        
        // Set test configuration values for JwtUtil
        ReflectionTestUtils.setField(jwtUtil, "secret", 
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 1800000L);
        ReflectionTestUtils.setField(jwtUtil, "issuer", "personnel-tracking-system");
        
        authenticationService = new AuthenticationServiceImpl(userRepository, passwordEncoder, jwtUtil);
    }
    
    /**
     * Property: For any user with any role, the JWT token generated during authentication
     * should include the user's role in the token payload.
     */
    @Property(trials = 100)
    public void jwtTokenShouldAlwaysIncludeUserRole(
            @From(UserGenerator.class) User user) {
        
        // Set up test password
        String plainPassword = "TestPassword123!";
        String hashedPassword = passwordEncoder.encode(plainPassword);
        user.setPasswordHash(hashedPassword);
        
        // Mock repository to return the user
        when(userRepository.findByTcNo(user.getTcNo())).thenReturn(Optional.of(user));
        
        // Perform login to generate token
        AuthTokenDto authToken = authenticationService.login(user.getTcNo(), plainPassword);
        
        // Requirement 7.4, 11.2: Token payload should include user role
        String extractedRole = jwtUtil.extractRole(authToken.token());
        
        assertNotNull("Role should be present in JWT token", extractedRole);
        assertEquals("Role in token should match user's role", 
            user.getRole().name(), extractedRole);
    }
    
    /**
     * Property: For any user, the role extracted from the JWT token should match
     * the role stored in the user entity.
     */
    @Property(trials = 100)
    public void extractedRoleShouldMatchUserRole(
            @From(UserGenerator.class) User user) {
        
        // Set up test password
        String plainPassword = "ValidPassword456!";
        String hashedPassword = passwordEncoder.encode(plainPassword);
        user.setPasswordHash(hashedPassword);
        
        // Mock repository to return the user
        when(userRepository.findByTcNo(user.getTcNo())).thenReturn(Optional.of(user));
        
        // Perform login
        AuthTokenDto authToken = authenticationService.login(user.getTcNo(), plainPassword);
        
        // Extract role from token
        String tokenRole = jwtUtil.extractRole(authToken.token());
        
        // Requirement 11.2: Role field should be stored and included in JWT
        assertEquals("Token role should exactly match user's role", 
            user.getRole().name(), tokenRole);
        
        // Verify role is also in the response DTO
        assertEquals("Response DTO role should match user's role", 
            user.getRole().name(), authToken.user().role());
    }
    
    /**
     * Property: For any user with NORMAL_USER role (the default), the JWT token
     * should correctly include NORMAL_USER as the role.
     */
    @Property(trials = 100)
    public void normalUserRoleShouldBeIncludedInToken(
            @From(UserGenerator.class) User user) {
        
        // Ensure user has NORMAL_USER role (default role)
        user.setRole(UserRole.NORMAL_USER);
        
        // Set up test password
        String plainPassword = "Password789!";
        String hashedPassword = passwordEncoder.encode(plainPassword);
        user.setPasswordHash(hashedPassword);
        
        // Mock repository to return the user
        when(userRepository.findByTcNo(user.getTcNo())).thenReturn(Optional.of(user));
        
        // Perform login
        AuthTokenDto authToken = authenticationService.login(user.getTcNo(), plainPassword);
        
        // Extract role from token
        String tokenRole = jwtUtil.extractRole(authToken.token());
        
        // Requirement 11.2: NORMAL_USER role should be included in token
        assertEquals("Token should include NORMAL_USER role", 
            "NORMAL_USER", tokenRole);
    }
    
    /**
     * Property: For any user, the role should be extractable from the token
     * without requiring database access.
     */
    @Property(trials = 100)
    public void roleShouldBeExtractableFromTokenAlone(
            @From(UserGenerator.class) User user) {
        
        // Set up test password
        String plainPassword = "SecurePass123!";
        String hashedPassword = passwordEncoder.encode(plainPassword);
        user.setPasswordHash(hashedPassword);
        
        // Mock repository to return the user
        when(userRepository.findByTcNo(user.getTcNo())).thenReturn(Optional.of(user));
        
        // Perform login
        AuthTokenDto authToken = authenticationService.login(user.getTcNo(), plainPassword);
        
        // Extract role directly from token using JwtUtil (no database access)
        String extractedRole = jwtUtil.extractRole(authToken.token());
        
        // Requirement 7.4: Role should be in token payload for authorization checks
        assertNotNull("Role should be extractable from token", extractedRole);
        assertEquals("Extracted role should match user's role", 
            user.getRole().name(), extractedRole);
        
        // Verify the role is a valid UserRole enum value
        UserRole roleEnum = UserRole.valueOf(extractedRole);
        assertNotNull("Extracted role should be a valid UserRole enum", roleEnum);
    }
}
