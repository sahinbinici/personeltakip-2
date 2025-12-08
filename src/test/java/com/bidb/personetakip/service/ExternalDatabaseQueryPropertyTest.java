package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.ExternalPersonnelDto;
import com.bidb.personetakip.exception.PersonnelNotFoundException;
import com.bidb.personetakip.model.ExternalPersonnel;
import com.bidb.personetakip.repository.external.ExternalPersonnelRepository;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Property-based test for external database query parameters
 * 
 * **Feature: personnel-tracking-system, Property 20: External database query parameters**
 * **Validates: Requirements 10.2**
 * 
 * For any personnel validation request, the query to the External Database
 * should include both TC ID and Personnel Number as search criteria.
 */
@RunWith(JUnitQuickcheck.class)
public class ExternalDatabaseQueryPropertyTest {
    
    @Mock
    private ExternalPersonnelRepository externalPersonnelRepository;
    
    @Mock
    private com.bidb.personetakip.repository.UserRepository userRepository;
    
    @Mock
    private com.bidb.personetakip.repository.OtpVerificationRepository otpVerificationRepository;
    
    @Mock
    private SmsService smsService;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    private RegistrationServiceImpl registrationService;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        registrationService = new RegistrationServiceImpl(
            externalPersonnelRepository,
            userRepository,
            otpVerificationRepository,
            smsService,
            passwordEncoder
        );
    }
    
    @Property(trials = 100)
    public void externalDatabaseQueryShouldIncludeBothTcNoAndPersonnelNo(
        @From(TcNoGenerator.class) String tcNo,
        @From(PersonnelNoGenerator.class) String personnelNo
    ) {
        // Parse personnel number to Long
        Long esicno;
        try {
            esicno = Long.parseLong(personnelNo);
        } catch (NumberFormatException e) {
            // Skip invalid personnel numbers
            return;
        }
        
        // Create a mock full data object
        com.bidb.personetakip.dto.ExternalPersonnelFullDto mockFullData = new com.bidb.personetakip.dto.ExternalPersonnelFullDto() {
            @Override public Long getEsicno() { return esicno; }
            @Override public String getTckiml() { return tcNo; }
            @Override public String getPeradi() { return "Test"; }
            @Override public String getSoyadi() { return "User"; }
            @Override public String getBrkodu() { return "BRK001"; }
            @Override public String getBrkdac() { return "Department"; }
            @Override public String getUnvkod() { return "UNV001"; }
            @Override public String getUnvack() { return "Title"; }
            @Override public String getTelefo() { return "05551234567"; }
        };
        
        // Configure mock to return the full data when queried with both parameters
        when(externalPersonnelRepository.findCompletePersonnelData(tcNo, esicno))
            .thenReturn(Optional.of(mockFullData));
        
        // Call the validatePersonnel method
        try {
            ExternalPersonnelDto result = registrationService.validatePersonnel(tcNo, personnelNo);
            
            // Verify that the repository method was called with BOTH parameters
            ArgumentCaptor<String> tcNoCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> esicnoCaptor = ArgumentCaptor.forClass(Long.class);
            
            verify(externalPersonnelRepository, times(1))
                .findCompletePersonnelData(tcNoCaptor.capture(), esicnoCaptor.capture());
            
            // Verify both parameters were passed to the query
            assertEquals("TC No should be passed to query", tcNo, tcNoCaptor.getValue());
            assertEquals("Personnel No should be passed to query", esicno, esicnoCaptor.getValue());
            
            // Verify the result contains the correct data
            assertNotNull("Result should not be null", result);
            assertEquals("TC No should match", tcNo, result.tcNo());
            assertEquals("Personnel No should match", personnelNo, result.personnelNo());
            
        } catch (PersonnelNotFoundException e) {
            // This is acceptable - the property still holds if personnel is not found
            // The important part is that the query was made with both parameters
            verify(externalPersonnelRepository, times(1))
                .findCompletePersonnelData(tcNo, esicno);
        }
    }
    

}
