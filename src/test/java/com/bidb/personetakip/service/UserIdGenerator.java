package com.bidb.personetakip.service;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

/**
 * Generator for valid user IDs (positive Long values)
 */
public class UserIdGenerator extends Generator<Long> {
    
    public UserIdGenerator() {
        super(Long.class);
    }
    
    @Override
    public Long generate(SourceOfRandomness random, GenerationStatus status) {
        // Generate positive user IDs between 1 and 1,000,000
        return random.nextLong(1L, 1_000_000L);
    }
}
