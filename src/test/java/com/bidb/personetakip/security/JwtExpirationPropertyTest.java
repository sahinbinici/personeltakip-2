package com.bidb.personetakip.security;

import com.bidb.personetakip.model.User;
import com.bidb.personetakip.model.UserRole;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Property-based tests for JWT expiration time consistency.
 * 
 * **Feature: personnel-tracking-system, Property 8: JWT expiration time consistency**
 * **Validates: Requirements 4.5**
 * 
 * Property: For any generated JWT token, the expiration time should be exactly 30 minutes 
 * from the issued-at time.
 */
@RunWith(JUnitQuickcheck.class)
public class JwtExpirationPropertyTest {
    
    private JwtUtil jwtUtil;
    private static final Long EXPECTED_EXPIRATION_MS = 1800000L; // 30 minutes
    
    @Before
    public void setUp() {
        jwtUtil = new JwtUtil();
        // Set test configuration values
        ReflectionTestUtils.setField(jwtUtil, "secret", 
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPECTED_EXPIRATION_MS);
        ReflectionTestUtils.setField(jwtUtil, "issuer", "personnel-tracking-system");
    }
    
    /**
     * Property: For any user, the generated JWT token should have an expiration time
     * that is exactly 30 minutes (1800000 milliseconds) from the issued-at time.
     */
    @Property(trials = 100)
    public void jwtExpirationShouldBeExactly30MinutesFromIssuedAt(
            @From(UserGenerator.class) User user) {
        
        // Generate token for the user
        String token = jwtUtil.generateToken(user);
        
        // Extract issued-at and expiration times
        Date issuedAt = jwtUtil.extractIssuedAt(token);
        Date expiration = jwtUtil.extractExpiration(token);
        
        // Calculate the difference in milliseconds
        long actualExpirationDuration = expiration.getTime() - issuedAt.getTime();
        
        // Assert that the expiration is exactly 30 minutes from issued-at
        assertEquals("JWT expiration time should be exactly 30 minutes (1800000ms) from issued-at time",
            EXPECTED_EXPIRATION_MS.longValue(), actualExpirationDuration);
    }
    
    /**
     * Property: For any user, multiple tokens generated at different times should
     * all have the same expiration duration of 30 minutes.
     */
    @Property(trials = 100)
    public void allTokensShouldHaveConsistentExpirationDuration(
            @From(UserGenerator.class) User user) throws InterruptedException {
        
        // Generate first token
        String token1 = jwtUtil.generateToken(user);
        Date issuedAt1 = jwtUtil.extractIssuedAt(token1);
        Date expiration1 = jwtUtil.extractExpiration(token1);
        long duration1 = expiration1.getTime() - issuedAt1.getTime();
        
        // Small delay to ensure different issued-at times
        Thread.sleep(10);
        
        // Generate second token
        String token2 = jwtUtil.generateToken(user);
        Date issuedAt2 = jwtUtil.extractIssuedAt(token2);
        Date expiration2 = jwtUtil.extractExpiration(token2);
        long duration2 = expiration2.getTime() - issuedAt2.getTime();
        
        // Both tokens should have the same expiration duration
        assertEquals("All tokens should have consistent expiration duration of 30 minutes",
            duration1, duration2);
        assertEquals("Expiration duration should be exactly 30 minutes",
            EXPECTED_EXPIRATION_MS.longValue(), duration1);
    }
}
