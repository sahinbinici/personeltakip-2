# IP Tracking Feature - Deployment Notes

## Database Schema Changes Required

The IP tracking feature requires database schema changes that must be applied before deployment.

### Required Migrations

1. **IP Tracking Migration** (`src/main/resources/db/migration_add_ip_tracking.sql`)
   - Adds `ip_address` column to `entry_exit_records` table
   - Adds `assigned_ip_addresses` column to `users` table  
   - Creates `ip_address_logs` table for audit logging
   - Adds performance indexes

### Manual Migration Steps

#### For Development Environment:
```bash
# Run the migration script
mysql -u root -p personnel_tracking < src/main/resources/db/migration_add_ip_tracking.sql
```

#### For Production Environment:
```bash
# 1. Backup the database first
mysqldump -u [username] -p personnel_tracking > backup_$(date +%Y%m%d_%H%M%S).sql

# 2. Apply the migration
mysql -u [username] -p personnel_tracking < src/main/resources/db/migration_add_ip_tracking.sql

# 3. Verify the changes
mysql -u [username] -p personnel_tracking -e "DESCRIBE entry_exit_records;"
mysql -u [username] -p personnel_tracking -e "DESCRIBE users;"
mysql -u [username] -p personnel_tracking -e "DESCRIBE ip_address_logs;"
```

### Verification Commands

After applying the migration, verify the schema changes:

```sql
-- Check entry_exit_records table has ip_address column
DESCRIBE entry_exit_records;

-- Check users table has assigned_ip_addresses column  
DESCRIBE users;

-- Check ip_address_logs table exists
DESCRIBE ip_address_logs;

-- Verify indexes
SHOW INDEX FROM entry_exit_records WHERE Key_name = 'idx_ip_address';
SHOW INDEX FROM ip_address_logs;
```

### Configuration Updates

Ensure the following configuration properties are set:

#### application.properties
```properties
# IP Tracking Configuration
ip.tracking.enabled=true
ip.tracking.privacy.enabled=false
ip.tracking.anonymization.enabled=false
ip.tracking.audit.logging=true
```

### Performance Considerations

- The `ip_address` column in `entry_exit_records` is indexed for filtering performance
- The `ip_address_logs` table has indexes on `user_id`, `timestamp`, and `action` columns
- IP address storage uses VARCHAR(45) to support both IPv4 and IPv6 addresses

### Security Considerations

- IP addresses are stored securely with appropriate database constraints
- Audit logging tracks all IP address access and modifications
- Privacy settings can be configured to anonymize IP addresses in reports

### Rollback Plan

If rollback is needed:

```sql
-- Remove added columns (WARNING: This will lose data)
ALTER TABLE entry_exit_records DROP COLUMN ip_address;
ALTER TABLE users DROP COLUMN assigned_ip_addresses;

-- Drop audit table (WARNING: This will lose audit data)
DROP TABLE ip_address_logs;

-- Remove indexes
DROP INDEX idx_ip_address ON entry_exit_records;
```

### Testing Checklist

After deployment, verify:

- [ ] Application starts without database errors
- [ ] Login functionality works (users table accessible)
- [ ] Entry/exit operations work with IP tracking
- [ ] Admin panel shows IP address columns
- [ ] IP assignment functionality works in user management
- [ ] Audit logging creates entries in ip_address_logs table

### Known Issues

1. **Database Migration Required**: The application will fail to start if the database migration is not applied
2. **Column Missing Error**: Error "Unknown column 'u1_0.assigned_ip_addresses'" indicates migration not applied
3. **Performance Impact**: Initial deployment may have slight performance impact due to new indexes being built

### Support

For deployment issues, check:
1. Database connection and permissions
2. Migration script execution logs
3. Application startup logs for schema validation errors