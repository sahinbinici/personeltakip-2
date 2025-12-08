package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.ExternalPersonnelDto;
import com.bidb.personetakip.dto.UserDto;

/**
 * Service interface for user registration operations.
 */
public interface RegistrationService {
    
    /**
     * Validates TC and Personnel No against external database
     * 
     * @param tcNo Turkish Citizen ID number
     * @param personnelNo Personnel/Employee number
     * @return ExternalPersonnelDto containing personnel data
     * @throws PersonnelNotFoundException if personnel not found in external database
     * @throws ExternalServiceException if external database connection fails
     */
    ExternalPersonnelDto validatePersonnel(String tcNo, String personnelNo);
    
    /**
     * Generates and sends OTP via SMS
     * 
     * @param tcNo Turkish Citizen ID number
     * @param mobilePhone Mobile phone number to send OTP
     * @throws SmsServiceException if SMS sending fails
     */
    void sendOtpVerification(String tcNo, String mobilePhone);
    
    /**
     * Verifies OTP code
     * 
     * @param tcNo Turkish Citizen ID number
     * @param otpCode OTP code to verify
     * @return true if OTP is valid and not expired
     * @throws OtpVerificationException if OTP is invalid or expired
     */
    boolean verifyOtp(String tcNo, String otpCode);
    
    /**
     * Completes registration with password
     * 
     * @param tcNo Turkish Citizen ID number
     * @param password User password
     * @return UserDto containing created user data
     * @throws ValidationException if password validation fails
     * @throws OtpNotVerifiedException if OTP has not been verified
     * @throws UserAlreadyExistsException if user already exists
     */
    UserDto completeRegistration(String tcNo, String password);
    
    /**
     * Cleans up expired OTP codes
     * 
     * @return Number of expired OTPs deleted
     */
    int cleanupExpiredOtps();
}
