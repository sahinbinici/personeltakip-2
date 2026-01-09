-- Migration: Add Department Permissions Table
-- This table stores which departments a DEPARTMENT_ADMIN user can manage

CREATE TABLE IF NOT EXISTS department_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    department_code VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT,
    
    -- Foreign key constraints
    CONSTRAINT fk_dept_perm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_dept_perm_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_dept_perm_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    
    -- Unique constraint to prevent duplicate permissions
    UNIQUE KEY uk_user_department (user_id, department_code),
    
    -- Index for performance
    INDEX idx_user_id (user_id),
    INDEX idx_department_code (department_code)
);

-- Insert sample data for testing
-- Give BIL department admin permission to manage BIL and IT departments
INSERT IGNORE INTO department_permissions (user_id, department_code, created_by) 
SELECT u.id, 'BIL', 1 
FROM users u 
WHERE u.tc_no = '11111111111' AND u.role = 'DEPARTMENT_ADMIN';

INSERT IGNORE INTO department_permissions (user_id, department_code, created_by) 
SELECT u.id, 'IT', 1 
FROM users u 
WHERE u.tc_no = '11111111111' AND u.role = 'DEPARTMENT_ADMIN';