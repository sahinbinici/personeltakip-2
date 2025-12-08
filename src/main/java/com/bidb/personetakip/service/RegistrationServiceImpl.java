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
import com.bidb.personetakip.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementation of RegistrationService for user registration operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationServiceImpl implements RegistrationService {
    
    private final ExternalPersonnelRepository externalPersonnelRepository;
    private final UserRepository userRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final SmsService smsService;
    private final PasswordEncoder passwordEncoder;
    
    private static final int OTP_EXPIRATION_MINUTES = 5;
    
    @Override
    @Transactional(readOnly = true, transactionManager = "externalTransactionManager")
    public ExternalPersonnelDto validatePersonnel(String tcNo, String personnelNo) {
        log.info("Validating personnel with TC No: {}", tcNo);
        
        try {
            ExternalPersonnel personnel = externalPersonnelRepository
                .findByTcNoAndPersonnelNo(tcNo, personnelNo)
                .orElseThrow(() -> new PersonnelNotFoundException(
                    "Personnel not found with TC No: " + tcNo + " and Personnel No: " + personnelNo
                ));
            
            log.info("Personnel validated successfully: {} {}", personnel.getFirstName(), personnel.getLastName());
            
            return new ExternalPersonnelDto(
                personnel.getUserId(),
                personnel.getTcNo(),
                personnel.getPersonnelNo(),
                personnel.getFirstName(),
                personnel.getLastName(),
                personnel.getMobilePhone()
            );
        } catch (PersonnelNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error connecting to external database", e);
            throw new ExternalServiceException("Failed to connect to external personnel database", e);
        }
    }
    
    @Override
    @Transactional
    public void sendOtpVerification(String tcNo, String mobilePhone) {
        log.info("Sending OTP verification to TC No: {}", tcNo);
        
        // Generate OTP
        String otpCode = smsService.generateOtp();
        
        // Calculate expiration time
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES);
        
        // Save OTP to database
        OtpVerification otpVerification = OtpVerification.builder()
            .tcNo(tcNo)
            .otpCode(otpCode)
            .expiresAt(expiresAt)
            .verified(false)
            .build();
        
        otpVerificationRepository.save(otpVerification);
        
        // Send SMS
        String message = "Your verification code is: " + otpCode + ". Valid for " + OTP_EXPIRATION_MINUTES + " minutes.";
        
        try {
            smsService.sendSms(mobilePhone, message);
            log.info("OTP sent successfully to {}", mobilePhone);
        } catch (SmsServiceException e) {
            log.error("Failed to send OTP SMS", e);
            throw e;
        }
    }
    
    @Override
    @Transactional
    public boolean verifyOtp(String tcNo, String otpCode) {
        log.info("Verifying OTP for TC No: {}", tcNo);
        
        OtpVerification otpVerification = otpVerificationRepository
            .findByTcNoAndOtpCode(tcNo, otpCode)
            .orElseThrow(() -> new OtpVerificationException("Invalid OTP code"));
        
        // Check if already verified
        if (otpVerification.getVerified()) {
            throw new OtpVerificationException("OTP has already been used");
        }
        
        // Check if expired
        if (otpVerification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new OtpVerificationException("OTP has expired");
        }
        
        // Mark as verified
        otpVerification.setVerified(true);
        otpVerificationRepository.save(otpVerification);
        
        log.info("OTP verified successfully for TC No: {}", tcNo);
        return true;
    }
    
    @Override
    @Transactional
    public UserDto completeRegistration(String tcNo, String password) {
        log.info("Completing registration for TC No: {}", tcNo);
        
        // Check if user already exists
        if (userRepository.existsByTcNo(tcNo)) {
            throw new UserAlreadyExistsException("User with TC No " + tcNo + " already exists");
        }
        
        // Validate password
        PasswordValidator.ValidationResult validationResult = PasswordValidator.validate(password);
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult.getErrorMessage());
        }
        
        // Check if OTP has been verified
        // Look for any verified OTP for this TC number
        boolean otpVerified = otpVerificationRepository.findAll().stream()
            .anyMatch(otp -> otp.getTcNo().equals(tcNo) && otp.getVerified());
        
        if (!otpVerified) {
            throw new OtpNotVerifiedException("OTP verification required before completing registration");
        }
        
        // Get personnel data from external database to populate user info
        ExternalPersonnel personnel = externalPersonnelRepository
            .findByTcNo(tcNo)
            .orElseThrow(() -> new PersonnelNotFoundException("Personnel data not found for TC No: " + tcNo));
        
        // Hash password
        String passwordHash = passwordEncoder.encode(password);
        
        // Create user
        User user = User.builder()
            .tcNo(tcNo)
            .personnelNo(personnel.getPersonnelNo())
            .firstName(personnel.getFirstName())
            .lastName(personnel.getLastName())
            .mobilePhone(personnel.getMobilePhone())
            .passwordHash(passwordHash)
            .role(UserRole.NORMAL_USER)
            .build();
        
        user = userRepository.save(user);
        
        log.info("User registration completed successfully for TC No: {}", tcNo);
        
        return new UserDto(
            user.getId(),
            user.getTcNo(),
            user.getPersonnelNo(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole().name()
        );
    }
    
    @Override
    @Transactional
    public int cleanupExpiredOtps() {
        log.info("Cleaning up expired OTPs");
        int deletedCount = otpVerificationRepository.deleteExpiredOtps(LocalDateTime.now());
        log.info("Deleted {} expired OTPs", deletedCount);
        return deletedCount;
    }
}
