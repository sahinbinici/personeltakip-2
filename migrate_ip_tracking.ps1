# Personnel Tracking System - IP Tracking Migration Script
# This script adds IP tracking functionality to the database

Write-Host "Adding IP tracking functionality to the database..." -ForegroundColor Green

# MySQL connection details
$mysqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
$username = "root"
$password = "sahinbey_"  # Update this with your actual MySQL root password
$database = "personnel_tracking"

# Check if MySQL is in PATH
$mysqlCommand = Get-Command mysql -ErrorAction SilentlyContinue

if ($mysqlCommand) {
    # MySQL is in PATH
    Write-Host "Using MySQL from PATH" -ForegroundColor Yellow

    # Add ip_address column to entry_exit_records
    Write-Host "Adding ip_address column to entry_exit_records table..." -ForegroundColor Cyan
    mysql -u $username -p$password $database -e "ALTER TABLE entry_exit_records ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45) NULL COMMENT 'Client IP address (IPv4 or IPv6)';"

    # Add assigned_ip_addresses column to users
    Write-Host "Adding assigned_ip_addresses column to users table..." -ForegroundColor Cyan
    mysql -u $username -p$password $database -e "ALTER TABLE users ADD COLUMN IF NOT EXISTS assigned_ip_addresses TEXT NULL COMMENT 'Comma-separated list of assigned IP addresses';"

    # Create ip_address_logs table
    Write-Host "Creating ip_address_logs table..." -ForegroundColor Cyan
    mysql -u $username -p$password $database -e "CREATE TABLE IF NOT EXISTS ip_address_logs (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, ip_address VARCHAR(45), action VARCHAR(20) NOT NULL, admin_user_id BIGINT NULL, timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, details JSON NULL, INDEX idx_user_id (user_id), INDEX idx_timestamp (timestamp), INDEX idx_action (action), CONSTRAINT chk_ip_action CHECK (action IN ('VIEW', 'ASSIGN', 'REMOVE', 'MISMATCH', 'ACCESS'))) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;"

    # Add index on ip_address column
    Write-Host "Adding performance index..." -ForegroundColor Cyan
    mysql -u $username -p$password $database -e "CREATE INDEX IF NOT EXISTS idx_ip_address ON entry_exit_records(ip_address);"

    Write-Host "`nVerifying changes..." -ForegroundColor Green
    Write-Host "Entry/Exit Records table structure:" -ForegroundColor Yellow
    mysql -u $username -p$password $database -e "DESCRIBE entry_exit_records;"
    
    Write-Host "`nUsers table structure:" -ForegroundColor Yellow
    mysql -u $username -p$password $database -e "DESCRIBE users;"
    
    Write-Host "`nIP Address Logs table structure:" -ForegroundColor Yellow
    mysql -u $username -p$password $database -e "DESCRIBE ip_address_logs;"

    Write-Host "`nIP Tracking migration completed successfully!" -ForegroundColor Green
} elseif (Test-Path $mysqlPath) {
    # MySQL is at default location
    Write-Host "Using MySQL from default location" -ForegroundColor Yellow

    # Add ip_address column to entry_exit_records
    Write-Host "Adding ip_address column to entry_exit_records table..." -ForegroundColor Cyan
    & $mysqlPath -u $username -p$password $database -e "ALTER TABLE entry_exit_records ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45) NULL COMMENT 'Client IP address (IPv4 or IPv6)';"

    # Add assigned_ip_addresses column to users
    Write-Host "Adding assigned_ip_addresses column to users table..." -ForegroundColor Cyan
    & $mysqlPath -u $username -p$password $database -e "ALTER TABLE users ADD COLUMN IF NOT EXISTS assigned_ip_addresses TEXT NULL COMMENT 'Comma-separated list of assigned IP addresses';"

    # Create ip_address_logs table
    Write-Host "Creating ip_address_logs table..." -ForegroundColor Cyan
    & $mysqlPath -u $username -p$password $database -e "CREATE TABLE IF NOT EXISTS ip_address_logs (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, ip_address VARCHAR(45), action VARCHAR(20) NOT NULL, admin_user_id BIGINT NULL, timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, details JSON NULL, INDEX idx_user_id (user_id), INDEX idx_timestamp (timestamp), INDEX idx_action (action), CONSTRAINT chk_ip_action CHECK (action IN ('VIEW', 'ASSIGN', 'REMOVE', 'MISMATCH', 'ACCESS'))) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;"

    # Add index on ip_address column
    Write-Host "Adding performance index..." -ForegroundColor Cyan
    & $mysqlPath -u $username -p$password $database -e "CREATE INDEX IF NOT EXISTS idx_ip_address ON entry_exit_records(ip_address);"

    Write-Host "`nVerifying changes..." -ForegroundColor Green
    Write-Host "Entry/Exit Records table structure:" -ForegroundColor Yellow
    & $mysqlPath -u $username -p$password $database -e "DESCRIBE entry_exit_records;"
    
    Write-Host "`nUsers table structure:" -ForegroundColor Yellow
    & $mysqlPath -u $username -p$password $database -e "DESCRIBE users;"
    
    Write-Host "`nIP Address Logs table structure:" -ForegroundColor Yellow
    & $mysqlPath -u $username -p$password $database -e "DESCRIBE ip_address_logs;"

    Write-Host "`nIP Tracking migration completed successfully!" -ForegroundColor Green
} else {
    Write-Host "ERROR: MySQL not found!" -ForegroundColor Red
    Write-Host "Please run the following SQL commands manually in MySQL Workbench:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "-- Add IP address column to entry_exit_records" -ForegroundColor Cyan
    Write-Host "ALTER TABLE personnel_tracking.entry_exit_records ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45) NULL COMMENT 'Client IP address (IPv4 or IPv6)';" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "-- Add assigned IP addresses column to users" -ForegroundColor Cyan
    Write-Host "ALTER TABLE personnel_tracking.users ADD COLUMN IF NOT EXISTS assigned_ip_addresses TEXT NULL COMMENT 'Comma-separated list of assigned IP addresses';" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "-- Create IP address logs table" -ForegroundColor Cyan
    Write-Host "CREATE TABLE IF NOT EXISTS personnel_tracking.ip_address_logs (" -ForegroundColor Cyan
    Write-Host "    id BIGINT AUTO_INCREMENT PRIMARY KEY," -ForegroundColor Cyan
    Write-Host "    user_id BIGINT NOT NULL," -ForegroundColor Cyan
    Write-Host "    ip_address VARCHAR(45)," -ForegroundColor Cyan
    Write-Host "    action VARCHAR(20) NOT NULL," -ForegroundColor Cyan
    Write-Host "    admin_user_id BIGINT NULL," -ForegroundColor Cyan
    Write-Host "    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," -ForegroundColor Cyan
    Write-Host "    details JSON NULL," -ForegroundColor Cyan
    Write-Host "    INDEX idx_user_id (user_id)," -ForegroundColor Cyan
    Write-Host "    INDEX idx_timestamp (timestamp)," -ForegroundColor Cyan
    Write-Host "    INDEX idx_action (action)," -ForegroundColor Cyan
    Write-Host "    CONSTRAINT chk_ip_action CHECK (action IN ('VIEW', 'ASSIGN', 'REMOVE', 'MISMATCH', 'ACCESS'))" -ForegroundColor Cyan
    Write-Host ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "-- Add performance index" -ForegroundColor Cyan
    Write-Host "CREATE INDEX IF NOT EXISTS idx_ip_address ON personnel_tracking.entry_exit_records(ip_address);" -ForegroundColor Cyan
    Write-Host ""
}