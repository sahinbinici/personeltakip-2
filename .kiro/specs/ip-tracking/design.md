# IP Adresi Takibi Design Document

## Overview

Bu tasarım, personel takip sistemine IP adresi takibi özelliği ekler. Sistem, giriş/çıkış işlemlerinin hangi IP adresinden yapıldığını otomatik olarak kaydedecek, admin panelinde bu bilgileri görüntüleyecek ve kullanıcılara opsiyonel IP tanımlaması yapma imkanı sağlayacaktır. Tasarım, mevcut sistemi etkilemeden geriye dönük uyumluluğu koruyacak şekilde yapılmıştır.

## Architecture

Mevcut personel takip sistemi mimarisini genişleten yapı:

- **Presentation Layer**: IP adresi görüntüleme ve yönetimi için genişletilmiş Thymeleaf şablonları
- **Controller Layer**: IP adresi yakalama ve yönetimi için genişletilmiş Spring MVC kontrolcüleri
- **Service Layer**: IP adresi işleme, doğrulama ve karşılaştırma iş mantığı
- **Data Access Layer**: IP adresi depolama için genişletilmiş JPA repository'leri
- **Security Layer**: IP adresi verilerinin güvenli işlenmesi ve gizlilik koruması

## Components and Interfaces

### Enhanced Entry/Exit Components
- **EntryExitController**: IP adresi yakalama ile genişletilmiş giriş/çıkış endpoint'leri
- **EntryExitService**: IP adresi işleme ve doğrulama ile genişletilmiş iş mantığı
- **IpAddressService**: IP adresi yakalama, doğrulama ve işleme için yeni servis

### Enhanced Admin Components
- **AdminRecordsController**: IP adresi filtreleme ve görüntüleme ile genişletilmiş kayıt yönetimi
- **AdminUserController**: IP tanımlaması yönetimi ile genişletilmiş kullanıcı yönetimi
- **AdminReportsController**: IP uyumluluk raporları için yeni kontrolcü

### New Services
- **IpAddressService**: IP adresi yakalama, doğrulama ve formatlaması
- **IpComplianceService**: IP uyumluluk kontrolü ve mismatch tespiti
- **IpPrivacyService**: IP adresi gizlilik ve anonimleştirme işlemleri

### Enhanced Data Transfer Objects
- **EntryExitRecordDto**: IP adresi bilgisi ile genişletilmiş
- **AdminRecordDto**: IP adresi ve uyumluluk bilgisi ile genişletilmiş
- **AdminUserDto**: IP tanımlaması bilgisi ile genişletilmiş
- **IpComplianceReportDto**: IP uyumluluk raporları için yeni DTO

## Data Models

### Enhanced Existing Models
- **EntryExitRecord**: `ipAddress` alanı eklenerek IP adresi desteği
- **User**: `assignedIpAddresses` alanı eklenerek IP tanımlaması desteği

### New Models
- **IpAddressLog**: IP adresi erişim ve değişiklik kayıtları
  - `id`: Primary key
  - `userId`: İlgili kullanıcı ID'si
  - `ipAddress`: Erişilen veya değiştirilen IP adresi
  - `action`: Yapılan işlem (VIEW, ASSIGN, REMOVE, MISMATCH)
  - `adminUserId`: İşlemi yapan admin ID'si (varsa)
  - `timestamp`: İşlem zamanı
  - `details`: Ek detaylar (JSON format)

### Database Schema Changes
```sql
-- EntryExitRecord tablosuna IP adresi alanı ekleme
ALTER TABLE entry_exit_records 
ADD COLUMN ip_address VARCHAR(45) NULL COMMENT 'Client IP address (IPv4 or IPv6)';

-- User tablosuna IP tanımlaması alanı ekleme
ALTER TABLE users 
ADD COLUMN assigned_ip_addresses TEXT NULL COMMENT 'Comma-separated list of assigned IP addresses';

-- IP adresi log tablosu oluşturma
CREATE TABLE ip_address_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ip_address VARCHAR(45),
    action VARCHAR(20) NOT NULL,
    admin_user_id BIGINT NULL,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details JSON NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_action (action)
);
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: IP Address Capture and Storage
*For any* entry/exit operation, the system should capture and store the client IP address in the database record
**Validates: Requirements 1.1**

### Property 2: Proxy-Aware IP Extraction
*For any* HTTP request with proxy headers, the system should extract the real client IP address correctly
**Validates: Requirements 1.2**

### Property 3: Unknown IP Default Handling
*For any* entry/exit operation where IP cannot be determined, the system should store a default "Unknown" value
**Validates: Requirements 1.3**

### Property 4: IP Address Format Validation
*For any* IP address before storage, the system should validate the format and reject invalid IP addresses
**Validates: Requirements 1.4**

### Property 5: Backward Compatibility Preservation
*For any* existing entry/exit functionality, adding IP tracking should not affect the original behavior
**Validates: Requirements 1.5**

### Property 6: IP Address Column Display
*For any* admin records view, the system should display IP address column for each entry/exit record
**Validates: Requirements 2.1**

### Property 7: IPv4 and IPv6 Format Display
*For any* IP address display, the system should show both IPv4 and IPv6 addresses in readable format
**Validates: Requirements 2.2**

### Property 8: Unknown IP Indicator Display
*For any* record with unknown IP address, the system should display clear "Unknown" or "N/A" indicator
**Validates: Requirements 2.3**

### Property 9: IP Address Filtering Functionality
*For any* IP address or IP range filter, the system should return only records matching the filter criteria
**Validates: Requirements 2.4**

### Property 10: CSV Export IP Inclusion
*For any* CSV export operation, the exported file should include IP address information for all records
**Validates: Requirements 2.5**

### Property 11: IP Assignment Field Availability
*For any* user management interface, the system should provide optional IP address assignment field
**Validates: Requirements 3.1**

### Property 12: IP Assignment Format Validation
*For any* IP address assignment, the system should validate both IPv4 and IPv6 formats before saving
**Validates: Requirements 3.2**

### Property 13: Assigned IP Display in User Details
*For any* user with assigned IP addresses, the system should display this information in user details
**Validates: Requirements 3.3**

### Property 14: Multiple IP Address Support
*For any* user IP assignment, the system should support multiple IP addresses separated by commas or semicolons
**Validates: Requirements 3.4**

### Property 15: IP Assignment Removal Safety
*For any* IP assignment removal operation, the system should not affect other user account data
**Validates: Requirements 3.5**

### Property 16: IP Mismatch Highlighting
*For any* entry/exit record view, the system should highlight mismatches between actual and assigned IP addresses
**Validates: Requirements 4.1**

### Property 17: IP Mismatch Warning Indicators
*For any* user with assigned IPs, the system should show warning indicators for entry/exit from different IPs
**Validates: Requirements 4.2**

### Property 18: IP Compliance Statistics in Reports
*For any* generated report, the system should include IP compliance statistics and mismatch information
**Validates: Requirements 4.3**

### Property 19: IP Mismatch Filtering
*For any* IP mismatch filter application, the system should show only records with IP address mismatches
**Validates: Requirements 4.4**

### Property 20: Secure IP Address Storage
*For any* IP address storage operation, the system should apply appropriate database constraints and security measures
**Validates: Requirements 5.1**

### Property 21: Privacy-Compliant IP Display
*For any* IP address display, the system should respect configured privacy settings and mask IPs when required
**Validates: Requirements 5.2**

### Property 22: IP Access Audit Logging
*For any* IP address access or modification, the system should create appropriate audit log entries
**Validates: Requirements 5.3**

### Property 23: IP Address Anonymization
*For any* IP anonymization request, the system should properly mask or anonymize IP addresses in reports
**Validates: Requirements 5.5**

### Property 24: Graceful IP Capture Failure Handling
*For any* IP capture failure, the system should continue entry/exit operations without blocking functionality
**Validates: Requirements 6.2**

### Property 25: QR Code Functionality Preservation
*For any* QR code operation, enabling IP tracking should not change existing QR code functionality
**Validates: Requirements 6.3**

### Property 26: Clear IP Tracking Information Display
*For any* user interface requiring IP tracking information, the system should provide clear and understandable information
**Validates: Requirements 6.4**

### Property 27: IP Tracking Configuration Control
*For any* IP tracking configuration change, the system should allow enabling/disabling IP tracking functionality
**Validates: Requirements 6.5**

## Error Handling

- **IP Capture Errors**: Log failures but continue with entry/exit operations
- **IP Validation Errors**: Return clear validation messages for invalid IP formats
- **IP Assignment Errors**: Provide user-friendly error messages for assignment failures
- **Privacy Configuration Errors**: Handle privacy setting failures gracefully
- **Database Constraint Errors**: Handle IP address storage constraints appropriately

## Testing Strategy

### Unit Testing
- Test IP address extraction from various HTTP headers
- Verify IP address validation for IPv4 and IPv6 formats
- Test IP assignment parsing and storage functionality
- Validate IP mismatch detection logic

### Property-Based Testing
- Use JUnit 5 with QuickTheories library for property-based tests
- Configure each property test to run minimum 100 iterations
- Each property-based test must reference the corresponding correctness property
- Tag format: **Feature: ip-tracking, Property {number}: {property_text}**

### Integration Testing
- Test complete IP tracking workflow from capture to display
- Verify IP assignment and mismatch detection end-to-end
- Test CSV export functionality with IP address data
- Validate privacy and anonymization features

## Security Considerations

- Secure storage of IP address data with appropriate constraints
- Privacy-compliant handling of IP address information
- Audit logging for all IP address access and modifications
- Input validation and sanitization for IP address inputs
- Configurable IP address masking and anonymization
- Rate limiting for IP-related administrative operations

## Performance Considerations

- Efficient IP address extraction without adding significant latency
- Optimized database queries for IP address filtering
- Caching for IP assignment lookups
- Indexed database columns for IP address searches
- Minimal performance impact on existing entry/exit operations

## Privacy and Compliance

- GDPR-compliant IP address handling with anonymization options
- Configurable data retention policies for IP address logs
- User consent mechanisms for IP address tracking if required
- Clear privacy notices about IP address collection and usage
- Secure deletion of IP address data when required

## UI/UX Design

- Clear IP address column in admin records table
- Intuitive IP assignment interface in user management
- Visual indicators for IP address mismatches
- User-friendly IP address format validation messages
- Responsive design for IP-related UI components
- Accessible design maintaining WCAG compliance