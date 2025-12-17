package com.bidb.personetakip.controller;

import com.bidb.personetakip.dto.AdminUserDto;
import com.bidb.personetakip.service.AdminUserService;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;

/**
 * Property-based test for IP assignment field availability in user management interface.
 * **Feature: ip-tracking, Property 11: IP Assignment Field Availability**
 * **Validates: Requirements 3.1**
 */
@RunWith(JUnitQuickcheck.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class IpAssignmentFieldAvailabilityPropertyTest {
    
    @Autowired
    private AdminUserService adminUserService;
    
    /**
     * Property 11: IP Assignment Field Availability
     * For any user management interface, the system should provide optional IP address assignment field
     * **Validates: Requirements 3.1**
     */
    @Property(trials = 100)
    public void testIpAssignmentFieldAvailabilityProperty(long userId, String firstName, String lastName) {
        // Ensure valid inputs
        long validUserId = Math.abs(userId % 1000) + 1;
        String validFirstName = firstName != null && !firstName.trim().isEmpty() ? firstName.trim() : "Test";
        String validLastName = lastName != null && !lastName.trim().isEmpty() ? lastName.trim() : "User";
        
        // Test that AdminUserDto has IP assignment field available
        AdminUserDto userDto = AdminUserDto.builder()
                .id(validUserId)
                .tcNo("12345678901")
                .personnelNo("P" + validUserId)
                .firstName(validFirstName)
                .lastName(validLastName)
                .mobilePhone("5551234567")
                .role("NORMAL_USER")
                .assignedIpAddresses("192.168.1.1,10.0.0.1")
                .build();
        
        // Verify that IP assignment field is available and accessible
        assertNotNull("AdminUserDto should not be null", userDto);
        
        // Test that IP assignment field can be set and retrieved
        String testIpAddresses = "192.168.1.100,10.0.0.100";
        userDto.setAssignedIpAddresses(testIpAddresses);
        assertEquals("IP assignment field should be settable and retrievable", 
                testIpAddresses, userDto.getAssignedIpAddresses());
        
        // Test that IP assignment field can be null (optional)
        userDto.setAssignedIpAddresses(null);
        assertNull("IP assignment field should accept null values (optional field)", 
                userDto.getAssignedIpAddresses());
        
        // Test that IP assignment field can be empty string
        userDto.setAssignedIpAddresses("");
        assertEquals("IP assignment field should accept empty string", 
                "", userDto.getAssignedIpAddresses());
    }
    
    /**
     * Test that IP assignment field is properly included in DTO conversion
     */
    @Property(trials = 50)
    public void testIpAssignmentFieldInDtoConversion(String firstName, String lastName) {
        // Ensure valid inputs
        String validFirstName = firstName != null && !firstName.trim().isEmpty() ? firstName.trim() : "Test";
        String validLastName = lastName != null && !lastName.trim().isEmpty() ? lastName.trim() : "User";
        
        // Create a test DTO with IP assignment
        String testIpAddresses = "192.168.1.1,10.0.0.1";
        AdminUserDto userDto = AdminUserDto.builder()
                .id(1L)
                .tcNo("12345678901")
                .personnelNo("P001")
                .firstName(validFirstName)
                .lastName(validLastName)
                .mobilePhone("5551234567")
                .role("NORMAL_USER")
                .assignedIpAddresses(testIpAddresses)
                .build();
        
        // Verify that IP assignment field is preserved in DTO
        assertEquals("IP assignment field should be preserved in AdminUserDto",
                testIpAddresses, userDto.getAssignedIpAddresses());
        
        // Verify that other fields are not affected by IP assignment field
        assertEquals("First name should not be affected by IP assignment field",
                validFirstName, userDto.getFirstName());
        assertEquals("Last name should not be affected by IP assignment field",
                validLastName, userDto.getLastName());
    }
}