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
    private com.bidb.personetakip.repository.external.ExternalTelephoneRepository externalTelephoneRepository;
    
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
            externalTelephoneRepository,
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
        // Create a mock personnel object to return
        ExternalPersonnel mockPersonnel = ExternalPersonnel.builder()
            .esicno(12345L)
            .tckiml(tcNo)
            .peradi("Test")
            .soyadi("User")
            .brkodu("BRK001")
            .unvkod("UNV001")
            .build();
        
        // Configure mock to return the personnel when queried with both parameters
        when(externalPersonnelRepository.findByTcNoAndPersonnelNo(tcNo, personnelNo))
            .thenReturn(Optional.of(mockPersonnel));
        
        // Call the validatePersonnel method
        try {
            ExternalPersonnelDto result = registrationService.validatePersonnel(tcNo, personnelNo);
            
            // Verify that the repository method was called with BOTH parameters
            ArgumentCaptor<String> tcNoCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> personnelNoCaptor = ArgumentCaptor.forClass(String.class);
            
            verify(externalPersonnelRepository, times(1))
                .findByTcNoAndPersonnelNo(tcNoCaptor.capture(), personnelNoCaptor.capture());
            
            // Verify both parameters were passed to the query
            assertEquals("TC No should be passed to query", tcNo, tcNoCaptor.getValue());
            assertEquals("Personnel No should be passed to query", personnelNo, personnelNoCaptor.getValue());
            
            // Verify that single-parameter methods were NOT called
            verify(externalPersonnelRepository, never()).findByTcNo(anyString());
            verify(externalPersonnelRepository, never()).findByPersonnelNo(anyString());
            
            // Verify the result contains the correct data
            assertNotNull("Result should not be null", result);
            assertEquals("TC No should match", tcNo, result.tcNo());
            assertEquals("Personnel No should match", personnelNo, result.personnelNo());
            
        } catch (PersonnelNotFoundException e) {
            // This is acceptable - the property still holds if personnel is not found
            // The important part is that the query was made with both parameters
            verify(externalPersonnelRepository, times(1))
                .findByTcNoAndPersonnelNo(tcNo, personnelNo);
        }
    }
    
    @Property(trials = 100)
    public void externalDatabaseQueryShouldNotUseOnlyTcNo(
        @From(TcNoGenerator.class) String tcNo,
        @From(PersonnelNoGenerator.class) String personnelNo
    ) {
        // Create a mock personnel object
        ExternalPersonnel mockPersonnel = ExternalPersonnel.builder()
            .esicno(12345L)
            .tckiml(tcNo)
            .peradi("Test")
            .soyadi("User")
            .brkodu("BRK001")
            .unvkod("UNV001")
            .build();
        
        // Configure mock to return the personnel
        when(externalPersonnelRepository.findByTcNoAndPersonnelNo(tcNo, personnelNo))
            .thenReturn(Optional.of(mockPersonnel));
        
        // Call the validatePersonnel method
        try {
            registrationService.validatePersonnel(tcNo, personnelNo);
            
            // Verify that the single-parameter TC No method was NOT used
            verify(externalPersonnelRepository, never()).findByTcNo(anyString());
            
        } catch (PersonnelNotFoundException e) {
            // Still verify that single-parameter method was not called
            verify(externalPersonnelRepository, never()).findByTcNo(anyString());
        }
    }
    
    @Property(trials = 100)
    public void externalDatabaseQueryShouldNotUseOnlyPersonnelNo(
        @From(TcNoGenerator.class) String tcNo,
        @From(PersonnelNoGenerator.class) String personnelNo
    ) {
        // Create a mock personnel object
        ExternalPersonnel mockPersonnel = ExternalPersonnel.builder()
            .esicno(12345L)
            .tckiml(tcNo)
            .peradi("Test")
            .soyadi("User")
            .brkodu("BRK001")
            .unvkod("UNV001")
            .build();
        
        // Configure mock to return the personnel
        when(externalPersonnelRepository.findByTcNoAndPersonnelNo(tcNo, personnelNo))
            .thenReturn(Optional.of(mockPersonnel));
        
        // Call the validatePersonnel method
        try {
            registrationService.validatePersonnel(tcNo, personnelNo);
            
            // Verify that the single-parameter Personnel No method was NOT used
            verify(externalPersonnelRepository, never()).findByPersonnelNo(anyString());
            
        } catch (PersonnelNotFoundException e) {
            // Still verify that single-parameter method was not called
            verify(externalPersonnelRepository, never()).findByPersonnelNo(anyString());
        }
    }
}
