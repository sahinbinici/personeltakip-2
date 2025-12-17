#!/bin/bash

# Personnel Tracking System - IP Tracking Migration Script for Docker
# This script adds IP tracking functionality to the MySQL container

echo "🗄️ Adding IP tracking functionality to the MySQL container..."

# Container name
MYSQL_CONTAINER="personeltakip-mysql"
DATABASE="personnel_tracking"

# Check if MySQL container is running
if ! docker ps | grep -q $MYSQL_CONTAINER; then
    echo "❌ MySQL container '$MYSQL_CONTAINER' is not running!"
    echo "Please start the containers first: docker-compose up -d"
    exit 1
fi

echo "✅ MySQL container is running"

# Wait for MySQL to be ready
echo "⏳ Waiting for MySQL to be ready..."
until docker exec $MYSQL_CONTAINER mysqladmin ping -h localhost --silent; do
    echo "Waiting for MySQL..."
    sleep 2
done

echo "✅ MySQL is ready"

# Add ip_address column to entry_exit_records
echo "📝 Adding ip_address column to entry_exit_records table..."
docker exec $MYSQL_CONTAINER mysql -u root -psahinbey_ $DATABASE -e "
ALTER TABLE entry_exit_records 
ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45) NULL 
COMMENT 'Client IP address (IPv4 or IPv6)';
"

# Add assigned_ip_addresses column to users
echo "📝 Adding assigned_ip_addresses column to users table..."
docker exec $MYSQL_CONTAINER mysql -u root -psahinbey_ $DATABASE -e "
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS assigned_ip_addresses TEXT NULL 
COMMENT 'Comma-separated list of assigned IP addresses';
"

# Create ip_address_logs table
echo "📝 Creating ip_address_logs table..."
docker exec $MYSQL_CONTAINER mysql -u root -psahinbey_ $DATABASE -e "
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
"

# Add index on ip_address column
echo "📝 Adding performance index..."
docker exec $MYSQL_CONTAINER mysql -u root -psahinbey_ $DATABASE -e "
CREATE INDEX IF NOT EXISTS idx_ip_address ON entry_exit_records(ip_address);
"

echo "✅ Verifying changes..."
echo "📋 Entry/Exit Records table structure:"
docker exec $MYSQL_CONTAINER mysql -u root -psahinbey_ $DATABASE -e "DESCRIBE entry_exit_records;"

echo ""
echo "📋 Users table structure:"
docker exec $MYSQL_CONTAINER mysql -u root -psahinbey_ $DATABASE -e "DESCRIBE users;"

echo ""
echo "📋 IP Address Logs table structure:"
docker exec $MYSQL_CONTAINER mysql -u root -psahinbey_ $DATABASE -e "DESCRIBE ip_address_logs;"

echo ""
echo "🎉 IP Tracking migration completed successfully!"
echo "📍 Database is ready for the Personnel Tracking application"