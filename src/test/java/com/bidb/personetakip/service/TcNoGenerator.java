package com.bidb.personetakip.service;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

/**
 * Generator for Turkish Citizen ID numbers (TC No) for property-based testing
 */
public class TcNoGenerator extends Generator<String> {
    
    public TcNoGenerator() {
        super(String.class);
    }
    
    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {
        // Generate 11-digit TC No
        StringBuilder tcNo = new StringBuilder();
        for (int i = 0; i < 11; i++) {
            tcNo.append(random.nextInt(0, 10));
        }
        return tcNo.toString();
    }
}
