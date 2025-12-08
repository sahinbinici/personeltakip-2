package com.bidb.personetakip.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for User entity.
 */
class UserTest {
    
    @Test
    void testUserCreation() {
        User user = User.builder()
                .tcNo("12345678901")
                .personnelNo("EMP001")
                .firstName("John")
                .lastName("Doe")
                .mobilePhone("+905551234567")
                .passwordHash("$2a$12$hashedpassword")
                .role(UserRole.NORMAL_USER)
                .build();
        
        assertNotNull(user);
        assertEquals("12345678901", user.getTcNo());
        assertEquals("EMP001", user.getPersonnelNo());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("+905551234567", user.getMobilePhone());
        assertEquals("$2a$12$hashedpassword", user.getPasswordHash());
        assertEquals(UserRole.NORMAL_USER, user.getRole());
    }
    
    @Test
    void testOnCreateSetsDefaultRole() {
        User user = User.builder()
                .tcNo("12345678901")
                .personnelNo("EMP001")
                .firstName("John")
                .lastName("Doe")
                .mobilePhone("+905551234567")
                .passwordHash("$2a$12$hashedpassword")
                .build();
        
        assertNull(user.getRole());
        user.onCreate();
        assertEquals(UserRole.NORMAL_USER, user.getRole());
    }
    
    @Test
    void testOnCreateDoesNotOverrideExistingRole() {
        User user = User.builder()
                .tcNo("12345678901")
                .personnelNo("EMP001")
                .firstName("John")
                .lastName("Doe")
                .mobilePhone("+905551234567")
                .passwordHash("$2a$12$hashedpassword")
                .role(UserRole.ADMIN)
                .build();
        
        user.onCreate();
        assertEquals(UserRole.ADMIN, user.getRole());
    }
}
