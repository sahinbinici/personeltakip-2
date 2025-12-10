-- Migration: Add IP address tracking functionality
-- Date: 2025-12-10
-- Description: Adds IP address tracking columns and audit logging table

USE personnel_tracking;

-- Add ip_address column to entry_exit_records table
ALTER TABLE entry_exit_records 
ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45) NULL COMMENT 'Client IP address (IPv4 or IPv6)';

-- Add assigned_ip_addresses column to users table
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS assigned_ip_addresses TEXT NULL COMMENT 'Comma-separated list of assigned IP addresses';

-- Create ip_address_logs table for audit logging
CREATE TABLE IF NOT EXISTS ip_address_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ip_address VARCHAR(45),
    action VARCHAR(20) NOT NULL,
    admin_user_id BIGINT NULL,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details JSON NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_action (action),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (admin_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_ip_action CHECK (action IN ('VIEW', 'ASSIGN', 'REMOVE', 'MISMATCH', 'ACCESS'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add index on ip_address column in entry_exit_records for filtering performance
CREATE INDEX IF NOT EXISTS idx_ip_address ON entry_exit_records(ip_address);

-- Verify the changes
DESCRIBE entry_exit_records;
DESCRIBE users;
DESCRIBE ip_address_logs;