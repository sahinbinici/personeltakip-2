package com.bidb.personetakip.controller;

import com.bidb.personetakip.dto.ExternalPersonnelDto;
import com.bidb.personetakip.dto.OtpVerificationRequest;
import com.bidb.personetakip.dto.PersonnelValidationRequest;
import com.bidb.personetakip.dto.RegistrationCompleteRequest;
import com.bidb.personetakip.dto.UserDto;
import com.bidb.personetakip.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for personnel registration operations.
 * Requirements: 1.1, 1.5, 2.1, 3.5
 */
@RestController
@RequestMapping("/api/register")
public class RegistrationController {
    
    private final RegistrationService registrationService;
    
    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }
    
    /**
     * Validates personnel against external database.
     * POST /api/register/validate
     * 
     * @param request Personnel validation request with TC No and Personnel No
     * @return ExternalPersonnelDto containing personnel data
     * Requirements: 1.1 - Query external database for matching personnel
     */
    @PostMapping("/validate")
    public ResponseEntity<ExternalPersonnelDto> validatePersonnel(
            @Valid @RequestBody PersonnelValidationRequest request) {
        ExternalPersonnelDto personnelDto = registrationService.validatePersonnel(
            request.tcNo(), 
            request.personnelNo()
        );
        return ResponseEntity.ok(personnelDto);
    }
    
    /**
     * Generates and sends OTP to mobile phone.
     * POST /api/register/send-otp
     * 
     * @param request Map containing tcNo and mobilePhone
     * @return Success message
     * Requirements: 1.5 - Generate and send OTP via SMS
     */
    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, String>> sendOtp(
            @RequestBody Map<String, String> request) {
        String tcNo = request.get("tcNo");
        String mobilePhone = request.get("mobilePhone");
        
        if (tcNo == null || tcNo.isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "TC No is required"));
        }
        if (mobilePhone == null || mobilePhone.isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Mobile phone is required"));
        }
        
        registrationService.sendOtpVerification(tcNo, mobilePhone);
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
    }
    
    /**
     * Verifies OTP code.
     * POST /api/register/verify-otp
     * 
     * @param request OTP verification request with TC No and OTP code
     * @return Success message
     * Requirements: 2.1 - Validate OTP code and expiration
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @Valid @RequestBody OtpVerificationRequest request) {
        boolean verified = registrationService.verifyOtp(
            request.tcNo(), 
            request.otpCode()
        );
        return ResponseEntity.ok(Map.of(
            "verified", verified,
            "message", "OTP verified successfully"
        ));
    }
    
    /**
     * Completes registration with password.
     * POST /api/register/complete
     * 
     * @param request Registration complete request with TC No and password
     * @return UserDto containing created user data
     * Requirements: 3.5 - Complete registration and assign NORMAL_USER role
     */
    @PostMapping("/complete")
    public ResponseEntity<UserDto> completeRegistration(
            @Valid @RequestBody RegistrationCompleteRequest request) {
        UserDto userDto = registrationService.completeRegistration(
            request.tcNo(), 
            request.password()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
    }
}
