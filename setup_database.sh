#!/bin/bash

# Personnel Tracking System - Database Setup Script
# This script creates the local database and applies the schema

echo "Creating personnel_tracking database..."
mysql -u root -psahinbey -e "CREATE DATABASE IF NOT EXISTS personnel_tracking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

echo "Applying database schema..."
mysql -u root -psahinbey personnel_tracking < src/main/resources/db/schema.sql

echo "Database setup completed successfully!"
echo ""
echo "Verifying tables..."
mysql -u root -psahinbey personnel_tracking -e "SHOW TABLES;"
