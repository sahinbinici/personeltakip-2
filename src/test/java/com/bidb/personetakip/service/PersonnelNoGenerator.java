package com.bidb.personetakip.service;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

/**
 * Generator for Personnel Numbers for property-based testing
 */
public class PersonnelNoGenerator extends Generator<String> {
    
    public PersonnelNoGenerator() {
        super(String.class);
    }
    
    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {
        // Generate personnel number (alphanumeric, 5-10 characters)
        int length = random.nextInt(5, 11);
        StringBuilder personnelNo = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            if (random.nextBoolean()) {
                // Add a digit
                personnelNo.append(random.nextInt(0, 10));
            } else {
                // Add an uppercase letter
                personnelNo.append((char) random.nextInt('A', 'Z' + 1));
            }
        }
        
        return personnelNo.toString();
    }
}
