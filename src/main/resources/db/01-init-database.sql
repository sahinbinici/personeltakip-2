-- Personnel Tracking System - Database Initialization
-- This script creates the basic database structure

-- Create database if not exists (handled by Docker environment variables)
-- CREATE DATABASE IF NOT EXISTS personnel_tracking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE personnel_tracking;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tc_no VARCHAR(11) UNIQUE NOT NULL,
    phone_number VARCHAR(15) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    department_name VARCHAR(200),
    title VARCHAR(200),
    email VARCHAR(255),
    is_admin BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    assigned_ip_addresses TEXT NULL COMMENT 'Comma-separated list of assigned IP addresses',
    INDEX idx_tc_no (tc_no),
    INDEX idx_phone_number (phone_number),
    INDEX idx_department (department_name),
    INDEX idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create entry_exit_records table
CREATE TABLE IF NOT EXISTS entry_exit_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    entry_time TIMESTAMP NULL,
    exit_time TIMESTAMP NULL,
    date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ip_address VARCHAR(45) NULL COMMENT 'Client IP address (IPv4 or IPv6)',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_date (date),
    INDEX idx_entry_time (entry_time),
    INDEX idx_exit_time (exit_time),
    INDEX idx_ip_address (ip_address),
    UNIQUE KEY unique_user_date (user_id, date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    CONSTRAINT chk_ip_action CHECK (action IN ('VIEW', 'ASSIGN', 'REMOVE', 'MISMATCH', 'ACCESS'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default admin user (password will be set via application)
INSERT IGNORE INTO users (tc_no, phone_number, first_name, last_name, department_name, title, email, is_admin, is_active) 
VALUES ('12345678901', '05551234567', 'Admin', 'User', 'IT Department', 'System Administrator', 'admin@gaziantep.edu.tr', TRUE, TRUE);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_admin ON users(is_admin);
CREATE INDEX IF NOT EXISTS idx_users_active_admin ON users(is_active, is_admin);
CREATE INDEX IF NOT EXISTS idx_records_user_date ON entry_exit_records(user_id, date);
CREATE INDEX IF NOT EXISTS idx_records_date_range ON entry_exit_records(date, entry_time, exit_time);

-- Show table structures
SHOW TABLES;
DESCRIBE users;
DESCRIBE entry_exit_records;
DESCRIBE ip_address_logs;