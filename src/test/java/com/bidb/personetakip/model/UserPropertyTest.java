package com.bidb.personetakip.model;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Property-based tests for User entity.
 * **Feature: personnel-tracking-system, Property 6: Default role assignment**
 */
@RunWith(JUnitQuickcheck.class)
public class UserPropertyTest {
    
    /**
     * Property 6: Default role assignment
     * For any newly created user account, the assigned role should always be NORMAL_USER.
     * **Validates: Requirements 3.5, 11.1**
     */
    @Property(trials = 100)
    public void newUserShouldHaveNormalUserRoleByDefault(
            String tcNo,
            String personnelNo,
            String firstName,
            String lastName,
            String mobilePhone,
            String passwordHash) {
        
        // Create a user without explicitly setting the role
        User user = User.builder()
                .tcNo(tcNo)
                .personnelNo(personnelNo)
                .firstName(firstName)
                .lastName(lastName)
                .mobilePhone(mobilePhone)
                .passwordHash(passwordHash)
                .build();
        
        // Trigger the @PrePersist callback
        user.onCreate();
        
        // Verify that the role is set to NORMAL_USER
        assertNotNull("Role should not be null after onCreate", user.getRole());
        assertEquals("Default role should be NORMAL_USER", UserRole.NORMAL_USER, user.getRole());
    }
    
    /**
     * Property: Role should remain unchanged if explicitly set
     * For any user with an explicitly set role, onCreate should not override it.
     */
    @Property(trials = 100)
    public void explicitlySetRoleShouldNotBeOverridden(
            String tcNo,
            String personnelNo,
            String firstName,
            String lastName,
            String mobilePhone,
            String passwordHash) {
        
        // Test with ADMIN role
        User adminUser = User.builder()
                .tcNo(tcNo)
                .personnelNo(personnelNo)
                .firstName(firstName)
                .lastName(lastName)
                .mobilePhone(mobilePhone)
                .passwordHash(passwordHash)
                .role(UserRole.ADMIN)
                .build();
        
        adminUser.onCreate();
        assertEquals("Explicitly set ADMIN role should not be changed", UserRole.ADMIN, adminUser.getRole());
        
        // Test with SUPER_ADMIN role
        User superAdminUser = User.builder()
                .tcNo(tcNo)
                .personnelNo(personnelNo)
                .firstName(firstName)
                .lastName(lastName)
                .mobilePhone(mobilePhone)
                .passwordHash(passwordHash)
                .role(UserRole.SUPER_ADMIN)
                .build();
        
        superAdminUser.onCreate();
        assertEquals("Explicitly set SUPER_ADMIN role should not be changed", UserRole.SUPER_ADMIN, superAdminUser.getRole());
    }
}
