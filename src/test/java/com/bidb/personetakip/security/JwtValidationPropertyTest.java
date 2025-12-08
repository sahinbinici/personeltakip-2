package com.bidb.personetakip.security;

import com.bidb.personetakip.model.User;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * Property-based tests for JWT validation strictness.
 * 
 * **Feature: personnel-tracking-system, Property 9: JWT validation strictness**
 * **Validates: Requirements 9.4**
 * 
 * Property: For any JWT token with invalid signature or expired timestamp, 
 * validation should fail and reject the request.
 */
@RunWith(JUnitQuickcheck.class)
public class JwtValidationPropertyTest {
    
    private JwtUtil jwtUtil;
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final String WRONG_SECRET = "WRONG404E635266556A586E3272357538782F413F4428472B4B6250645367566B";
    private static final Long EXPIRATION_MS = 1800000L; // 30 minutes
    
    @Before
    public void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPIRATION_MS);
        ReflectionTestUtils.setField(jwtUtil, "issuer", "personnel-tracking-system");
    }
    
    /**
     * Property: For any user, a token signed with a different secret key should fail validation.
     */
    @Property(trials = 100)
    public void tokenWithInvalidSignatureShouldFailValidation(
            @From(UserGenerator.class) User user) {
        
        // Create a token with wrong secret
        SecretKey wrongKey = Keys.hmacShaKeyFor(WRONG_SECRET.getBytes(StandardCharsets.UTF_8));
        
        String invalidToken = Jwts.builder()
                .subject(user.getTcNo())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .issuer("personnel-tracking-system")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusMillis(EXPIRATION_MS)))
                .signWith(wrongKey)
                .compact();
        
        // Validation should fail due to invalid signature
        boolean isValid = jwtUtil.validateToken(invalidToken);
        
        assertFalse("Token with invalid signature should fail validation", isValid);
    }
    
    /**
     * Property: For any user, an expired token should fail validation.
     */
    @Property(trials = 100)
    public void expiredTokenShouldFailValidation(
            @From(UserGenerator.class) User user) {
        
        // Create an expired token (expired 1 hour ago)
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        Instant expiredTime = now.minusMillis(3600000); // 1 hour ago
        
        String expiredToken = Jwts.builder()
                .subject(user.getTcNo())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .issuer("personnel-tracking-system")
                .issuedAt(Date.from(expiredTime.minusMillis(EXPIRATION_MS)))
                .expiration(Date.from(expiredTime))
                .signWith(key)
                .compact();
        
        // Validation should fail due to expiration
        boolean isValid = jwtUtil.validateToken(expiredToken);
        
        assertFalse("Expired token should fail validation", isValid);
    }
    
    /**
     * Property: For any user, a valid token (correct signature and not expired) 
     * should pass validation.
     */
    @Property(trials = 100)
    public void validTokenShouldPassValidation(
            @From(UserGenerator.class) User user) {
        
        // Generate a valid token using the JwtUtil
        String validToken = jwtUtil.generateToken(user);
        
        // Validation should succeed
        boolean isValid = jwtUtil.validateToken(validToken, user.getTcNo());
        
        assertTrue("Valid token with correct signature and not expired should pass validation", 
            isValid);
    }
    
    /**
     * Property: For any user, a token with correct signature but wrong TC No 
     * should fail validation when checked against a specific TC No.
     */
    @Property(trials = 100)
    public void tokenWithWrongTcNoShouldFailValidation(
            @From(UserGenerator.class) User user) {
        
        // Generate a valid token
        String token = jwtUtil.generateToken(user);
        
        // Try to validate with a different TC No
        String wrongTcNo = "99999999999";
        if (user.getTcNo().equals(wrongTcNo)) {
            wrongTcNo = "88888888888";
        }
        
        boolean isValid = jwtUtil.validateToken(token, wrongTcNo);
        
        assertFalse("Token validated with wrong TC No should fail", isValid);
    }
    
    /**
     * Property: For any malformed token string, validation should fail gracefully.
     */
    @Property(trials = 100)
    public void malformedTokenShouldFailValidation(String randomString) {
        
        // Skip if the random string happens to be a valid JWT format
        if (randomString == null || randomString.isEmpty() || randomString.split("\\.").length == 3) {
            return;
        }
        
        // Validation should fail for malformed tokens
        boolean isValid = jwtUtil.validateToken(randomString);
        
        assertFalse("Malformed token should fail validation", isValid);
    }
}
