# Süper Admin Yetkileri Design Document

## Overview

Bu tasarım, mevcut admin panel sistemini genişleterek Süper Admin rolüne özel yetkiler sağlar. Süper Admin kullanıcıları, rol yönetimi, giriş/çıkış kayıtlarını düzenleme ve kullanıcı verilerini yönetme konularında tam yetkiye sahip olacaktır. Admin kullanıcıları ise mevcut okuma yetkilerini koruyacak ancak veri değiştirme yetkilerine sahip olmayacaktır.

## Architecture

Mevcut admin panel mimarisini genişleten katmanlı yapı:

- **Presentation Layer**: Rol tabanlı UI elementleri ile genişletilmiş Thymeleaf şablonları
- **Controller Layer**: Süper Admin özel endpoint'leri ile genişletilmiş Spring MVC kontrolcüleri
- **Service Layer**: Rol tabanlı yetkilendirme ve gelişmiş audit logging ile genişletilmiş iş mantığı
- **Data Access Layer**: Soft delete ve audit logging desteği ile genişletilmiş JPA repository'leri
- **Security Layer**: Granüler rol tabanlı yetkilendirme ile genişletilmiş JWT güvenlik

## Components and Interfaces

### Enhanced Admin Controllers
- **AdminUserController**: Süper Admin için rol değiştirme ve kullanıcı düzenleme endpoint'leri
- **AdminRecordsController**: Süper Admin için kayıt düzenleme ve silme endpoint'leri
- **AdminAuditController**: Audit log görüntüleme ve filtreleme endpoint'leri

### Enhanced Admin Services
- **AdminUserService**: Rol yönetimi ve kullanıcı veri yönetimi işlevleri
- **AdminRecordsService**: Kayıt düzenleme ve soft delete işlevleri
- **AdminAuditService**: Gelişmiş audit logging ve raporlama
- **PermissionService**: Rol tabanlı yetki kontrolü için yeni servis

### New Data Transfer Objects
- **RoleChangeDto**: Rol değişikliği istekleri için
- **RecordUpdateDto**: Kayıt güncelleme istekleri için
- **UserUpdateDto**: Kullanıcı bilgi güncelleme istekleri için
- **AuditLogDto**: Audit log görüntüleme için
- **PermissionCheckDto**: Yetki kontrolü sonuçları için

## Data Models

### Enhanced Existing Models
- **User**: `deletedAt` alanı eklenerek soft delete desteği
- **EntryExitRecord**: `deletedAt` ve `modifiedAt` alanları eklenerek düzenleme desteği
- **AdminAuditLog**: Genişletilmiş audit logging için ek alanlar

### New Models
- **UserModificationHistory**: Kullanıcı değişiklik geçmişi
  - `id`: Primary key
  - `userId`: Değiştirilen kullanıcı ID'si
  - `modifiedBy`: Değişikliği yapan admin ID'si
  - `fieldName`: Değiştirilen alan adı
  - `oldValue`: Eski değer
  - `newValue`: Yeni değer
  - `modificationDate`: Değişiklik tarihi

- **RecordModificationHistory**: Kayıt değişiklik geçmişi
  - `id`: Primary key
  - `recordId`: Değiştirilen kayıt ID'si
  - `modifiedBy`: Değişikliği yapan admin ID'si
  - `fieldName`: Değiştirilen alan adı
  - `oldValue`: Eski değer
  - `newValue`: Yeni değer
  - `modificationDate`: Değişiklik tarihi

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Super Admin Role Change UI Access
*For any* Super Admin user accessing user management, the system should display role change options for all users
**Validates: Requirements 1.1**

### Property 2: Admin Role Change UI Restriction
*For any* Admin user accessing user management, the system should hide role change functionality completely
**Validates: Requirements 1.2**

### Property 3: Role Change Persistence and Logging
*For any* role change operation by Super Admin, the system should update the role in database and create an audit log entry
**Validates: Requirements 1.3**

### Property 4: Admin Role Change API Restriction
*For any* role change API request from Admin user, the system should return 403 Forbidden error
**Validates: Requirements 1.4**

### Property 5: Super Admin Role Validation
*For any* role modification operation, the system should validate Super Admin role before allowing the operation
**Validates: Requirements 1.5**

### Property 6: Super Admin Record Management UI
*For any* Super Admin viewing entry/exit records, the system should display edit and delete buttons for each record
**Validates: Requirements 2.1**

### Property 7: Admin Record View Restriction
*For any* Admin viewing entry/exit records, the system should display records in read-only mode without edit/delete options
**Validates: Requirements 2.2**

### Property 8: Record Edit Validation and Logging
*For any* record edit operation by Super Admin, the system should validate changes and update the record with audit logging
**Validates: Requirements 2.3**

### Property 9: Record Soft Delete and Logging
*For any* record deletion by Super Admin, the system should soft-delete the record and create an audit log entry
**Validates: Requirements 2.4**

### Property 10: Admin Record Modification API Restriction
*For any* record modification API request from Admin user, the system should prevent access and return appropriate error
**Validates: Requirements 2.5**

### Property 11: Super Admin User Management UI
*For any* Super Admin viewing user details, the system should display edit and delete options for user information
**Validates: Requirements 3.1**

### Property 12: Admin User View Restriction
*For any* Admin viewing user details, the system should display information in read-only mode
**Validates: Requirements 3.2**

### Property 13: User Data Update Validation and Logging
*For any* user information update by Super Admin, the system should validate and save changes with audit logging
**Validates: Requirements 3.3**

### Property 14: User Soft Delete and Data Integrity
*For any* user deletion by Super Admin, the system should soft-delete the user and maintain referential integrity with audit logging
**Validates: Requirements 3.4**

### Property 15: Referential Integrity Maintenance
*For any* user data modification or deletion, the system should maintain referential integrity across all related data
**Validates: Requirements 3.5**

### Property 16: Super Admin Audit Logging
*For any* modification operation by Super Admin, the system should create detailed audit log entries
**Validates: Requirements 4.1**

### Property 17: Role-Based Audit Log Categorization
*For any* audit log viewing, the system should display Super Admin actions separately from regular Admin actions
**Validates: Requirements 4.2**

### Property 18: Role Change Audit Detail
*For any* role change operation, the system should log before/after values and justification if provided
**Validates: Requirements 4.3**

### Property 19: Record Modification Audit Detail
*For any* record modification, the system should log original and updated values in audit logs
**Validates: Requirements 4.4**

### Property 20: User Data Change Field Tracking
*For any* user data change, the system should log field-level changes with complete before/after information
**Validates: Requirements 4.5**

### Property 21: Admin Dashboard Access Preservation
*For any* Admin accessing the dashboard, the system should display all current statistics and information as before
**Validates: Requirements 5.1**

### Property 22: Admin Read-Only Access Preservation
*For any* Admin viewing users and records, the system should provide read-only access as currently implemented
**Validates: Requirements 5.2**

### Property 23: Admin Functionality Preservation
*For any* existing Admin functionality except role management and data modification, the system should maintain all current capabilities
**Validates: Requirements 5.3**

### Property 24: Clear Permission Error Messages
*For any* restricted operation attempt by Admin, the system should provide clear error messages explaining insufficient permissions
**Validates: Requirements 5.4**

### Property 25: Admin Search and Export Preservation
*For any* Admin user, the system should ensure user searches, filtering, and CSV exports continue to work
**Validates: Requirements 5.5**

### Property 26: Role-Based UI Element Display
*For any* user role (Super Admin vs Admin), the system should display different UI elements based on permissions
**Validates: Requirements 6.1**

### Property 27: Permission-Based UI Control Hiding
*For any* user lacking permissions for an action, the system should hide the corresponding UI controls
**Validates: Requirements 6.2**

### Property 28: Dual-Layer Permission Validation
*For any* operation, the system should validate permissions on both frontend and backend layers
**Validates: Requirements 6.3**

### Property 29: Clear Permission Error Indication
*For any* permission error, the system should clearly indicate insufficient permission levels in error messages
**Validates: Requirements 6.4**

### Property 30: Role-Based Navigation Menu
*For any* user role, the system should provide navigation menus showing only accessible functions
**Validates: Requirements 6.5**

## Error Handling

- **Permission Errors**: Return 403 Forbidden with clear role-based error messages
- **Validation Errors**: Return 400 Bad Request with detailed field validation errors
- **Not Found Errors**: Return 404 Not Found for invalid user/record IDs
- **Soft Delete Errors**: Handle cascading soft deletes with appropriate error messages
- **Audit Log Errors**: Ensure audit logging failures don't prevent main operations

## Testing Strategy

### Unit Testing
- Test role-based permission checking logic
- Verify soft delete functionality and referential integrity
- Test audit logging for all modification operations
- Validate UI element visibility based on user roles

### Property-Based Testing
- Use JUnit 5 with QuickTheories library for property-based tests
- Configure each property test to run minimum 100 iterations
- Each property-based test must reference the corresponding correctness property
- Tag format: **Feature: super-admin-permissions, Property {number}: {property_text}**

### Integration Testing
- Test complete Super Admin workflows (role changes, record edits, user management)
- Verify Admin users retain existing functionality without new permissions
- Test audit logging across all modification operations
- Validate UI behavior for different user roles

## Security Considerations

- Granular role-based access control at both UI and API levels
- Audit logging for all Super Admin operations
- Soft delete to maintain data integrity and audit trails
- Input validation and sanitization for all modification operations
- CSRF protection for all state-changing operations
- Rate limiting for modification endpoints

## Performance Considerations

- Efficient soft delete queries with proper indexing
- Optimized audit log storage and retrieval
- Caching for role-based UI element rendering
- Batch operations for bulk user/record modifications
- Database indexing on soft delete and audit log fields

## UI/UX Design

- Role-based UI element visibility (show/hide based on permissions)
- Clear visual indicators for Super Admin exclusive features
- Confirmation dialogs for destructive operations (delete, role changes)
- Audit trail visualization for tracking changes
- Consistent styling with existing admin panel design
- Accessible design maintaining WCAG compliance