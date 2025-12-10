package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.ExternalPersonnelDto;
import com.bidb.personetakip.dto.UserDto;
import com.bidb.personetakip.exception.ExternalServiceException;
import com.bidb.personetakip.exception.OtpNotVerifiedException;
import com.bidb.personetakip.exception.PersonnelNotFoundException;
import com.bidb.personetakip.exception.ValidationException;
import com.bidb.personetakip.model.ExternalPersonnel;
import com.bidb.personetakip.model.User;
import com.bidb.personetakip.model.UserRole;
import com.bidb.personetakip.repository.OtpVerificationRepository;
import com.bidb.personetakip.repository.UserRepository;
import com.bidb.personetakip.repository.external.ExternalPersonnelRepository;
import com.bidb.personetakip.util.PasswordValidator;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

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
    @CircuitBreaker(name = "externalDb", fallbackMethod = "validatePersonnelFallback")
    @Retry(name = "externalDb")
    public ExternalPersonnelDto validatePersonnel(String tcNo, String personnelNo) {
        log.info("Validating personnel with TC No: {} and Personnel No: {}", tcNo, personnelNo);
        
        try {
            // Convert personnelNo to Long for database query
            Long personnelId;
            try {
                personnelId = Long.valueOf(personnelNo);
            } catch (NumberFormatException e) {
                log.warn("Invalid personnel number format: {}", personnelNo);
                throw new PersonnelNotFoundException("Invalid personnel number format: " + personnelNo);
            }
            
            // Query external database for complete personnel data with JOIN to get phone number
            log.info("Querying external database with native SQL (includes telefo JOIN)");
            Optional<com.bidb.personetakip.dto.ExternalPersonnelFullDto> fullDataOpt = 
                externalPersonnelRepository.findCompletePersonnelData(tcNo, personnelId);
            
            if (fullDataOpt.isEmpty()) {
                log.warn("Personnel not found in external database - TC No: {}, Personnel No: {}", tcNo, personnelNo);
                throw new PersonnelNotFoundException(
                    "Personnel not found with TC No: " + tcNo + " and Personnel No: " + personnelNo
                );
            }
            
            com.bidb.personetakip.dto.ExternalPersonnelFullDto fullData = fullDataOpt.get();
            
            // Log phone number from DB for debugging
            log.info("DB'den çekilen telefon numarası: {}", fullData.getTelefo());
            
            log.info("Personnel validated successfully: {} {} (Dept: {}, Title: {}, Phone: {})", 
                fullData.getPeradi(), fullData.getSoyadi(), 
                fullData.getBrkdac(), fullData.getUnvack(), fullData.getTelefo());
            
            // Map to ExternalPersonnelDto
            String phoneNumber = fullData.getTelefo() != null ? fullData.getTelefo() : "0000000000";
            log.info("Kullanılacak telefon numarası: {}", phoneNumber);
            
            return new ExternalPersonnelDto(
                fullData.getEsicno(),
                fullData.getTckiml(),
                personnelNo != null ? personnelNo : fullData.getEsicno().toString(),
                fullData.getPeradi(),
                fullData.getSoyadi(),
                phoneNumber,
                fullData.getBrkodu(),
                fullData.getBrkdac(),
                fullData.getUnvkod(),
                fullData.getUnvack()
            );
        } catch (PersonnelNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error connecting to external database", e);
            throw new ExternalServiceException("Failed to connect to external personnel database", e);
        }
    }
    
    // Fallback method for circuit breaker
    public ExternalPersonnelDto validatePersonnelFallback(String tcNo, String personnelNo, Exception ex) {
        log.warn("External database circuit breaker opened. Falling back to cached data or throwing exception.");
        throw new ExternalServiceException("External personnel database is temporarily unavailable. Please try again later.", ex);
    }
    
    @Override
    @Transactional
    public void sendOtpVerification(String tcNo, String mobilePhone) {
        log.info("Sending OTP verification to TC No: {}", tcNo);
        
        // Generate OTP
        String otpCode = smsService.generateOtp();
        
        // Save OTP verification record
        var otpVerification = new com.bidb.personetakip.model.OtpVerification();
        otpVerification.setTcNo(tcNo);
        otpVerification.setOtpCode(otpCode);
        otpVerification.setCreatedAt(LocalDateTime.now());
        otpVerification.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES));
        otpVerification.setVerified(false);
        
        otpVerificationRepository.save(otpVerification);
        
        // Send OTP via SMS
        String message = "Your verification code is: " + otpCode + ". Valid for 5 minutes.";
        smsService.sendSms(mobilePhone, message);
        
        log.info("OTP sent successfully to phone: {}", mobilePhone);
    }
    
    @Override
    @Transactional
    public boolean verifyOtp(String tcNo, String otpCode) {
        log.info("Verifying OTP for TC No: {}", tcNo);
        
        // Find non-expired, non-verified OTP records for this TC number
        var otpRecords = otpVerificationRepository.findByTcNoAndVerifiedFalse(tcNo);
        
        // Check each record for match and expiration
        for (var otpRecord : otpRecords) {
            if (otpRecord.getOtpCode().equals(otpCode)) {
                // Check if OTP is expired
                if (otpRecord.getExpiresAt().isBefore(LocalDateTime.now())) {
                    log.warn("OTP expired for TC No: {}", tcNo);
                    throw new ValidationException("OTP code has expired");
                }
                
                // Mark as verified
                otpRecord.setVerified(true);
                otpVerificationRepository.save(otpRecord);
                
                log.info("OTP verified successfully for TC No: {}", tcNo);
                return true;
            }
        }
        
        log.warn("Invalid OTP provided for TC No: {}", tcNo);
        throw new ValidationException("Invalid OTP code");
    }
    
    @Override
    @Transactional
    public UserDto completeRegistration(String tcNo, String password) {
        log.info("Completing registration for TC No: {}", tcNo);
        
        // Validate password strength
        var validationResult = PasswordValidator.validate(password);
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
        com.bidb.personetakip.dto.ExternalPersonnelFullDto fullData = externalPersonnelRepository
            .findCompletePersonnelDataByTcNo(tcNo)
            .orElseThrow(() -> new PersonnelNotFoundException("Personnel data not found for TC No: " + tcNo));
        
        var personnelOpt = externalPersonnelRepository.findByTcNo(tcNo);
        String personnelNoFromRepo = personnelOpt
            .map(ExternalPersonnel::getPersonnelNo)
            .orElse(null);
        String mobileFromRepo = personnelOpt
            .map(ExternalPersonnel::getMobilePhone)
            .orElse(null);
        
        // Hash password
        String passwordHash = passwordEncoder.encode(password);
        
        // Create user
        User user = User.builder()
            .tcNo(tcNo)
            .personnelNo(personnelNoFromRepo != null ? personnelNoFromRepo : fullData.getEsicno().toString())
            .firstName(fullData.getPeradi())
            .lastName(fullData.getSoyadi())
            .mobilePhone(mobileFromRepo != null ? mobileFromRepo : 
                       (fullData.getTelefo() != null ? fullData.getTelefo() : "0000000000"))
            .departmentCode(fullData.getBrkodu())
            .departmentName(fullData.getBrkdac())
            .titleCode(fullData.getUnvkod())
            .passwordHash(passwordHash)
            .role(UserRole.NORMAL_USER)
            .createdAt(LocalDateTime.now())
            .lastLoginAt(null)
            .build();
        
        User savedUser = userRepository.save(user);
        
        log.info("User registered successfully with ID: {}", savedUser.getId());
        
        return new UserDto(
            savedUser.getId(),
            savedUser.getTcNo(),
            savedUser.getPersonnelNo(),
            savedUser.getFirstName(),
            savedUser.getLastName(),
            savedUser.getMobilePhone(),
            savedUser.getDepartmentCode(),
            savedUser.getTitleCode(),
            savedUser.getRole()
        );
    }
    
    @Override
    @Transactional
    public int cleanupExpiredOtps() {
        log.info("Cleaning up expired OTPs");
        LocalDateTime now = LocalDateTime.now();
        int deletedCount = otpVerificationRepository.deleteExpiredOtps(now);
        log.info("Cleaned up {} expired OTPs", deletedCount);
        return deletedCount;
    }
}