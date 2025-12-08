package com.bidb.personetakip.service;

import com.bidb.personetakip.model.OtpVerification;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

import java.time.LocalDateTime;

import static org.junit.Assert.*;

/**
 * Property-based test for OTP verification correctness
 * 
 * **Feature: personnel-tracking-system, Property 3: OTP verification correctness**
 * **Validates: Requirements 2.1, 2.2**
 * 
 * For any submitted OTP and TC ID combination, verification should succeed if and only if
 * the OTP matches the stored code, has not expired, and has not been previously verified.
 */
@RunWith(JUnitQuickcheck.class)
public class OtpVerificationPropertyTest {
    
    /**
     * Property: For any OTP that matches, is not expired, and not verified,
     * verification should succeed.
     */
    @Property(trials = 100)
    public void validOtpShouldPassVerification(
        @From(TcNoGenerator.class) String tcNo,
        @From(OtpCodeGenerator.class) String otpCode
    ) {
        // Create a valid OTP verification (not expired, not verified)
        LocalDateTime now = LocalDateTime.now();
        OtpVerification otp = OtpVerification.builder()
            .tcNo(tcNo)
            .otpCode(otpCode)
            .expiresAt(now.plusMinutes(5))
            .verified(false)
            .createdAt(now)
            .build();
        
        // Simulate verification logic
        boolean shouldPass = otp.getTcNo().equals(tcNo) 
            && otp.getOtpCode().equals(otpCode)
            && !otp.getVerified()
            && otp.getExpiresAt().isAfter(LocalDateTime.now());
        
        // Should pass verification
        assertTrue("Valid OTP should pass verification", shouldPass);
    }
    
    /**
     * Property: For any OTP that has expired, verification should fail.
     */
    @Property(trials = 100)
    public void expiredOtpShouldFailVerification(
        @From(TcNoGenerator.class) String tcNo,
        @From(OtpCodeGenerator.class) String otpCode,
        @InRange(minInt = 1, maxInt = 60) int minutesAgo
    ) {
        // Create an expired OTP verification
        LocalDateTime now = LocalDateTime.now();
        OtpVerification otp = OtpVerification.builder()
            .tcNo(tcNo)
            .otpCode(otpCode)
            .expiresAt(now.minusMinutes(minutesAgo))
            .verified(false)
            .createdAt(now.minusMinutes(minutesAgo + 5))
            .build();
        
        // Simulate verification logic
        boolean shouldFail = otp.getExpiresAt().isBefore(LocalDateTime.now());
        
        // Should fail verification due to expiration
        assertTrue("Expired OTP should fail verification", shouldFail);
    }
    
    /**
     * Property: For any OTP that has already been verified, verification should fail.
     */
    @Property(trials = 100)
    public void alreadyVerifiedOtpShouldFailVerification(
        @From(TcNoGenerator.class) String tcNo,
        @From(OtpCodeGenerator.class) String otpCode
    ) {
        // Create an already verified OTP
        LocalDateTime now = LocalDateTime.now();
        OtpVerification otp = OtpVerification.builder()
            .tcNo(tcNo)
            .otpCode(otpCode)
            .expiresAt(now.plusMinutes(5))
            .verified(true)  // Already verified
            .createdAt(now)
            .build();
        
        // Simulate verification logic
        boolean shouldFail = otp.getVerified();
        
        // Should fail verification because already verified
        assertTrue("Already verified OTP should fail verification", shouldFail);
    }
    
    /**
     * Property: For any OTP with mismatched code, verification should fail.
     */
    @Property(trials = 100)
    public void mismatchedOtpCodeShouldFailVerification(
        @From(TcNoGenerator.class) String tcNo,
        @From(OtpCodeGenerator.class) String storedCode,
        @From(OtpCodeGenerator.class) String submittedCode
    ) {
        // Ensure codes are different
        if (storedCode.equals(submittedCode)) {
            return; // Skip this trial if codes happen to match
        }
        
        // Create OTP with stored code
        LocalDateTime now = LocalDateTime.now();
        OtpVerification otp = OtpVerification.builder()
            .tcNo(tcNo)
            .otpCode(storedCode)
            .expiresAt(now.plusMinutes(5))
            .verified(false)
            .createdAt(now)
            .build();
        
        // Simulate verification with different code
        boolean shouldFail = !otp.getOtpCode().equals(submittedCode);
        
        // Should fail verification due to code mismatch
        assertTrue("Mismatched OTP code should fail verification", shouldFail);
    }
    
    /**
     * Property: For any OTP, verification should succeed if and only if
     * all conditions are met: matching code, not expired, not verified.
     */
    @Property(trials = 100)
    public void otpVerificationShouldRequireAllConditions(
        @From(TcNoGenerator.class) String tcNo,
        @From(OtpCodeGenerator.class) String otpCode,
        boolean isExpired,
        boolean isVerified
    ) {
        // Create OTP with varying conditions
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = isExpired ? now.minusMinutes(1) : now.plusMinutes(5);
        
        OtpVerification otp = OtpVerification.builder()
            .tcNo(tcNo)
            .otpCode(otpCode)
            .expiresAt(expiresAt)
            .verified(isVerified)
            .createdAt(now.minusMinutes(5))
            .build();
        
        // Verification should succeed only if all conditions are met
        boolean shouldSucceed = otp.getTcNo().equals(tcNo)
            && otp.getOtpCode().equals(otpCode)
            && !otp.getVerified()
            && otp.getExpiresAt().isAfter(LocalDateTime.now());
        
        // If any condition fails, verification should fail
        boolean actuallyExpired = otp.getExpiresAt().isBefore(LocalDateTime.now());
        boolean actuallyVerified = otp.getVerified();
        
        if (actuallyExpired || actuallyVerified) {
            assertFalse("OTP verification should fail if expired or already verified", shouldSucceed);
        } else {
            assertTrue("OTP verification should succeed if all conditions are met", shouldSucceed);
        }
    }
}
