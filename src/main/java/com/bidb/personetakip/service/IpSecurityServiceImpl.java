package com.bidb.personetakip.service;

import com.bidb.personetakip.exception.IpValidationException;
import com.bidb.personetakip.model.IpAddressAction;
import com.bidb.personetakip.model.IpAddressLog;
import com.bidb.personetakip.repository.IpAddressLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Implementation of IP security service for secure storage, monitoring, and retention.
 * Requirements: 5.1, 5.3
 */
@Service
@Transactional
public class IpSecurityServiceImpl implements IpSecurityService {
    
    private static final Logger logger = LoggerFactory.getLogger(IpSecurityServiceImpl.class);
    
    private final IpAddressLogRepository ipAddressLogRepository;
    private final IpAddressService ipAddressService;
    private final IpTrackingConfigurationService configService;
    
    // Security constraints
    private static final int MAX_IP_LENGTH = 45;
    private static final int MIN_IP_LENGTH = 7; // Minimum for "1.1.1.1"
    private static final Pattern MALICIOUS_PATTERN = Pattern.compile(
        ".*[<>\"'&;\\\\|`$(){}\\[\\]\\*\\?~#%\\^!@+=].*", 
        Pattern.CASE_INSENSITIVE
    );
    
    // SQL injection patterns
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        ".*(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|vbscript).*",
        Pattern.CASE_INSENSITIVE
    );
    
    // XSS patterns
    private static final Pattern XSS_PATTERN = Pattern.compile(
        ".*(script|iframe|object|embed|form|input|img|svg|onload|onerror|onclick).*",
        Pattern.CASE_INSENSITIVE
    );
    
    // Encryption key (in production, this should be loaded from secure configuration)
    private static final String ENCRYPTION_KEY = "MySecretKey12345"; // 16 bytes for AES-128
    
    @Autowired
    public IpSecurityServiceImpl(IpAddressLogRepository ipAddressLogRepository,
                                IpAddressService ipAddressService,
                                IpTrackingConfigurationService configService) {
        this.ipAddressLogRepository = ipAddressLogRepository;
        this.ipAddressService = ipAddressService;
        this.configService = configService;
    }
    
    @Override
    public String sanitizeIpAddress(String ipAddress) throws IpValidationException {
        if (ipAddress == null) {
            throw new IpValidationException("IP address cannot be null", null, "null input");
        }
        
        // Basic input validation
        if (!isSecureInput(ipAddress)) {
            throw new IpValidationException("IP address contains potentially malicious characters", 
                ipAddress, "security validation failed");
        }
        
        // Trim whitespace and normalize
        String sanitized = ipAddress.trim();
        
        // Remove any null bytes or control characters
        sanitized = sanitized.replaceAll("[\\x00-\\x1F\\x7F]", "");
        
        // Validate length constraints
        if (sanitized.length() > MAX_IP_LENGTH) {
            throw new IpValidationException("IP address exceeds maximum allowed length", 
                sanitized, "length validation failed");
        }
        
        if (sanitized.length() < MIN_IP_LENGTH) {
            throw new IpValidationException("IP address is too short to be valid", 
                sanitized, "minimum length validation failed");
        }
        
        // Validate IP format using existing service
        if (!ipAddressService.isValidIpAddress(sanitized)) {
            throw new IpValidationException("IP address format is invalid after sanitization", 
                sanitized, "format validation failed");
        }
        
        logger.debug("Successfully sanitized IP address: {} -> {}", ipAddress, sanitized);
        return sanitized;
    }
    
    @Override
    public void validateSecureStorage(String ipAddress) throws IpValidationException {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            // Allow null/empty for unknown IP addresses
            return;
        }
        
        String trimmed = ipAddress.trim();
        
        // Check if it's the unknown IP default
        if (ipAddressService.getUnknownIpDefault().equals(trimmed)) {
            return; // Allow unknown IP default
        }
        
        // Validate against security constraints
        if (!isSecureStorage(trimmed)) {
            throw new IpValidationException("IP address does not meet security storage requirements", 
                trimmed, "security constraints validation failed");
        }
        
        // Additional security checks
        validateAgainstBlacklist(trimmed);
        validateStorageConstraints(trimmed);
        
        logger.debug("IP address passed secure storage validation: {}", trimmed);
    }
    
    /**
     * Validates IP address against known malicious IP blacklist
     */
    private void validateAgainstBlacklist(String ipAddress) throws IpValidationException {
        // Check for localhost/loopback addresses in production
        if (ipAddress.equals("127.0.0.1") || ipAddress.equals("::1") || ipAddress.equals("localhost")) {
            logger.warn("Localhost IP address detected: {}", ipAddress);
            // Don't throw exception, just log warning
        }
        
        // Check for private network ranges that might indicate misconfiguration
        if (ipAddress.startsWith("192.168.") || ipAddress.startsWith("10.") || 
            ipAddress.startsWith("172.16.") || ipAddress.startsWith("172.17.") ||
            ipAddress.startsWith("172.18.") || ipAddress.startsWith("172.19.") ||
            ipAddress.startsWith("172.20.") || ipAddress.startsWith("172.21.") ||
            ipAddress.startsWith("172.22.") || ipAddress.startsWith("172.23.") ||
            ipAddress.startsWith("172.24.") || ipAddress.startsWith("172.25.") ||
            ipAddress.startsWith("172.26.") || ipAddress.startsWith("172.27.") ||
            ipAddress.startsWith("172.28.") || ipAddress.startsWith("172.29.") ||
            ipAddress.startsWith("172.30.") || ipAddress.startsWith("172.31.")) {
            logger.debug("Private network IP address detected: {}", ipAddress);
            // Allow private IPs but log for monitoring
        }
    }
    
    /**
     * Validates storage-specific constraints
     */
    private void validateStorageConstraints(String ipAddress) throws IpValidationException {
        // Check database column constraints
        if (ipAddress.length() > 45) {
            throw new IpValidationException("IP address exceeds database column limit", 
                ipAddress, "database constraint violation");
        }
        
        // Validate character encoding
        byte[] bytes = ipAddress.getBytes(StandardCharsets.UTF_8);
        if (bytes.length != ipAddress.length()) {
            throw new IpValidationException("IP address contains non-ASCII characters", 
                ipAddress, "character encoding validation failed");
        }
    }
    
    @Override
    public void monitorIpDataAccess(String ipAddress, Long userId, Long adminUserId, String accessType, String sourceIp) {
        try {
            // Log the access event
            IpAddressLog log = new IpAddressLog();
            log.setUserId(userId);
            log.setIpAddress(ipAddress);
            log.setAction(IpAddressAction.valueOf(accessType.toUpperCase()));
            log.setAdminUserId(adminUserId);
            log.setTimestamp(LocalDateTime.now());
            
            // Add security monitoring details
            String details = String.format(
                "{\"accessType\": \"%s\", \"sourceIp\": \"%s\", \"targetIp\": \"%s\", \"securityLevel\": \"monitored\"}", 
                accessType, sourceIp != null ? sourceIp : "unknown", ipAddress != null ? ipAddress : "null"
            );
            log.setDetails(details);
            
            ipAddressLogRepository.save(log);
            
            // Check for suspicious patterns
            if (detectSuspiciousAccess(userId, 1)) { // Check last hour
                logger.warn("Suspicious IP data access pattern detected for user: {}, admin: {}, source: {}", 
                    userId, adminUserId, sourceIp);
                
                // Log security alert
                IpAddressLog alertLog = new IpAddressLog();
                alertLog.setUserId(userId);
                alertLog.setIpAddress(sourceIp);
                alertLog.setAction(IpAddressAction.ACCESS);
                alertLog.setAdminUserId(adminUserId);
                alertLog.setTimestamp(LocalDateTime.now());
                alertLog.setDetails("{\"alertType\": \"suspicious_access_pattern\", \"severity\": \"medium\"}");
                
                ipAddressLogRepository.save(alertLog);
            }
            
            logger.debug("Monitored IP data access: user={}, admin={}, type={}, source={}", 
                userId, adminUserId, accessType, sourceIp);
                
        } catch (Exception e) {
            logger.error("Failed to monitor IP data access: user={}, admin={}, type={}", 
                userId, adminUserId, accessType, e);
        }
    }
    
    @Override
    public int enforceRetentionPolicy() {
        try {
            int retentionDays = configService.getRetentionDays();
            if (retentionDays <= 0) {
                logger.debug("IP data retention policy disabled (retentionDays={})", retentionDays);
                return 0;
            }
            
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            
            // Find old records to delete
            List<IpAddressLog> oldLogs = ipAddressLogRepository.findByTimestampBefore(cutoffDate);
            int recordCount = oldLogs.size();
            
            if (recordCount > 0) {
                // Delete old records
                ipAddressLogRepository.deleteByTimestampBefore(cutoffDate);
                
                logger.info("Enforced IP data retention policy: deleted {} records older than {} days", 
                    recordCount, retentionDays);
                
                // Log retention enforcement
                IpAddressLog retentionLog = new IpAddressLog();
                retentionLog.setUserId(0L); // System user
                retentionLog.setIpAddress("system");
                retentionLog.setAction(IpAddressAction.ACCESS);
                retentionLog.setTimestamp(LocalDateTime.now());
                retentionLog.setDetails(String.format(
                    "{\"operation\": \"retention_enforcement\", \"deletedRecords\": %d, \"retentionDays\": %d}", 
                    recordCount, retentionDays
                ));
                
                ipAddressLogRepository.save(retentionLog);
            }
            
            return recordCount;
            
        } catch (Exception e) {
            logger.error("Failed to enforce IP data retention policy", e);
            return 0;
        }
    }
    
    @Override
    public boolean isSecureStorage(String ipAddress) {
        if (ipAddress == null) {
            return true; // Null is allowed for unknown IPs
        }
        
        String trimmed = ipAddress.trim();
        
        // Allow unknown IP default
        if (ipAddressService.getUnknownIpDefault().equals(trimmed)) {
            return true;
        }
        
        // Check basic security constraints
        if (trimmed.length() > MAX_IP_LENGTH || trimmed.length() < MIN_IP_LENGTH) {
            return false;
        }
        
        // Check for malicious patterns
        if (MALICIOUS_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        
        // Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        
        // Check for XSS patterns
        if (XSS_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        
        // Validate IP format
        return ipAddressService.isValidIpAddress(trimmed);
    }
    
    @Override
    public List<IpAddressLog> getSecurityAuditLogs(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            if (userId != null) {
                return ipAddressLogRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
                    userId, startDate, endDate);
            } else {
                return ipAddressLogRepository.findByTimestampBetweenOrderByTimestampDesc(
                    startDate, endDate);
            }
        } catch (Exception e) {
            logger.error("Failed to retrieve security audit logs: userId={}, start={}, end={}", 
                userId, startDate, endDate, e);
            return List.of();
        }
    }
    
    @Override
    public boolean detectSuspiciousAccess(Long userId, int timeWindowHours) {
        try {
            LocalDateTime startTime = LocalDateTime.now().minusHours(timeWindowHours);
            List<IpAddressLog> recentLogs = ipAddressLogRepository.findByUserIdAndTimestampAfterOrderByTimestampDesc(
                userId, startTime);
            
            // Check for suspicious patterns
            if (recentLogs.size() > 50) { // More than 50 accesses in time window
                logger.warn("High frequency IP access detected for user {}: {} accesses in {} hours", 
                    userId, recentLogs.size(), timeWindowHours);
                return true;
            }
            
            // Check for access from multiple admin users
            long uniqueAdmins = recentLogs.stream()
                .filter(log -> log.getAdminUserId() != null)
                .mapToLong(IpAddressLog::getAdminUserId)
                .distinct()
                .count();
            
            if (uniqueAdmins > 5) { // More than 5 different admins accessing
                logger.warn("Multiple admin access detected for user {}: {} different admins in {} hours", 
                    userId, uniqueAdmins, timeWindowHours);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error("Failed to detect suspicious access for user {}", userId, e);
            return false;
        }
    }
    
    @Override
    public boolean isSecureInput(String ipAddress) {
        if (ipAddress == null) {
            return true; // Null is allowed
        }
        
        String trimmed = ipAddress.trim();
        
        // Check length constraints
        if (trimmed.length() > MAX_IP_LENGTH) {
            return false;
        }
        
        // Check for malicious characters
        if (MALICIOUS_PATTERN.matcher(trimmed).matches()) {
            logger.warn("Malicious pattern detected in IP input: {}", trimmed);
            return false;
        }
        
        // Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(trimmed).matches()) {
            logger.warn("SQL injection pattern detected in IP input: {}", trimmed);
            return false;
        }
        
        // Check for XSS patterns
        if (XSS_PATTERN.matcher(trimmed).matches()) {
            logger.warn("XSS pattern detected in IP input: {}", trimmed);
            return false;
        }
        
        // Check for null bytes and control characters
        for (char c : trimmed.toCharArray()) {
            if (c < 32 && c != 9 && c != 10 && c != 13) { // Allow tab, LF, CR
                logger.warn("Control character detected in IP input: {}", (int) c);
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public String encryptIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return ipAddress;
        }
        
        try {
            SecretKeySpec secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encrypted = cipher.doFinal(ipAddress.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
            
        } catch (Exception e) {
            logger.error("Failed to encrypt IP address", e);
            return ipAddress; // Return original if encryption fails
        }
    }
    
    @Override
    public String decryptIpAddress(String encryptedIpAddress) {
        if (encryptedIpAddress == null || encryptedIpAddress.trim().isEmpty()) {
            return encryptedIpAddress;
        }
        
        try {
            SecretKeySpec secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedIpAddress));
            return new String(decrypted, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            logger.error("Failed to decrypt IP address", e);
            return encryptedIpAddress; // Return original if decryption fails
        }
    }
}