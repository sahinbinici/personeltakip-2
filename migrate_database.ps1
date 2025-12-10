# Personnel Tracking System - Database Migration Script
# This script adds missing columns to the users table

Write-Host "Adding department_code, department_name and title_code columns to users table..." -ForegroundColor Green

# MySQL connection details
$mysqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
$username = "root"
$password = "sahinbey_"  # Empty password - update this with your actual MySQL root password
$database = "personnel_tracking"

# Check if MySQL is in PATH
$mysqlCommand = Get-Command mysql -ErrorAction SilentlyContinue

if ($mysqlCommand) {
    # MySQL is in PATH
    Write-Host "Using MySQL from PATH" -ForegroundColor Yellow

    # Add department_code column
    mysql -u $username -p$password $database -e "ALTER TABLE users ADD COLUMN IF NOT EXISTS department_code VARCHAR(10) AFTER mobile_phone;"

    # Add department_name column
    mysql -u $username -p$password $database -e "ALTER TABLE users ADD COLUMN IF NOT EXISTS department_name VARCHAR(200) AFTER department_code;"

    # Add title_code column
    mysql -u $username -p$password $database -e "ALTER TABLE users ADD COLUMN IF NOT EXISTS title_code VARCHAR(10) AFTER department_name;"

    Write-Host "`nVerifying changes..." -ForegroundColor Green
    mysql -u $username -p$password $database -e "DESCRIBE users;"

    Write-Host "`nMigration completed successfully!" -ForegroundColor Green
} elseif (Test-Path $mysqlPath) {
    # MySQL is at default location
    Write-Host "Using MySQL from default location" -ForegroundColor Yellow

    # Add department_code column
    & $mysqlPath -u $username -p$password $database -e "ALTER TABLE users ADD COLUMN IF NOT EXISTS department_code VARCHAR(10) AFTER mobile_phone;"

    # Add department_name column
    & $mysqlPath -u $username -p$password $database -e "ALTER TABLE users ADD COLUMN IF NOT EXISTS department_name VARCHAR(200) AFTER department_code;"

    # Add title_code column
    & $mysqlPath -u $username -p$password $database -e "ALTER TABLE users ADD COLUMN IF NOT EXISTS title_code VARCHAR(10) AFTER department_name;"

    Write-Host "`nVerifying changes..." -ForegroundColor Green
    & $mysqlPath -u $username -p$password $database -e "DESCRIBE users;"

    Write-Host "`nMigration completed successfully!" -ForegroundColor Green
} else {
    Write-Host "ERROR: MySQL not found!" -ForegroundColor Red
    Write-Host "Please run the following SQL commands manually in MySQL Workbench:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "ALTER TABLE personnel_tracking.users ADD COLUMN IF NOT EXISTS department_code VARCHAR(10) AFTER mobile_phone;" -ForegroundColor Cyan
    Write-Host "ALTER TABLE personnel_tracking.users ADD COLUMN IF NOT EXISTS department_name VARCHAR(200) AFTER department_code;" -ForegroundColor Cyan
    Write-Host "ALTER TABLE personnel_tracking.users ADD COLUMN IF NOT EXISTS title_code VARCHAR(10) AFTER department_name;" -ForegroundColor Cyan
    Write-Host "ALTER TABLE personnel_tracking.qr_codes ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 AFTER created_at;" -ForegroundColor Cyan
    Write-Host ""
}
