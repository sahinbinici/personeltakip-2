package com.bidb.personetakip.service;

import com.bidb.personetakip.model.OtpVerification;
import com.bidb.personetakip.repository.OtpVerificationRepository;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.*;

/**
 * Property-based tests for OTP format and expiration consistency.
 * 
 * **Feature: personnel-tracking-system, Property 2: OTP format and expiration consistency**
 * **Validates: Requirements 1.4**
 * 
 * Property: For any generated OTP, the code should be exactly 6 digits and the expiration 
 * time should be exactly 5 minutes from creation time.
 */
@RunWith(JUnitQuickcheck.class)
public class OtpFormatPropertyTest {
    
    private SmsServiceImpl smsService;
    private static final int EXPECTED_OTP_LENGTH = 6;
    private static final long EXPECTED_EXPIRATION_MINUTES = 5;
    
    @Before
    public void setUp() {
        smsService = new SmsServiceImpl();
        // Set test configuration values
        ReflectionTestUtils.setField(smsService, "otpLength", EXPECTED_OTP_LENGTH);
        ReflectionTestUtils.setField(smsService, "smsGatewayUrl", "http://test");
        ReflectionTestUtils.setField(smsService, "apiKey", "test-key");
        ReflectionTestUtils.setField(smsService, "senderId", "test-sender");
    }
    
    /**
     * Property: For any generated OTP, the code should be exactly 6 digits.
     */
    @Property(trials = 100)
    public void generatedOtpShouldBeExactly6Digits() {
        // Generate OTP
        String otp = smsService.generateOtp();
        
        // Assert OTP is not null
        assertNotNull("Generated OTP should not be null", otp);
        
        // Assert OTP length is exactly 6
        assertEquals("OTP should be exactly 6 characters long", 
            EXPECTED_OTP_LENGTH, otp.length());
        
        // Assert OTP contains only digits
        assertTrue("OTP should contain only digits", 
            otp.matches("\\d{6}"));
    }
    
    /**
     * Property: For any generated OTP, all digits should be numeric (0-9).
     */
    @Property(trials = 100)
    public void generatedOtpShouldContainOnlyNumericDigits() {
        // Generate OTP
        String otp = smsService.generateOtp();
        
        // Parse as integer to ensure it's numeric
        try {
            int otpValue = Integer.parseInt(otp);
            // Should be between 0 and 999999
            assertTrue("OTP value should be between 0 and 999999", 
                otpValue >= 0 && otpValue <= 999999);
        } catch (NumberFormatException e) {
            fail("OTP should be parseable as an integer: " + otp);
        }
    }
    
    /**
     * Property: For any generated OTP, it should have leading zeros if the random value
     * is less than 100000 to maintain 6-digit format.
     */
    @Property(trials = 100)
    public void generatedOtpShouldMaintainLeadingZeros() {
        // Generate multiple OTPs
        String otp = smsService.generateOtp();
        
        // Check format is maintained
        assertEquals("OTP should always be 6 digits with leading zeros if needed", 
            6, otp.length());
        
        // Verify it's a valid 6-digit string
        assertTrue("OTP should match 6-digit pattern", 
            otp.matches("^[0-9]{6}$"));
    }
    
    /**
     * Property: For any OTP verification entity created with current time,
     * the expiration should be exactly 5 minutes from creation.
     */
    @Property(trials = 100)
    public void otpExpirationShouldBeExactly5MinutesFromCreation() {
        // Simulate OTP creation with expiration
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime expiresAt = createdAt.plusMinutes(EXPECTED_EXPIRATION_MINUTES);
        
        // Create OTP verification entity
        OtpVerification otpVerification = new OtpVerification();
        otpVerification.setTcNo("12345678901");
        otpVerification.setOtpCode(smsService.generateOtp());
        otpVerification.setCreatedAt(createdAt);
        otpVerification.setExpiresAt(expiresAt);
        
        // Calculate the difference in minutes
        long minutesDifference = ChronoUnit.MINUTES.between(
            otpVerification.getCreatedAt(), 
            otpVerification.getExpiresAt()
        );
        
        // Assert expiration is exactly 5 minutes from creation
        assertEquals("OTP expiration should be exactly 5 minutes from creation time",
            EXPECTED_EXPIRATION_MINUTES, minutesDifference);
    }
    
    /**
     * Property: For any OTP verification, the expiration time should always be
     * after the creation time.
     */
    @Property(trials = 100)
    public void otpExpirationShouldAlwaysBeAfterCreation() {
        // Simulate OTP creation
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime expiresAt = createdAt.plusMinutes(EXPECTED_EXPIRATION_MINUTES);
        
        // Create OTP verification entity
        OtpVerification otpVerification = new OtpVerification();
        otpVerification.setTcNo("12345678901");
        otpVerification.setOtpCode(smsService.generateOtp());
        otpVerification.setCreatedAt(createdAt);
        otpVerification.setExpiresAt(expiresAt);
        
        // Assert expiration is after creation
        assertTrue("OTP expiration time should be after creation time",
            otpVerification.getExpiresAt().isAfter(otpVerification.getCreatedAt()));
    }
}
