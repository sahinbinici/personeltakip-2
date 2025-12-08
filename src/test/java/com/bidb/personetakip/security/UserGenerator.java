package com.bidb.personetakip.security;

import com.bidb.personetakip.model.User;
import com.bidb.personetakip.model.UserRole;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

/**
 * Generator for creating random User instances for property-based testing.
 */
public class UserGenerator extends Generator<User> {
    
    public UserGenerator() {
        super(User.class);
    }
    
    @Override
    public User generate(SourceOfRandomness random, GenerationStatus status) {
        // Generate random TC No (11 digits)
        String tcNo = generateTcNo(random);
        
        // Generate random personnel number
        String personnelNo = String.format("%06d", random.nextInt(1, 999999));
        
        // Generate random names
        String firstName = generateName(random, "FirstName");
        String lastName = generateName(random, "LastName");
        
        // Generate random mobile phone
        String mobilePhone = String.format("05%09d", random.nextLong(0, 999999999));
        
        // Generate random password hash (simulated BCrypt hash)
        String passwordHash = "$2a$12$" + random.nextBytes(53).toString();
        
        // Random role
        UserRole role = random.choose(UserRole.values());
        
        return User.builder()
                .id(random.nextLong(1, 1000000))
                .tcNo(tcNo)
                .personnelNo(personnelNo)
                .firstName(firstName)
                .lastName(lastName)
                .mobilePhone(mobilePhone)
                .passwordHash(passwordHash)
                .role(role)
                .build();
    }
    
    /**
     * Generates a valid 11-digit TC No.
     */
    private String generateTcNo(SourceOfRandomness random) {
        StringBuilder tcNo = new StringBuilder();
        // First digit cannot be 0
        tcNo.append(random.nextInt(1, 10));
        // Next 9 digits
        for (int i = 0; i < 9; i++) {
            tcNo.append(random.nextInt(0, 10));
        }
        // Last digit (checksum - simplified, just random for testing)
        tcNo.append(random.nextInt(0, 10));
        return tcNo.toString();
    }
    
    /**
     * Generates a random name.
     */
    private String generateName(SourceOfRandomness random, String prefix) {
        return prefix + random.nextInt(1, 10000);
    }
}
