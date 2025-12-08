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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for authentication token generation.
 * 
 * **Feature: personnel-tracking-system, Property 7: Authentication token generation**
 * **Validates: Requirements 4.3, 7.2, 11.3**
 * 
 * Property: For any successful login with valid TC ID and password, the system should 
 * generate a JWT token containing the user's TC ID, user ID, and role in the payload.
 */
@RunWith(JUnitQuickcheck.class)
public class AuthenticationTokenPropertyTest {
    
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
     * Property: For any valid user with valid credentials, the generated JWT token
     * should contain the user's TC ID, user ID, and role in the payload.
     */
    @Property(trials = 100)
    public void authenticationTokenShouldContainUserIdTcNoAndRole(
            @From(UserGenerator.class) User user) {
        
        // Set up test password
        String plainPassword = "TestPass123!";
        String hashedPassword = passwordEncoder.encode(plainPassword);
        user.setPasswordHash(hashedPassword);
        
        // Mock repository to return the user
        when(userRepository.findByTcNo(user.getTcNo())).thenReturn(Optional.of(user));
        
        // Perform login
        AuthTokenDto authToken = authenticationService.login(user.getTcNo(), plainPassword);
        
        // Verify token is not null
        assertNotNull("Token should not be null", authToken.token());
        
        // Extract claims from the token
        String extractedTcNo = jwtUtil.extractTcNo(authToken.token());
        Long extractedUserId = jwtUtil.extractUserId(authToken.token());
        String extractedRole = jwtUtil.extractRole(authToken.token());
        
        // Requirement 4.3: Token should contain userId, tcNo, and role
        assertEquals("Token should contain the user's TC No", 
            user.getTcNo(), extractedTcNo);
        assertEquals("Token should contain the user's ID", 
            user.getId(), extractedUserId);
        assertEquals("Token should contain the user's role", 
            user.getRole().name(), extractedRole);
    }
    
    /**
     * Property: For any valid user, the authentication response should include
     * the token, token type, expiration time, and user details.
     */
    @Property(trials = 100)
    public void authenticationResponseShouldContainAllRequiredFields(
            @From(UserGenerator.class) User user) {
        
        // Set up test password
        String plainPassword = "ValidPass123!";
        String hashedPassword = passwordEncoder.encode(plainPassword);
        user.setPasswordHash(hashedPassword);
        
        // Mock repository to return the user
        when(userRepository.findByTcNo(user.getTcNo())).thenReturn(Optional.of(user));
        
        // Perform login
        AuthTokenDto authToken = authenticationService.login(user.getTcNo(), plainPassword);
        
        // Requirement 7.2: Response should contain token and user details
        assertNotNull("Token should not be null", authToken.token());
        assertNotNull("Token type should not be null", authToken.tokenType());
        assertEquals("Token type should be Bearer", "Bearer", authToken.tokenType());
        assertNotNull("Expiration time should not be null", authToken.expiresIn());
        assertEquals("Expiration time should be 30 minutes (1800000ms)", 
            Long.valueOf(1800000L), authToken.expiresIn());
        
        // Requirement 11.3: User details should be included
        assertNotNull("User details should not be null", authToken.user());
        assertEquals("User TC No should match", user.getTcNo(), authToken.user().tcNo());
        assertEquals("User ID should match", user.getId(), authToken.user().id());
        assertEquals("User role should match", user.getRole().name(), authToken.user().role());
        assertEquals("User first name should match", user.getFirstName(), authToken.user().firstName());
        assertEquals("User last name should match", user.getLastName(), authToken.user().lastName());
    }
    
    /**
     * Property: For any user, the generated token should be valid and verifiable.
     */
    @Property(trials = 100)
    public void generatedTokenShouldBeValidAndVerifiable(
            @From(UserGenerator.class) User user) {
        
        // Set up test password
        String plainPassword = "SecurePass456!";
        String hashedPassword = passwordEncoder.encode(plainPassword);
        user.setPasswordHash(hashedPassword);
        
        // Mock repository to return the user
        when(userRepository.findByTcNo(user.getTcNo())).thenReturn(Optional.of(user));
        
        // Perform login
        AuthTokenDto authToken = authenticationService.login(user.getTcNo(), plainPassword);
        
        // Token should be valid
        assertTrue("Generated token should be valid", 
            jwtUtil.validateToken(authToken.token()));
        assertTrue("Generated token should be valid for the user's TC No", 
            jwtUtil.validateToken(authToken.token(), user.getTcNo()));
    }
}
