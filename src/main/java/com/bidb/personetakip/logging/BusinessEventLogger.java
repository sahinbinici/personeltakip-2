package com.bidb.personetakip.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Utility for logging business events.
 * Logs registration, authentication, QR code generation, and entry/exit events.
 * 
 * Requirements: 1.1, 4.1, 5.1, 8.5, 9.2, 10.4
 */
@Component
public class BusinessEventLogger {
    
    private static final Logger logger = LoggerFactory.getLogger("BUSINESS_EVENTS");
    
    /**
     * Log registration attempts.
     * Requirement: 1.1 - Log registration attempts
     */
    public void logRegistrationAttempt(String tcNo, String personnelNo) {
        String maskedTcNo = maskTcNo(tcNo);
        
        MDC.put("event", "REGISTRATION_ATTEMPT");
        MDC.put("tcNo", maskedTcNo);
        MDC.put("personnelNo", personnelNo);
        
        logger.info("Registration attempt - TC: {}, Personnel No: {}", maskedTcNo, personnelNo);
        
        MDC.clear();
    }
    
    /**
     * Log successful registration completion.
     * Requirement: 1.1 - Log registration completions
     */
    public void logRegistrationSuccess(String tcNo) {
        String maskedTcNo = maskTcNo(tcNo);
        
        MDC.put("event", "REGISTRATION_SUCCESS");
        MDC.put("tcNo", maskedTcNo);
        
        logger.info("Registration completed successfully - TC: {}", maskedTcNo);
        
        MDC.clear();
    }
    
    /**
     * Log failed registration attempts.
     * Requirement: 1.1 - Log registration failures
     */
    public void logRegistrationFailure(String method, Exception exception) {
        MDC.put("event", "REGISTRATION_FAILURE");
        MDC.put("error", exception.getClass().getSimpleName());
        MDC.put("errorMessage", exception.getMessage());
        
        logger.warn("Registration failed - Method: {}, Error: {}", method, exception.getMessage());
        
        MDC.clear();
    }
    
    /**
     * Log authentication attempts.
     * Requirement: 4.1 - Log authentication attempts
     */
    public void logAuthenticationAttempt(String tcNo) {
        String maskedTcNo = maskTcNo(tcNo);
        
        MDC.put("event", "LOGIN_ATTEMPT");
        MDC.put("tcNo", maskedTcNo);
        
        logger.info("Login attempt - TC: {}", maskedTcNo);
        
        MDC.clear();
    }
    
    /**
     * Log successful authentication.
     * Requirement: 4.1 - Log successful logins
     */
    public void logAuthenticationSuccess(String tcNo) {
        String maskedTcNo = maskTcNo(tcNo);
        
        MDC.put("event", "LOGIN_SUCCESS");
        MDC.put("tcNo", maskedTcNo);
        
        logger.info("Login successful - TC: {}", maskedTcNo);
        
        MDC.clear();
    }
    
    /**
     * Log failed authentication attempts.
     * Requirement: 4.1 - Log failed logins
     */
    public void logAuthenticationFailure(String tcNo, Exception exception) {
        String maskedTcNo = maskTcNo(tcNo);
        
        MDC.put("event", "LOGIN_FAILURE");
        MDC.put("tcNo", maskedTcNo);
        MDC.put("error", exception.getClass().getSimpleName());
        
        logger.warn("Login failed - TC: {}, Error: {}", maskedTcNo, exception.getMessage());
        
        MDC.clear();
    }
    
    /**
     * Log QR code generation.
     * Requirement: 5.1 - Log QR code generation
     */
    public void logQrCodeGeneration(Long userId) {
        MDC.put("event", "QR_CODE_GENERATED");
        MDC.put("userId", String.valueOf(userId));
        
        logger.info("QR code generated - User ID: {}", userId);
        
        MDC.clear();
    }
    
    /**
     * Log entry/exit recordings.
     * Requirement: 8.5 - Log entry/exit recordings
     */
    public void logEntryExitRecording(Long userId, String qrCodeValue) {
        MDC.put("event", "ENTRY_EXIT_RECORDED");
        MDC.put("userId", String.valueOf(userId));
        MDC.put("qrCodeValue", maskQrCode(qrCodeValue));
        
        logger.info("Entry/Exit recorded - User ID: {}, QR Code: {}", userId, maskQrCode(qrCodeValue));
        
        MDC.clear();
    }
    
    /**
     * Log failed entry/exit attempts.
     * Requirement: 8.5 - Log failed entry/exit attempts
     */
    public void logEntryExitFailure(Long userId, Exception exception) {
        MDC.put("event", "ENTRY_EXIT_FAILURE");
        MDC.put("userId", String.valueOf(userId));
        MDC.put("error", exception.getClass().getSimpleName());
        
        logger.warn("Entry/Exit recording failed - User ID: {}, Error: {}", userId, exception.getMessage());
        
        MDC.clear();
    }
    
    /**
     * Mask TC number for privacy (show only first 3 and last 2 digits).
     * Requirement: 9.2 - Mask sensitive data
     */
    private String maskTcNo(String tcNo) {
        if (tcNo == null || tcNo.length() < 5) {
            return "***";
        }
        return tcNo.substring(0, 3) + "******" + tcNo.substring(tcNo.length() - 2);
    }
    
    /**
     * Mask QR code value (show only first 4 and last 4 characters).
     * Requirement: 9.2 - Mask sensitive data
     */
    private String maskQrCode(String qrCode) {
        if (qrCode == null || qrCode.length() < 8) {
            return "****";
        }
        return qrCode.substring(0, 4) + "..." + qrCode.substring(qrCode.length() - 4);
    }
}
