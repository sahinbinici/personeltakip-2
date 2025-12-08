package com.bidb.personetakip.service;

import com.bidb.personetakip.util.PasswordValidator;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Property-based test for password validation rules enforcement
 * 
 * **Feature: personnel-tracking-system, Property 4: Password validation rules enforcement**
 * **Validates: Requirements 3.1, 3.2**
 * 
 * For any submitted password, validation should reject passwords that are shorter than 8 characters
 * or lack at least one uppercase letter, one lowercase letter, or one special character.
 */
@RunWith(JUnitQuickcheck.class)
public class PasswordValidationPropertyTest {
    
    /**
     * Property: For any password shorter than 8 characters, validation should fail.
     */
    @Property(trials = 100)
    public void passwordShorterThan8CharactersShouldFail(
        @InRange(minInt = 1, maxInt = 7) int length
    ) {
        // Generate a password with length < 8 (but not empty)
        String password = generatePasswordWithLength(length);
        
        // Validate
        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);
        
        // Should fail
        assertFalse("Password shorter than 8 characters should fail validation", result.isValid());
        assertTrue("Error message should mention minimum length or required", 
            result.getErrorMessage().toLowerCase().contains("8 characters") ||
            result.getErrorMessage().toLowerCase().contains("required"));
    }
    
    /**
     * Property: For any password without an uppercase letter, validation should fail.
     */
    @Property(trials = 100)
    public void passwordWithoutUppercaseShouldFail(
        @InRange(minInt = 8, maxInt = 20) int length
    ) {
        // Generate a password without uppercase (only lowercase, digits, special chars)
        String password = generatePasswordWithoutUppercase(length);
        
        // Validate
        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);
        
        // Should fail
        assertFalse("Password without uppercase letter should fail validation", result.isValid());
        assertTrue("Error message should mention uppercase requirement", 
            result.getErrorMessage().toLowerCase().contains("uppercase"));
    }
    
    /**
     * Property: For any password without a lowercase letter, validation should fail.
     */
    @Property(trials = 100)
    public void passwordWithoutLowercaseShouldFail(
        @InRange(minInt = 8, maxInt = 20) int length
    ) {
        // Generate a password without lowercase (only uppercase, digits, special chars)
        String password = generatePasswordWithoutLowercase(length);
        
        // Validate
        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);
        
        // Should fail
        assertFalse("Password without lowercase letter should fail validation", result.isValid());
        assertTrue("Error message should mention lowercase requirement", 
            result.getErrorMessage().toLowerCase().contains("lowercase"));
    }
    
    /**
     * Property: For any password without a special character, validation should fail.
     */
    @Property(trials = 100)
    public void passwordWithoutSpecialCharacterShouldFail(
        @InRange(minInt = 8, maxInt = 20) int length
    ) {
        // Generate a password without special characters (only letters and digits)
        String password = generatePasswordWithoutSpecialChar(length);
        
        // Validate
        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);
        
        // Should fail
        assertFalse("Password without special character should fail validation", result.isValid());
        assertTrue("Error message should mention special character requirement", 
            result.getErrorMessage().toLowerCase().contains("special"));
    }
    
    /**
     * Property: For any password that meets all requirements, validation should succeed.
     */
    @Property(trials = 100)
    public void validPasswordShouldPass(
        @InRange(minInt = 8, maxInt = 30) int length
    ) {
        // Generate a valid password with all requirements
        String password = generateValidPassword(length);
        
        // Validate
        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);
        
        // Should pass
        assertTrue("Valid password should pass validation", result.isValid());
        assertTrue("Valid password should have no errors", result.getErrors().isEmpty());
    }
    
    /**
     * Property: For any null or empty password, validation should fail.
     */
    @Property(trials = 100)
    public void nullOrEmptyPasswordShouldFail() {
        // Test null
        PasswordValidator.ValidationResult nullResult = PasswordValidator.validate(null);
        assertFalse("Null password should fail validation", nullResult.isValid());
        
        // Test empty
        PasswordValidator.ValidationResult emptyResult = PasswordValidator.validate("");
        assertFalse("Empty password should fail validation", emptyResult.isValid());
    }
    
    // Helper methods to generate passwords with specific characteristics
    
    private String generatePasswordWithLength(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append('a');
        }
        return sb.toString();
    }
    
    private String generatePasswordWithoutUppercase(int length) {
        // Generate password with lowercase, digits, and special chars, but no uppercase
        StringBuilder sb = new StringBuilder();
        sb.append("abc");  // lowercase
        sb.append("123");  // digits
        sb.append("!@#");  // special chars
        
        while (sb.length() < length) {
            sb.append('a');
        }
        
        return sb.substring(0, length);
    }
    
    private String generatePasswordWithoutLowercase(int length) {
        // Generate password with uppercase, digits, and special chars, but no lowercase
        StringBuilder sb = new StringBuilder();
        sb.append("ABC");  // uppercase
        sb.append("123");  // digits
        sb.append("!@#");  // special chars
        
        while (sb.length() < length) {
            sb.append('A');
        }
        
        return sb.substring(0, length);
    }
    
    private String generatePasswordWithoutSpecialChar(int length) {
        // Generate password with uppercase, lowercase, and digits, but no special chars
        StringBuilder sb = new StringBuilder();
        sb.append("Abc");  // uppercase and lowercase
        sb.append("123");  // digits
        
        while (sb.length() < length) {
            if (sb.length() % 2 == 0) {
                sb.append('A');
            } else {
                sb.append('a');
            }
        }
        
        return sb.substring(0, length);
    }
    
    private String generateValidPassword(int length) {
        // Generate password with all requirements met
        StringBuilder sb = new StringBuilder();
        sb.append("Aa1!");  // Uppercase, lowercase, digit, special char
        
        while (sb.length() < length) {
            int pos = sb.length() % 4;
            switch (pos) {
                case 0: sb.append('A'); break;
                case 1: sb.append('a'); break;
                case 2: sb.append('1'); break;
                case 3: sb.append('!'); break;
            }
        }
        
        return sb.substring(0, length);
    }
}
