package com.bidb.personetakip.security;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.Assert.*;

/**
 * Property-based tests for password hashing irreversibility.
 * 
 * **Feature: personnel-tracking-system, Property 5: Password hashing irreversibility**
 * **Validates: Requirements 3.4, 9.2**
 * 
 * Property: For any valid password, after BCrypt hashing, the stored hash should never 
 * equal the plaintext password and should be verifiable using BCrypt's verification function.
 */
@RunWith(JUnitQuickcheck.class)
public class PasswordHashingPropertyTest {
    
    private PasswordEncoder passwordEncoder;
    
    @Before
    public void setUp() {
        // Use BCrypt with cost factor 12 as specified in requirements
        passwordEncoder = new BCryptPasswordEncoder(12);
    }
    
    /**
     * Property: For any password, the BCrypt hash should never equal the plaintext password.
     * This ensures that passwords are truly hashed and not stored in plaintext.
     */
    @Property(trials = 50)
    public void hashedPasswordShouldNeverEqualPlaintextPassword(String password) {
        // Skip empty passwords or passwords that exceed BCrypt's 72-byte limit
        if (password == null || password.isEmpty() || password.getBytes().length > 72) {
            return;
        }
        
        // Hash the password
        String hashedPassword = passwordEncoder.encode(password);
        
        // The hash should never equal the plaintext password
        assertNotEquals("Hashed password should never equal plaintext password",
            password, hashedPassword);
    }
    
    /**
     * Property: For any password, the BCrypt hash should be verifiable using 
     * BCrypt's verification function with the original password.
     */
    @Property(trials = 50)
    public void hashedPasswordShouldBeVerifiableWithOriginalPassword(String password) {
        // Skip empty passwords or passwords that exceed BCrypt's 72-byte limit
        if (password == null || password.isEmpty() || password.getBytes().length > 72) {
            return;
        }
        
        // Hash the password
        String hashedPassword = passwordEncoder.encode(password);
        
        // The hash should be verifiable with the original password
        assertTrue("Hashed password should be verifiable with original password",
            passwordEncoder.matches(password, hashedPassword));
    }
    
    /**
     * Property: For any password, the BCrypt hash should NOT be verifiable 
     * with a different password.
     */
    @Property(trials = 50)
    public void hashedPasswordShouldNotBeVerifiableWithDifferentPassword(String password) {
        // Skip empty, very short passwords, or passwords that exceed BCrypt's 72-byte limit
        // Also skip passwords where adding "X" would exceed the limit (to avoid truncation issues)
        if (password == null || password.length() < 2 || password.getBytes().length > 70) {
            return;
        }
        
        // Hash the password
        String hashedPassword = passwordEncoder.encode(password);
        
        // Create a different password by appending a character
        String differentPassword = password + "X";
        
        // The hash should NOT be verifiable with a different password
        assertFalse("Hashed password should not be verifiable with different password",
            passwordEncoder.matches(differentPassword, hashedPassword));
    }
    
    /**
     * Property: For any password, hashing it multiple times should produce 
     * different hashes (due to random salt), but all should be verifiable.
     */
    @Property(trials = 50)
    public void multipleHashesOfSamePasswordShouldBeDifferentButVerifiable(String password) {
        // Skip empty passwords or passwords that exceed BCrypt's 72-byte limit
        if (password == null || password.isEmpty() || password.getBytes().length > 72) {
            return;
        }
        
        // Hash the password twice
        String hash1 = passwordEncoder.encode(password);
        String hash2 = passwordEncoder.encode(password);
        
        // The hashes should be different (due to random salt)
        assertNotEquals("Multiple hashes of same password should be different due to salt",
            hash1, hash2);
        
        // Both hashes should be verifiable with the original password
        assertTrue("First hash should be verifiable with original password",
            passwordEncoder.matches(password, hash1));
        assertTrue("Second hash should be verifiable with original password",
            passwordEncoder.matches(password, hash2));
    }
    
    /**
     * Property: For any password, the BCrypt hash should start with the BCrypt prefix
     * and have the expected format.
     */
    @Property(trials = 50)
    public void hashedPasswordShouldHaveBCryptFormat(String password) {
        // Skip empty passwords or passwords that exceed BCrypt's 72-byte limit
        if (password == null || password.isEmpty() || password.getBytes().length > 72) {
            return;
        }
        
        // Hash the password
        String hashedPassword = passwordEncoder.encode(password);
        
        // BCrypt hashes should start with $2a$ or $2b$ (BCrypt version identifiers)
        assertTrue("Hashed password should start with BCrypt prefix",
            hashedPassword.startsWith("$2a$") || hashedPassword.startsWith("$2b$"));
        
        // BCrypt hashes with cost factor 12 should have $12$ after the version
        assertTrue("Hashed password should include cost factor 12",
            hashedPassword.contains("$12$"));
        
        // BCrypt hashes should be 60 characters long
        assertEquals("BCrypt hash should be 60 characters long",
            60, hashedPassword.length());
    }
    
    /**
     * Property: For any password, attempting to verify with null or empty string 
     * should fail.
     */
    @Property(trials = 50)
    public void hashedPasswordShouldNotMatchNullOrEmpty(String password) {
        // Skip empty passwords or passwords that exceed BCrypt's 72-byte limit
        if (password == null || password.isEmpty() || password.getBytes().length > 72) {
            return;
        }
        
        // Hash the password
        String hashedPassword = passwordEncoder.encode(password);
        
        // Verification with empty string should fail (if password is not empty)
        if (!password.isEmpty()) {
            assertFalse("Hashed password should not match empty string",
                passwordEncoder.matches("", hashedPassword));
        }
    }
}
