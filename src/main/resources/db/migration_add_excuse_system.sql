-- Migration script to add excuse system tables
-- Requirements: 3.1, 3.2, 3.3, 3.4, 3.5

-- Create excuses table
CREATE TABLE IF NOT EXISTS excuses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    excuse_type_id BIGINT NOT NULL,
    excuse_type_name VARCHAR(100) NOT NULL,
    description TEXT,
    excuse_date DATE NOT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    attachments TEXT COMMENT 'JSON array of attachment file paths',
    admin_notes TEXT,
    reviewed_at TIMESTAMP NULL,
    reviewed_by BIGINT NULL,
    
    -- Indexes for performance
    INDEX idx_excuses_user_id (user_id),
    INDEX idx_excuses_status (status),
    INDEX idx_excuses_excuse_date (excuse_date),
    INDEX idx_excuses_submitted_at (submitted_at),
    INDEX idx_excuses_user_date (user_id, excuse_date),
    INDEX idx_excuses_pending (status, submitted_at),
    
    -- Foreign key constraint (assuming users table exists)
    CONSTRAINT fk_excuses_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) 
        ON DELETE CASCADE ON UPDATE CASCADE,
        
    -- Constraint to prevent duplicate excuses for same user and date
    UNIQUE KEY uk_excuses_user_date (user_id, excuse_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Table for storing personnel excuse reports';

-- Insert sample excuse types data (if needed for testing)
-- Note: In production, excuse types might be managed through admin interface
-- or stored in a separate excuse_types table

-- Add some sample data for testing (optional)
-- INSERT INTO excuses (user_id, excuse_type_id, excuse_type_name, description, excuse_date, status)
-- VALUES 
--     (1, 1, 'Hastalık', 'Grip nedeniyle işe gelemiyorum', '2024-12-15', 'PENDING'),
--     (2, 2, 'Aile Acil Durumu', 'Aile bireyinde acil durum', '2024-12-14', 'APPROVED');

-- Create view for excuse statistics (optional)
CREATE OR REPLACE VIEW excuse_statistics AS
SELECT 
    DATE_FORMAT(excuse_date, '%Y-%m') as month_year,
    status,
    COUNT(*) as count,
    COUNT(DISTINCT user_id) as unique_users
FROM excuses 
GROUP BY DATE_FORMAT(excuse_date, '%Y-%m'), status
ORDER BY month_year DESC, status;

-- Create view for pending excuses with user information (optional)
CREATE OR REPLACE VIEW pending_excuses_with_users AS
SELECT 
    e.id,
    e.user_id,
    u.tc_no,
    CONCAT(u.first_name, ' ', u.last_name) as full_name,
    e.excuse_type_name,
    e.description,
    e.excuse_date,
    e.submitted_at,
    DATEDIFF(CURRENT_DATE, e.submitted_at) as days_pending
FROM excuses e
JOIN users u ON e.user_id = u.id
WHERE e.status = 'PENDING'
ORDER BY e.submitted_at ASC;

-- Add comments for documentation
ALTER TABLE excuses 
    MODIFY COLUMN user_id BIGINT NOT NULL COMMENT 'Reference to users table',
    MODIFY COLUMN excuse_type_id BIGINT NOT NULL COMMENT 'ID of the excuse type (1=Hastalık, 2=Aile Acil Durumu, etc.)',
    MODIFY COLUMN excuse_type_name VARCHAR(100) NOT NULL COMMENT 'Name of the excuse type for display',
    MODIFY COLUMN description TEXT COMMENT 'Detailed description of the excuse (required for some types)',
    MODIFY COLUMN excuse_date DATE NOT NULL COMMENT 'Date for which the excuse is being reported',
    MODIFY COLUMN submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When the excuse was submitted',
    MODIFY COLUMN status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT 'Current status of the excuse',
    MODIFY COLUMN attachments TEXT COMMENT 'JSON array of attachment file paths or URLs',
    MODIFY COLUMN admin_notes TEXT COMMENT 'Notes added by admin during review',
    MODIFY COLUMN reviewed_at TIMESTAMP NULL COMMENT 'When the excuse was reviewed by admin',
    MODIFY COLUMN reviewed_by BIGINT NULL COMMENT 'ID of admin who reviewed the excuse';

-- Grant necessary permissions (adjust as needed for your setup)
-- GRANT SELECT, INSERT, UPDATE ON excuses TO 'app_user'@'%';
-- GRANT SELECT ON excuse_statistics TO 'app_user'@'%';
-- GRANT SELECT ON pending_excuses_with_users TO 'app_user'@'%';