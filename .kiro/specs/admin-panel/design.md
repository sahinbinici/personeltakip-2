# Admin Panel Design Document

## Overview

The admin panel is a web-based administrative interface that provides system administrators with comprehensive tools to manage the personnel tracking system. The panel consists of three main modules: Dashboard, User Management, and Entry/Exit Records. The design follows a responsive, modern web interface pattern that integrates seamlessly with the existing QR code system.

## Architecture

The admin panel follows a layered architecture pattern:

- **Presentation Layer**: Thymeleaf templates with responsive CSS and JavaScript
- **Controller Layer**: Spring MVC controllers handling admin-specific endpoints
- **Service Layer**: Business logic for admin operations, user management, and reporting
- **Data Access Layer**: JPA repositories for user and entry/exit record operations
- **Security Layer**: JWT-based authentication with role-based authorization

## Components and Interfaces

### Admin Web Controller
- **AdminController**: Serves admin HTML pages with role-based access control
- **Routes**: `/admin/dashboard`, `/admin/users`, `/admin/records`
- **Security**: Validates admin role before serving pages

### Admin API Controllers
- **AdminDashboardController**: Provides dashboard statistics and summary data
- **AdminUserController**: Handles user management operations (list, update, search)
- **AdminRecordsController**: Manages entry/exit record queries and exports

### Admin Services
- **AdminDashboardService**: Aggregates system statistics and recent activity
- **AdminUserService**: Provides user management operations with audit logging
- **AdminRecordsService**: Handles record queries, filtering, and CSV export

### Data Transfer Objects
- **DashboardStatsDto**: Contains user counts, today's activity, recent records
- **AdminUserDto**: Extended user information with last login and activity
- **RecordFilterDto**: Parameters for filtering entry/exit records
- **RecordExportDto**: Formatted record data for CSV export

## Data Models

### Existing Models (Extended)
- **User**: Add `lastLoginAt` field tracking (already exists)
- **EntryExitRecord**: Existing model used for record queries
- **UserRole**: Existing enum (NORMAL_USER, ADMIN, SUPER_ADMIN)

### New Models
- **AdminAuditLog**: Tracks administrative actions
  - `id`: Primary key
  - `adminUserId`: ID of admin performing action
  - `action`: Type of action performed
  - `targetUserId`: ID of affected user (if applicable)
  - `details`: JSON details of the action
  - `timestamp`: When action was performed

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Admin Role-Based Routing
*For any* user login, if the user has ADMIN or SUPER_ADMIN role, then the system should redirect to the admin dashboard instead of the QR code page
**Validates: Requirements 1.1**

### Property 2: Dashboard Statistics Accuracy
*For any* dashboard load, the displayed statistics should match the actual database counts for users and today's entry/exit records
**Validates: Requirements 1.2**

### Property 3: Access Control Enforcement
*For any* admin page access attempt, users with NORMAL_USER role should receive 403 Forbidden response
**Validates: Requirements 1.3, 4.1**

### Property 4: User List Pagination
*For any* user management page request, the system should return paginated results with correct user information including TC number, name, personnel number, role, and registration date
**Validates: Requirements 2.1, 2.2**

### Property 5: Role Update Persistence
*For any* user role change operation, the new role should be immediately persisted to database and an audit log entry should be created
**Validates: Requirements 2.4**

### Property 6: User Search Functionality
*For any* search query on TC number, name, or personnel number, the system should return only users matching the search criteria
**Validates: Requirements 2.5**

### Property 7: Role-Based User Filtering
*For any* role filter selection, the system should return only users with the specified role
**Validates: Requirements 2.6**

### Property 8: Record Pagination and Display
*For any* entry/exit records page request, the system should return paginated records with timestamp, user name, entry/exit type, and location information
**Validates: Requirements 3.1, 3.2**

### Property 9: Date Range Filtering
*For any* date range selection, the system should return only entry/exit records within the specified time period
**Validates: Requirements 3.3**

### Property 10: User-Specific Record Filtering
*For any* user selection in records view, the system should return only that user's entry/exit activities
**Validates: Requirements 3.4**

### Property 11: CSV Export Data Integrity
*For any* CSV export operation, the exported file should contain the same data as displayed in the filtered record list
**Validates: Requirements 3.5**

### Property 12: Statistical Aggregation Accuracy
*For any* date range selection, the daily summary statistics should accurately reflect the count and distribution of entry/exit activities
**Validates: Requirements 3.6**

### Property 13: Audit Log Creation
*For any* administrative action (role change, user update), an audit log entry should be created with correct admin user ID, action type, and timestamp
**Validates: Requirements 4.3**

### Property 14: JWT Role Validation
*For any* admin API request, the system should validate the JWT token contains ADMIN or SUPER_ADMIN role before processing the request
**Validates: Requirements 4.4**

### Property 15: URL-Based Access Control
*For any* direct admin URL access, the system should verify both authentication (valid JWT) and authorization (admin role) before serving content
**Validates: Requirements 4.5**

## Error Handling

- **Authentication Errors**: Redirect to login page with appropriate error message
- **Authorization Errors**: Return 403 Forbidden with clear error message
- **Data Validation Errors**: Return 400 Bad Request with field-specific error details
- **Server Errors**: Return 500 Internal Server Error with generic error message
- **Not Found Errors**: Return 404 Not Found for invalid user IDs or record IDs

## Testing Strategy

### Unit Testing
- Test admin service methods with mock data
- Verify role-based access control logic
- Test data aggregation and filtering functions
- Validate CSV export formatting

### Property-Based Testing
- Use JUnit 5 with QuickTheories library for property-based tests
- Configure each property test to run minimum 100 iterations
- Each property-based test must reference the corresponding correctness property
- Tag format: **Feature: admin-panel, Property {number}: {property_text}**

### Integration Testing
- Test complete admin workflows (login → dashboard → user management)
- Verify database operations and audit logging
- Test CSV export functionality end-to-end
- Validate responsive design on different screen sizes

## Security Considerations

- All admin endpoints require ADMIN or SUPER_ADMIN role
- JWT tokens validated on every request
- Audit logging for all administrative actions
- Input validation and sanitization for all user inputs
- CSRF protection for state-changing operations
- Rate limiting for admin API endpoints

## Performance Considerations

- Pagination for large user lists and record sets
- Database indexing on frequently queried fields
- Caching for dashboard statistics (30-second refresh)
- Lazy loading for user detail information
- Efficient CSV export for large datasets

## UI/UX Design

- Consistent styling with existing QR code interface
- Responsive design supporting desktop, tablet, and mobile
- Intuitive navigation with breadcrumbs
- Loading indicators for async operations
- Confirmation dialogs for destructive actions
- Accessible design following WCAG guidelines