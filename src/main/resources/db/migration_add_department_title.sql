-- Migration: Add department_code and title_code columns to users table
-- Date: 2025-12-09

USE personnel_tracking;

-- Add department_code column if it doesn't exist
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS department_code VARCHAR(10) AFTER mobile_phone;

-- Add title_code column if it doesn't exist
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS title_code VARCHAR(10) AFTER department_code;

-- Verify the changes
DESCRIBE users;
