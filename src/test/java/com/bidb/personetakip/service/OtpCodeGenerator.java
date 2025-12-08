package com.bidb.personetakip.service;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

/**
 * Generator for 6-digit OTP codes for property-based testing
 */
public class OtpCodeGenerator extends Generator<String> {
    
    public OtpCodeGenerator() {
        super(String.class);
    }
    
    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {
        // Generate 6-digit OTP code
        int otpValue = random.nextInt(0, 1000000);
        return String.format("%06d", otpValue);
    }
}
