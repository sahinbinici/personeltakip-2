-- Migration script to add excuse column to entry_exit_records table
-- Run this script on the production database

-- Add excuse column to entry_exit_records table
ALTER TABLE entry_exit_records 
ADD COLUMN IF NOT EXISTS excuse VARCHAR(500) NULL;

-- Add index for excuse column (optional, for filtering)
CREATE INDEX IF NOT EXISTS idx_excuse ON entry_exit_records(excuse);

-- Verify the column was added
DESCRIBE entry_exit_records;
