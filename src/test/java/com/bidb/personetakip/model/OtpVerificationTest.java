package com.bidb.personetakip.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OtpVerification entity.
 */
class OtpVerificationTest {
    
    @Test
    void testOtpVerificationCreation() {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);
        
        OtpVerification otp = OtpVerification.builder()
                .tcNo("12345678901")
                .otpCode("123456")
                .expiresAt(expiresAt)
                .verified(false)
                .build();
        
        assertNotNull(otp);
        assertEquals("12345678901", otp.getTcNo());
        assertEquals("123456", otp.getOtpCode());
        assertEquals(expiresAt, otp.getExpiresAt());
        assertFalse(otp.getVerified());
    }
    
    @Test
    void testIsExpiredReturnsTrueForExpiredOtp() {
        OtpVerification otp = OtpVerification.builder()
                .tcNo("12345678901")
                .otpCode("123456")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .verified(false)
                .build();
        
        assertTrue(otp.isExpired());
    }
    
    @Test
    void testIsExpiredReturnsFalseForValidOtp() {
        OtpVerification otp = OtpVerification.builder()
                .tcNo("12345678901")
                .otpCode("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .build();
        
        assertFalse(otp.isExpired());
    }
    
    @Test
    void testIsValidReturnsTrueForUnverifiedNonExpiredOtp() {
        OtpVerification otp = OtpVerification.builder()
                .tcNo("12345678901")
                .otpCode("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .build();
        
        assertTrue(otp.isValid());
    }
    
    @Test
    void testIsValidReturnsFalseForExpiredOtp() {
        OtpVerification otp = OtpVerification.builder()
                .tcNo("12345678901")
                .otpCode("123456")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .verified(false)
                .build();
        
        assertFalse(otp.isValid());
    }
    
    @Test
    void testIsValidReturnsFalseForVerifiedOtp() {
        OtpVerification otp = OtpVerification.builder()
                .tcNo("12345678901")
                .otpCode("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(true)
                .build();
        
        assertFalse(otp.isValid());
    }
}
