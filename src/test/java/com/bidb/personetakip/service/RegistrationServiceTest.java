package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.ExternalPersonnelDto;
import com.bidb.personetakip.dto.UserDto;
import com.bidb.personetakip.exception.*;
import com.bidb.personetakip.model.ExternalPersonnel;
import com.bidb.personetakip.model.OtpVerification;
import com.bidb.personetakip.model.User;
import com.bidb.personetakip.model.UserRole;
import com.bidb.personetakip.repository.OtpVerificationRepository;
import com.bidb.personetakip.repository.UserRepository;
import com.bidb.personetakip.repository.external.ExternalPersonnelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RegistrationService
 */
@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {
    
    @Mock
    private ExternalPersonnelRepository externalPersonnelRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private OtpVerificationRepository otpVerificationRepository;
    
    @Mock
    private SmsService smsService;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private RegistrationServiceImpl registrationService;
    
    private ExternalPersonnel testPersonnel;
    private String testTcNo = "12345678901";
    private String testPersonnelNo = "12345";
    
    @BeforeEach
    void setUp() {
        testPersonnel = ExternalPersonnel.builder()
            .esicno(12345L)
            .tckiml(testTcNo)
            .peradi("Ahmet")
            .soyadi("Yılmaz")
            .brkodu("BRK001")
            .unvkod("UNV001")
            .build();
    }
    
    @Test
    void validatePersonnel_WithValidData_ShouldReturnPersonnelDto() {
        // Arrange
        com.bidb.personetakip.dto.ExternalPersonnelFullDto mockFullData = new com.bidb.personetakip.dto.ExternalPersonnelFullDto() {
            @Override public Long getEsicno() { return 12345L; }
            @Override public String getTckiml() { return testTcNo; }
            @Override public String getPeradi() { return "Ahmet"; }
            @Override public String getSoyadi() { return "Yılmaz"; }
            @Override public String getBrkodu() { return "BRK001"; }
            @Override public String getBrkdac() { return "Department Name"; }
            @Override public String getUnvkod() { return "UNV001"; }
            @Override public String getUnvack() { return "Title Name"; }
            @Override public String getTelefo() { return "05551234567"; }
        };
        
        when(externalPersonnelRepository.findCompletePersonnelData(testTcNo, 12345L))
            .thenReturn(Optional.of(mockFullData));
        
        // Act
        ExternalPersonnelDto result = registrationService.validatePersonnel(testTcNo, testPersonnelNo);
        
        // Assert
        assertNotNull(result);
        assertEquals("12345", result.personnelNo());
        assertEquals(testTcNo, result.tcNo());
        assertEquals("Ahmet", result.firstName());
        assertEquals("Yılmaz", result.lastName());
        
        verify(externalPersonnelRepository).findCompletePersonnelData(testTcNo, 12345L);
    }
    
    @Test
    void validatePersonnel_WithInvalidData_ShouldThrowPersonnelNotFoundException() {
        // Arrange
        when(externalPersonnelRepository.findCompletePersonnelData(testTcNo, 12345L))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(PersonnelNotFoundException.class, () -> {
            registrationService.validatePersonnel(testTcNo, testPersonnelNo);
        });
        
        verify(externalPersonnelRepository).findCompletePersonnelData(testTcNo, 12345L);
    }
    
    @Test
    void validatePersonnel_WithDatabaseError_ShouldThrowExternalServiceException() {
        // Arrange
        when(externalPersonnelRepository.findCompletePersonnelData(testTcNo, 12345L))
            .thenThrow(new RuntimeException("Database connection failed"));
        
        // Act & Assert
        assertThrows(ExternalServiceException.class, () -> {
            registrationService.validatePersonnel(testTcNo, testPersonnelNo);
        });
    }
    
    @Test
    void sendOtpVerification_ShouldGenerateAndSendOtp() {
        // Arrange
        String otpCode = "123456";
        String mobilePhone = "05551234567";
        when(smsService.generateOtp()).thenReturn(otpCode);
        when(otpVerificationRepository.save(any(OtpVerification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        registrationService.sendOtpVerification(testTcNo, mobilePhone);
        
        // Assert
        verify(smsService).generateOtp();
        verify(otpVerificationRepository).save(argThat(otp -> 
            otp.getTcNo().equals(testTcNo) &&
            otp.getOtpCode().equals(otpCode) &&
            !otp.getVerified() &&
            otp.getExpiresAt().isAfter(LocalDateTime.now())
        ));
        verify(smsService).sendSms(eq(mobilePhone), contains(otpCode));
    }
    
    @Test
    void verifyOtp_WithValidOtp_ShouldReturnTrue() {
        // Arrange
        String otpCode = "123456";
        OtpVerification otp = OtpVerification.builder()
            .tcNo(testTcNo)
            .otpCode(otpCode)
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .verified(false)
            .build();
        
        when(otpVerificationRepository.findByTcNoAndVerifiedFalse(testTcNo))
            .thenReturn(List.of(otp));
        when(otpVerificationRepository.save(any(OtpVerification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        boolean result = registrationService.verifyOtp(testTcNo, otpCode);
        
        // Assert
        assertTrue(result);
        verify(otpVerificationRepository).save(argThat(o -> o.getVerified()));
    }
    
    @Test
    void verifyOtp_WithInvalidOtp_ShouldThrowException() {
        // Arrange
        String otpCode = "123456";
        when(otpVerificationRepository.findByTcNoAndVerifiedFalse(testTcNo))
            .thenReturn(new ArrayList<>());
        
        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            registrationService.verifyOtp(testTcNo, otpCode);
        });
    }
    
    @Test
    void verifyOtp_WithExpiredOtp_ShouldThrowException() {
        // Arrange
        String otpCode = "123456";
        OtpVerification otp = OtpVerification.builder()
            .tcNo(testTcNo)
            .otpCode(otpCode)
            .expiresAt(LocalDateTime.now().minusMinutes(1))
            .verified(false)
            .build();
        
        when(otpVerificationRepository.findByTcNoAndVerifiedFalse(testTcNo))
            .thenReturn(List.of(otp));
        
        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            registrationService.verifyOtp(testTcNo, otpCode);
        });
    }
    
    @Test
    void verifyOtp_WithAlreadyVerifiedOtp_ShouldThrowException() {
        // Arrange
        String otpCode = "123456";
        // Already verified OTPs won't be returned by findByTcNoAndVerifiedFalse
        when(otpVerificationRepository.findByTcNoAndVerifiedFalse(testTcNo))
            .thenReturn(new ArrayList<>());
        
        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            registrationService.verifyOtp(testTcNo, otpCode);
        });
    }
    
    @Test
    void completeRegistration_WithValidData_ShouldCreateUser() {
        // Arrange
        String password = "Password123!";
        String hashedPassword = "$2a$12$hashedpassword";
        
        OtpVerification verifiedOtp = OtpVerification.builder()
            .tcNo(testTcNo)
            .otpCode("123456")
            .verified(true)
            .build();
        
        User savedUser = User.builder()
            .id(1L)
            .tcNo(testTcNo)
            .personnelNo(testPersonnelNo)
            .firstName("Ahmet")
            .lastName("Yılmaz")
            .mobilePhone("05551234567")
            .passwordHash(hashedPassword)
            .role(UserRole.NORMAL_USER)
            .build();
        
        com.bidb.personetakip.dto.ExternalPersonnelFullDto mockFullData = new com.bidb.personetakip.dto.ExternalPersonnelFullDto() {
            @Override public Long getEsicno() { return 12345L; }
            @Override public String getTckiml() { return testTcNo; }
            @Override public String getPeradi() { return "Ahmet"; }
            @Override public String getSoyadi() { return "Yılmaz"; }
            @Override public String getBrkodu() { return "BRK001"; }
            @Override public String getBrkdac() { return "Department Name"; }
            @Override public String getUnvkod() { return "UNV001"; }
            @Override public String getUnvack() { return "Title Name"; }
            @Override public String getTelefo() { return "05551234567"; }
        };
        
        when(otpVerificationRepository.findAll()).thenReturn(List.of(verifiedOtp));
        when(externalPersonnelRepository.findCompletePersonnelDataByTcNo(testTcNo)).thenReturn(Optional.of(mockFullData));
        when(passwordEncoder.encode(password)).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        // Act
        UserDto result = registrationService.completeRegistration(testTcNo, password);
        
        // Assert
        assertNotNull(result);
        assertEquals(savedUser.getId(), result.id());
        assertEquals(savedUser.getTcNo(), result.tcNo());
        assertEquals(savedUser.getRole(), result.role());
        
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void completeRegistration_WithExistingUser_ShouldThrowException() {
        // Arrange
        String password = "Password123!";
        
        // Mock OTP verification
        OtpVerification verifiedOtp = OtpVerification.builder()
            .tcNo(testTcNo)
            .otpCode("123456")
            .verified(true)
            .build();
        when(otpVerificationRepository.findAll()).thenReturn(List.of(verifiedOtp));
        
        // Mock external personnel data
        com.bidb.personetakip.dto.ExternalPersonnelFullDto mockFullData = new com.bidb.personetakip.dto.ExternalPersonnelFullDto() {
            @Override public Long getEsicno() { return 12345L; }
            @Override public String getTckiml() { return testTcNo; }
            @Override public String getPeradi() { return "Ahmet"; }
            @Override public String getSoyadi() { return "Yılmaz"; }
            @Override public String getBrkodu() { return "BRK001"; }
            @Override public String getBrkdac() { return "Department Name"; }
            @Override public String getUnvkod() { return "UNV001"; }
            @Override public String getUnvack() { return "Title Name"; }
            @Override public String getTelefo() { return "05551234567"; }
        };
        when(externalPersonnelRepository.findCompletePersonnelDataByTcNo(testTcNo))
            .thenReturn(Optional.of(mockFullData));
        
        // Mock existing user check
        when(userRepository.save(any(User.class)))
            .thenThrow(new RuntimeException("Duplicate entry"));
        when(passwordEncoder.encode(password)).thenReturn("$2a$12$hashedpassword");
        
        // Act & Assert - Should throw RuntimeException from save
        assertThrows(RuntimeException.class, () -> {
            registrationService.completeRegistration(testTcNo, password);
        });
    }
    
    @Test
    void completeRegistration_WithInvalidPassword_ShouldThrowException() {
        // Arrange
        String weakPassword = "weak";
        
        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            registrationService.completeRegistration(testTcNo, weakPassword);
        });
    }
    
    @Test
    void completeRegistration_WithoutVerifiedOtp_ShouldThrowException() {
        // Arrange
        String password = "Password123!";
        when(otpVerificationRepository.findAll()).thenReturn(new ArrayList<>());
        
        // Act & Assert
        assertThrows(OtpNotVerifiedException.class, () -> {
            registrationService.completeRegistration(testTcNo, password);
        });
    }
    
    @Test
    void cleanupExpiredOtps_ShouldDeleteExpiredRecords() {
        // Arrange
        when(otpVerificationRepository.deleteExpiredOtps(any(LocalDateTime.class)))
            .thenReturn(5);
        
        // Act
        int result = registrationService.cleanupExpiredOtps();
        
        // Assert
        assertEquals(5, result);
        verify(otpVerificationRepository).deleteExpiredOtps(any(LocalDateTime.class));
    }
}
