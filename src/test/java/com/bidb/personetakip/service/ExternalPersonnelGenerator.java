package com.bidb.personetakip.service;

import com.bidb.personetakip.model.ExternalPersonnel;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

/**
 * Generator for ExternalPersonnel entities for property-based testing
 */
public class ExternalPersonnelGenerator extends Generator<ExternalPersonnel> {
    
    private static final String[] FIRST_NAMES = {
        "Ahmet", "Mehmet", "Ayşe", "Fatma", "Ali", "Zeynep", "Mustafa", "Elif"
    };
    
    private static final String[] LAST_NAMES = {
        "Yılmaz", "Kaya", "Demir", "Şahin", "Çelik", "Yıldız", "Öztürk", "Aydın"
    };
    
    public ExternalPersonnelGenerator() {
        super(ExternalPersonnel.class);
    }
    
    @Override
    public ExternalPersonnel generate(SourceOfRandomness random, GenerationStatus status) {
        // Generate 11-digit TC No
        String tcNo = generateTcNo(random);
        
        // Generate personnel number (alphanumeric, 5-10 characters)
        String personnelNo = generatePersonnelNo(random);
        
        // Generate user ID
        Long userId = random.nextLong(1L, 999999L);
        
        // Select random names
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        
        // Generate mobile phone (Turkish format: 05XXXXXXXXX)
        String mobilePhone = "05" + String.format("%09d", random.nextLong(0, 999999999));
        
        return ExternalPersonnel.builder()
            .userId(userId)
            .tcNo(tcNo)
            .personnelNo(personnelNo)
            .firstName(firstName)
            .lastName(lastName)
            .mobilePhone(mobilePhone)
            .build();
    }
    
    private String generateTcNo(SourceOfRandomness random) {
        // Generate 11-digit TC No
        StringBuilder tcNo = new StringBuilder();
        for (int i = 0; i < 11; i++) {
            tcNo.append(random.nextInt(0, 10));
        }
        return tcNo.toString();
    }
    
    private String generatePersonnelNo(SourceOfRandomness random) {
        int length = random.nextInt(5, 11);
        StringBuilder personnelNo = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (random.nextBoolean()) {
                personnelNo.append(random.nextInt(0, 10));
            } else {
                personnelNo.append((char) random.nextInt('A', 'Z' + 1));
            }
        }
        return personnelNo.toString();
    }
}
