# Süper Admin Yetkileri Implementation Plan

- [ ] 1. Enhance data models for soft delete and audit tracking
  - Add `deletedAt` field to User and EntryExitRecord models
  - Add `modifiedAt` field to EntryExitRecord model
  - Create UserModificationHistory and RecordModificationHistory models
  - Update JPA repositories to support soft delete queries
  - _Requirements: 3.4, 3.5, 4.4, 4.5_

- [ ] 1.1 Write property test for soft delete functionality
  - **Property 9: Record Soft Delete and Logging**
  - **Validates: Requirements 2.4**

- [ ] 1.2 Write property test for referential integrity
  - **Property 15: Referential Integrity Maintenance**
  - **Validates: Requirements 3.5**

- [ ] 2. Create permission service for role-based authorization
  - Implement PermissionService with role checking methods
  - Add methods for Super Admin validation
  - Create permission checking utilities for UI and API
  - _Requirements: 1.5, 6.3_

- [ ] 2.1 Write property test for Super Admin role validation
  - **Property 5: Super Admin Role Validation**
  - **Validates: Requirements 1.5**

- [ ] 2.2 Write property test for dual-layer permission validation
  - **Property 28: Dual-Layer Permission Validation**
  - **Validates: Requirements 6.3**

- [ ] 3. Enhance AdminUserService for role management and user data operations
  - Add role change functionality restricted to Super Admin
  - Implement user data update methods with audit logging
  - Add soft delete functionality for users
  - Create user modification history tracking
  - _Requirements: 1.3, 3.3, 3.4, 4.5_

- [ ] 3.1 Write property test for role change persistence and logging
  - **Property 3: Role Change Persistence and Logging**
  - **Validates: Requirements 1.3**

- [ ] 3.2 Write property test for user data update validation and logging
  - **Property 13: User Data Update Validation and Logging**
  - **Validates: Requirements 3.3**

- [ ] 3.3 Write property test for user soft delete and data integrity
  - **Property 14: User Soft Delete and Data Integrity**
  - **Validates: Requirements 3.4**

- [ ] 4. Enhance AdminRecordsService for record management operations
  - Add record editing functionality for Super Admin
  - Implement soft delete for entry/exit records
  - Create record modification history tracking
  - Add validation for record updates
  - _Requirements: 2.3, 2.4, 4.4_

- [ ] 4.1 Write property test for record edit validation and logging
  - **Property 8: Record Edit Validation and Logging**
  - **Validates: Requirements 2.3**

- [ ] 4.2 Write property test for record modification audit detail
  - **Property 19: Record Modification Audit Detail**
  - **Validates: Requirements 4.4**

- [ ] 5. Create enhanced audit logging service
  - Implement AdminAuditService with detailed logging capabilities
  - Add role-based audit log categorization
  - Create field-level change tracking for user data
  - Add audit log viewing and filtering functionality
  - _Requirements: 4.1, 4.2, 4.3, 4.5_

- [ ] 5.1 Write property test for Super Admin audit logging
  - **Property 16: Super Admin Audit Logging**
  - **Validates: Requirements 4.1**

- [ ] 5.2 Write property test for role-based audit log categorization
  - **Property 17: Role-Based Audit Log Categorization**
  - **Validates: Requirements 4.2**

- [ ] 5.3 Write property test for role change audit detail
  - **Property 18: Role Change Audit Detail**
  - **Validates: Requirements 4.3**

- [ ] 6. Update AdminUserController with Super Admin endpoints
  - Add role change endpoints restricted to Super Admin
  - Implement user data update endpoints
  - Add user soft delete endpoints
  - Create permission validation for all new endpoints
  - _Requirements: 1.1, 1.4, 3.1, 3.2_

- [ ] 6.1 Write property test for Admin role change API restriction
  - **Property 4: Admin Role Change API Restriction**
  - **Validates: Requirements 1.4**

- [ ] 7. Update AdminRecordsController with Super Admin endpoints
  - Add record editing endpoints for Super Admin
  - Implement record soft delete endpoints
  - Add permission validation for record modification
  - Create API restrictions for Admin users
  - _Requirements: 2.1, 2.2, 2.5_

- [ ] 7.1 Write property test for Admin record modification API restriction
  - **Property 10: Admin Record Modification API Restriction**
  - **Validates: Requirements 2.5**

- [ ] 8. Create AdminAuditController for audit log management
  - Implement audit log viewing endpoints
  - Add filtering and search functionality for audit logs
  - Create role-based audit log access control
  - _Requirements: 4.2_

- [ ] 9. Update user management UI templates for role-based features
  - Add role change controls visible only to Super Admin
  - Implement user edit and delete buttons for Super Admin
  - Hide modification controls from Admin users
  - Add confirmation dialogs for destructive operations
  - _Requirements: 1.1, 1.2, 3.1, 3.2, 6.1, 6.2_

- [ ] 9.1 Write property test for Super Admin role change UI access
  - **Property 1: Super Admin Role Change UI Access**
  - **Validates: Requirements 1.1**

- [ ] 9.2 Write property test for Admin role change UI restriction
  - **Property 2: Admin Role Change UI Restriction**
  - **Validates: Requirements 1.2**

- [ ] 9.3 Write property test for Super Admin user management UI
  - **Property 11: Super Admin User Management UI**
  - **Validates: Requirements 3.1**

- [ ] 9.4 Write property test for Admin user view restriction
  - **Property 12: Admin User View Restriction**
  - **Validates: Requirements 3.2**

- [ ] 10. Update records management UI templates for Super Admin features
  - Add edit and delete buttons visible only to Super Admin
  - Implement record editing forms and validation
  - Hide modification controls from Admin users
  - Add audit trail display for record changes
  - _Requirements: 2.1, 2.2, 6.1, 6.2_

- [ ] 10.1 Write property test for Super Admin record management UI
  - **Property 6: Super Admin Record Management UI**
  - **Validates: Requirements 2.1**

- [ ] 10.2 Write property test for Admin record view restriction
  - **Property 7: Admin Record View Restriction**
  - **Validates: Requirements 2.2**

- [ ] 11. Update navigation and UI components for role-based access
  - Implement role-based navigation menu items
  - Add permission-based UI control hiding
  - Update error messages for permission denials
  - Create role-based UI element display logic
  - _Requirements: 6.1, 6.2, 6.4, 6.5_

- [ ] 11.1 Write property test for role-based UI element display
  - **Property 26: Role-Based UI Element Display**
  - **Validates: Requirements 6.1**

- [ ] 11.2 Write property test for permission-based UI control hiding
  - **Property 27: Permission-Based UI Control Hiding**
  - **Validates: Requirements 6.2**

- [ ] 11.3 Write property test for role-based navigation menu
  - **Property 30: Role-Based Navigation Menu**
  - **Validates: Requirements 6.5**

- [ ] 12. Ensure Admin functionality preservation
  - Verify all existing Admin features continue to work
  - Test dashboard access and statistics display for Admin users
  - Validate read-only access to users and records for Admin
  - Ensure search, filtering, and CSV export functionality for Admin
  - _Requirements: 5.1, 5.2, 5.3, 5.5_

- [ ] 12.1 Write property test for Admin dashboard access preservation
  - **Property 21: Admin Dashboard Access Preservation**
  - **Validates: Requirements 5.1**

- [ ] 12.2 Write property test for Admin read-only access preservation
  - **Property 22: Admin Read-Only Access Preservation**
  - **Validates: Requirements 5.2**

- [ ] 12.3 Write property test for Admin functionality preservation
  - **Property 23: Admin Functionality Preservation**
  - **Validates: Requirements 5.3**

- [ ] 12.4 Write property test for Admin search and export preservation
  - **Property 25: Admin Search and Export Preservation**
  - **Validates: Requirements 5.5**

- [ ] 13. Implement comprehensive error handling and validation
  - Add clear permission error messages
  - Implement input validation for all modification operations
  - Create user-friendly error responses for insufficient permissions
  - Add validation for soft delete operations
  - _Requirements: 5.4, 6.4_

- [ ] 13.1 Write property test for clear permission error messages
  - **Property 24: Clear Permission Error Messages**
  - **Validates: Requirements 5.4**

- [ ] 13.2 Write property test for clear permission error indication
  - **Property 29: Clear Permission Error Indication**
  - **Validates: Requirements 6.4**

- [ ] 14. Add security enhancements and rate limiting
  - Implement CSRF protection for all modification endpoints
  - Add rate limiting for Super Admin operations
  - Create input sanitization for all user inputs
  - Add logging for security events
  - _Requirements: All security-related requirements_

- [ ] 15. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 15.1 Write integration tests for complete Super Admin workflows
  - Test role change workflow from UI to database
  - Test record modification workflow with audit logging
  - Test user management workflow with soft delete
  - Verify Admin user restrictions across all features

- [ ] 16. Final validation and documentation
  - Verify all requirements are implemented and tested
  - Test UI responsiveness for new features
  - Validate audit logging completeness
  - Create deployment notes for database schema changes
  - _Requirements: All requirements validation_