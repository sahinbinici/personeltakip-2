# Admin Panel Implementation Plan

- [x] 1. Set up admin database schema and audit logging

  - Create AdminAuditLog entity with JPA annotations
  - Add database migration for admin_audit_logs table
  - Create AdminAuditLogRepository interface
  - _Requirements: 4.3_

- [x] 1.1 Create AdminAuditLog entity

  - Define entity with id, adminUserId, action, targetUserId, details, timestamp fields
  - Add JPA annotations and table constraints
  - _Requirements: 4.3_

- [ ]* 1.2 Write property test for audit log creation
  - **Property 13: Audit Log Creation**
  - **Validates: Requirements 4.3**


- [x] 2. Implement admin role-based routing and access control
  - Update WebController to redirect admins to dashboard
  - Create AdminController for serving admin pages
  - Add role-based authorization checks
  - _Requirements: 1.1, 1.3, 4.1, 4.5_

- [x] 2.1 Update login routing for admin users
  - Modify login success logic to check user role
  - Redirect ADMIN/SUPER_ADMIN users to /admin/dashboard
  - Keep NORMAL_USER redirect to /qrcode
  - _Requirements: 1.1_

- [x] 2.2 Create AdminController for page routing
  - Create controller with /admin/dashboard, /admin/users, /admin/records endpoints
  - Add @PreAuthorize annotations for role checking
  - Implement basic page serving logic
  - _Requirements: 1.3, 4.1, 4.5_

- [ ]* 2.3 Write property test for admin routing
  - **Property 1: Admin Role-Based Routing**
  - **Validates: Requirements 1.1**


- [ ]* 2.4 Write property test for access control
  - **Property 3: Access Control Enforcement**
  - **Validates: Requirements 1.3, 4.1**

- [x] 3. Create admin dashboard functionality
  - Implement AdminDashboardService for statistics
  - Create AdminDashboardController API endpoints
  - Build dashboard HTML template with statistics display
  - _Requirements: 1.2, 1.4_

- [x] 3.1 Implement AdminDashboardService
  - Create service with methods for user count, today's activity count
  - Add method for recent entry/exit records summary
  - Implement caching for performance
  - _Requirements: 1.2_

- [x] 3.2 Create AdminDashboardController API
  - Create REST endpoints for dashboard statistics
  - Add JWT role validation for all endpoints
  - Return DashboardStatsDto with aggregated data
  - _Requirements: 1.2, 4.4_

- [x] 3.3 Build admin dashboard HTML template
  - Create admin-dashboard.html with statistics cards
  - Add navigation menu for admin modules
  - Implement auto-refresh functionality
  - Style with responsive CSS
  - _Requirements: 1.2, 1.4, 5.1_

- [ ]* 3.4 Write property test for dashboard statistics
  - **Property 2: Dashboard Statistics Accuracy**


  - **Validates: Requirements 1.2**

- [x] 4. Implement user management functionality
  - Create AdminUserService for user operations
  - Build AdminUserController with CRUD endpoints
  - Create user management HTML template
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

- [x] 4.1 Implement AdminUserService
  - Create service with paginated user listing
  - Add user search by TC, name, personnel number
  - Implement role filtering functionality
  - Add user role update with audit logging
  - _Requirements: 2.1, 2.4, 2.5, 2.6_

- [x] 4.2 Create AdminUserController API
  - Create REST endpoints for user listing with pagination
  - Add search and filter endpoints
  - Implement user role update endpoint
  - Add user detail view endpoint
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

- [x] 4.3 Build user management HTML template
  - Create admin-users.html with user table and pagination
  - Add search form and role filter dropdown
  - Implement user detail modal/page
  - Add role change functionality with confirmation
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

- [ ]* 4.4 Write property test for user pagination
  - **Property 4: User List Pagination**
  - **Validates: Requirements 2.1, 2.2**

- [ ]* 4.5 Write property test for role updates
  - **Property 5: Role Update Persistence**
  - **Validates: Requirements 2.4**

- [ ]* 4.6 Write property test for user search
  - **Property 6: User Search Functionality**
  - **Validates: Requirements 2.5**

- [ ]* 4.7 Write property test for role filtering
  - **Property 7: Role-Based User Filtering**
  - **Validates: Requirements 2.6**

- [x] 5. Implement entry/exit records management
  - Create AdminRecordsService for record operations
  - Build AdminRecordsController with query and export endpoints
  - Create records management HTML template
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [x] 5.1 Implement AdminRecordsService
  - Create service with paginated record listing
  - Add date range filtering functionality
  - Implement user-specific record filtering
  - Add CSV export functionality
  - Create daily summary statistics calculation
  - _Requirements: 3.1, 3.3, 3.4, 3.5, 3.6_

- [x] 5.2 Create AdminRecordsController API
  - Create REST endpoints for record listing with pagination
  - Add date range and user filter endpoints
  - Implement CSV export endpoint with proper headers
  - Add daily summary statistics endpoint
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [x] 5.3 Build records management HTML template
  - Create admin-records.html with record table and pagination
  - Add date range picker and user filter dropdown
  - Implement CSV export button
  - Add daily summary statistics display
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [ ]* 5.4 Write property test for record pagination
  - **Property 8: Record Pagination and Display**
  - **Validates: Requirements 3.1, 3.2**

- [ ]* 5.5 Write property test for date filtering
  - **Property 9: Date Range Filtering**
  - **Validates: Requirements 3.3**

- [ ]* 5.6 Write property test for user filtering
  - **Property 10: User-Specific Record Filtering**
  - **Validates: Requirements 3.4**

- [ ]* 5.7 Write property test for CSV export
  - **Property 11: CSV Export Data Integrity**
  - **Validates: Requirements 3.5**

- [ ]* 5.8 Write property test for statistics
  - **Property 12: Statistical Aggregation Accuracy**
  - **Validates: Requirements 3.6**

- [ ] 6. Implement security and validation
  - Add JWT role validation to all admin endpoints
  - Create security configuration for admin routes
  - Implement input validation and sanitization
  - _Requirements: 4.2, 4.4, 4.5_

- [ ] 6.1 Configure admin security
  - Update SecurityConfig to protect /admin/** routes
  - Add method-level security annotations
  - Configure CSRF protection for admin forms
  - _Requirements: 4.2, 4.4, 4.5_

- [ ] 6.2 Add input validation
  - Create validation annotations for admin DTOs
  - Add request validation in controllers
  - Implement proper error handling and responses
  - _Requirements: 4.2_

- [ ]* 6.3 Write property test for JWT validation
  - **Property 14: JWT Role Validation**
  - **Validates: Requirements 4.4**

- [ ]* 6.4 Write property test for URL access control
  - **Property 15: URL-Based Access Control**
  - **Validates: Requirements 4.5**

- [x] 7. Create responsive admin UI styling
  - Create admin-specific CSS with responsive design
  - Implement consistent styling with existing interface
  - Add mobile-friendly navigation and tables
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 7.1 Create admin CSS framework
  - Create admin.css with responsive grid system
  - Style admin navigation, cards, and tables
  - Implement mobile-first responsive design
  - Ensure consistency with existing QR code interface
  - _Requirements: 5.1, 5.3, 5.4_

- [x] 7.2 Add JavaScript functionality
  - Create admin.js for interactive features
  - Implement auto-refresh for dashboard
  - Add confirmation dialogs for destructive actions
  - Handle form submissions and API calls
  - _Requirements: 1.5, 5.2_

- [ ] 8. Checkpoint - Ensure all tests pass
  - Run all property-based tests and verify they pass
  - Test admin functionality end-to-end
  - Verify responsive design on different screen sizes
  - Ensure all tests pass, ask the user if questions arise

- [ ] 9. Integration and final testing
  - Test complete admin workflows
  - Verify audit logging functionality
  - Test CSV export with large datasets
  - Validate security and access control
  - _Requirements: All requirements_

- [ ] 9.1 End-to-end admin workflow testing
  - Test admin login → dashboard → user management → records
  - Verify all navigation and functionality works
  - Test role changes and audit logging
  - _Requirements: All requirements_

- [ ]* 9.2 Write integration tests for admin workflows
  - Create integration tests for complete admin user journeys
  - Test authentication, authorization, and functionality
  - Verify audit logging and data persistence
  - _Requirements: All requirements_

- [ ] 10. Final Checkpoint - Complete admin panel
  - Ensure all admin functionality is working
  - Verify responsive design and user experience
  - Confirm all security measures are in place
  - Ensure all tests pass, ask the user if questions arise