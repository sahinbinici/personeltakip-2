package com.bidb.personetakip.service;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.util.Base64;

/**
 * Generator for valid QR code values (Base64-encoded strings)
 */
public class QrCodeValueGenerator extends Generator<String> {
    
    public QrCodeValueGenerator() {
        super(String.class);
    }
    
    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {
        // Generate random bytes and encode as Base64 (similar to actual QR code generation)
        byte[] randomBytes = new byte[32];
        random.nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
    }
}
