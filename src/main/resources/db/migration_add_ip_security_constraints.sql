-- Migration: Add IP address security constraints and enhancements
-- Date: 2025-12-11
-- Description: Adds security constraints, indexes, and triggers for IP address data protection

USE personnel_tracking;

-- Add security constraints to ip_address columns
ALTER TABLE entry_exit_records 
MODIFY COLUMN ip_address VARCHAR(45) NULL 
COMMENT 'Client IP address (IPv4 or IPv6) - Security validated';

-- Add check constraint for IP address format validation (MySQL 8.0+)
-- Note: This is a basic constraint, application-level validation is more comprehensive
ALTER TABLE entry_exit_records 
ADD CONSTRAINT chk_ip_address_format 
CHECK (ip_address IS NULL OR 
       ip_address = 'Unknown' OR 
       ip_address REGEXP '^([0-9]{1,3}\.){3}[0-9]{1,3}$' OR 
       ip_address REGEXP '^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$');

-- Add security constraints to users assigned_ip_addresses
ALTER TABLE users 
MODIFY COLUMN assigned_ip_addresses TEXT NULL 
COMMENT 'Comma-separated list of assigned IP addresses - Security validated';

-- Enhance ip_address_logs table with additional security constraints
ALTER TABLE ip_address_logs 
MODIFY COLUMN ip_address VARCHAR(45) NULL 
COMMENT 'IP address involved in the logged action - Security monitored';

-- Add constraint for valid actions
ALTER TABLE ip_address_logs 
DROP CONSTRAINT IF EXISTS chk_ip_action;

ALTER TABLE ip_address_logs 
ADD CONSTRAINT chk_ip_action 
CHECK (action IN ('VIEW', 'ASSIGN', 'REMOVE', 'MISMATCH', 'ACCESS', 'MODIFY', 'DELETE'));

-- Add constraint for valid JSON in details column
ALTER TABLE ip_address_logs 
ADD CONSTRAINT chk_details_json 
CHECK (details IS NULL OR JSON_VALID(details));

-- Add security indexes for performance and monitoring
CREATE INDEX IF NOT EXISTS idx_ip_logs_security_monitoring 
ON ip_address_logs(user_id, action, timestamp);

CREATE INDEX IF NOT EXISTS idx_ip_logs_admin_activity 
ON ip_address_logs(admin_user_id, timestamp) 
WHERE admin_user_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_ip_logs_retention_cleanup 
ON ip_address_logs(timestamp, action);

-- Add index for IP address pattern searches (for security monitoring)
CREATE INDEX IF NOT EXISTS idx_entry_exit_ip_pattern 
ON entry_exit_records(ip_address, timestamp) 
WHERE ip_address IS NOT NULL AND ip_address != 'Unknown';

-- Create view for security monitoring (read-only access to sensitive data)
CREATE OR REPLACE VIEW ip_security_audit_view AS
SELECT 
    l.id,
    l.user_id,
    CASE 
        WHEN l.ip_address = 'Unknown' THEN 'Unknown'
        WHEN l.ip_address IS NULL THEN 'NULL'
        ELSE CONCAT(SUBSTRING(l.ip_address, 1, 
            CASE 
                WHEN l.ip_address LIKE '%.%.%.%' THEN LOCATE('.', l.ip_address, LOCATE('.', l.ip_address) + 1)
                ELSE LOCATE(':', l.ip_address, LOCATE(':', l.ip_address) + 1)
            END
        ), '***')
    END AS masked_ip_address,
    l.action,
    l.admin_user_id,
    l.timestamp,
    JSON_EXTRACT(l.details, '$.accessType') as access_type,
    JSON_EXTRACT(l.details, '$.securityLevel') as security_level
FROM ip_address_logs l
WHERE l.timestamp >= DATE_SUB(NOW(), INTERVAL 90 DAY);

-- Create stored procedure for secure IP data cleanup (retention policy)
DELIMITER //

CREATE OR REPLACE PROCEDURE CleanupIpDataRetention(IN retention_days INT)
BEGIN
    DECLARE cleanup_count INT DEFAULT 0;
    DECLARE cutoff_date DATETIME;
    
    -- Calculate cutoff date
    SET cutoff_date = DATE_SUB(NOW(), INTERVAL retention_days DAY);
    
    -- Start transaction for atomic cleanup
    START TRANSACTION;
    
    -- Count records to be deleted
    SELECT COUNT(*) INTO cleanup_count 
    FROM ip_address_logs 
    WHERE timestamp < cutoff_date;
    
    -- Delete old records if any exist
    IF cleanup_count > 0 THEN
        DELETE FROM ip_address_logs 
        WHERE timestamp < cutoff_date;
        
        -- Log the cleanup operation
        INSERT INTO ip_address_logs (user_id, ip_address, action, timestamp, details)
        VALUES (0, 'system', 'ACCESS', NOW(), 
                JSON_OBJECT('operation', 'retention_cleanup', 
                           'deletedRecords', cleanup_count,
                           'retentionDays', retention_days,
                           'cutoffDate', cutoff_date));
    END IF;
    
    COMMIT;
    
    -- Return cleanup count
    SELECT cleanup_count as records_cleaned;
END //

DELIMITER ;

-- Create function for IP address security validation
DELIMITER //

CREATE OR REPLACE FUNCTION ValidateIpAddressSecurity(ip_addr VARCHAR(45))
RETURNS BOOLEAN
READS SQL DATA
DETERMINISTIC
BEGIN
    -- Allow NULL and 'Unknown'
    IF ip_addr IS NULL OR ip_addr = 'Unknown' THEN
        RETURN TRUE;
    END IF;
    
    -- Check length constraints
    IF LENGTH(ip_addr) > 45 OR LENGTH(ip_addr) < 7 THEN
        RETURN FALSE;
    END IF;
    
    -- Check for malicious characters (basic check)
    IF ip_addr REGEXP '[<>"\''&;\\|`$(){}\\[\\]\\*\\?~#%\\^!@+=]' THEN
        RETURN FALSE;
    END IF;
    
    -- Check basic IP format (IPv4 or IPv6)
    IF ip_addr REGEXP '^([0-9]{1,3}\.){3}[0-9]{1,3}$' OR 
       ip_addr REGEXP '^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$' THEN
        RETURN TRUE;
    END IF;
    
    RETURN FALSE;
END //

DELIMITER ;

-- Create trigger for automatic IP address security validation on insert/update
DELIMITER //

CREATE OR REPLACE TRIGGER trg_entry_exit_ip_security_check
BEFORE INSERT ON entry_exit_records
FOR EACH ROW
BEGIN
    IF NEW.ip_address IS NOT NULL AND NOT ValidateIpAddressSecurity(NEW.ip_address) THEN
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'IP address failed security validation';
    END IF;
END //

CREATE OR REPLACE TRIGGER trg_entry_exit_ip_security_update
BEFORE UPDATE ON entry_exit_records
FOR EACH ROW
BEGIN
    IF NEW.ip_address IS NOT NULL AND NOT ValidateIpAddressSecurity(NEW.ip_address) THEN
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'IP address failed security validation';
    END IF;
END //

CREATE OR REPLACE TRIGGER trg_ip_logs_security_check
BEFORE INSERT ON ip_address_logs
FOR EACH ROW
BEGIN
    IF NEW.ip_address IS NOT NULL AND NEW.ip_address != 'system' AND NOT ValidateIpAddressSecurity(NEW.ip_address) THEN
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'IP address in audit log failed security validation';
    END IF;
END //

DELIMITER ;

-- Grant appropriate permissions for the security view
-- Note: In production, create specific roles with limited access
-- GRANT SELECT ON ip_security_audit_view TO 'security_auditor'@'%';

-- Verify the security enhancements
SELECT 'IP Security Constraints Applied Successfully' as status;

-- Show table constraints
SELECT 
    TABLE_NAME,
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
WHERE TABLE_SCHEMA = 'personnel_tracking' 
AND TABLE_NAME IN ('entry_exit_records', 'ip_address_logs', 'users')
AND CONSTRAINT_NAME LIKE '%ip%';