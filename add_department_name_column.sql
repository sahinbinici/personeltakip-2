-- Add department_name column to users table
-- Run this SQL in MySQL Workbench

USE personnel_tracking;

-- Add department_name column after department_code
ALTER TABLE users ADD COLUMN department_name VARCHAR(200) AFTER department_code;

-- Verify the change
DESCRIBE users;

-- Show sample data to confirm structure
SELECT id, tc_no, first_name, last_name, department_code, department_name, title_code 
FROM users 
LIMIT 5;