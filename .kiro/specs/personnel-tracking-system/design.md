# Personnel Tracking System - Design Document

## Overview

The Personnel Tracking System is a Spring Boot-based web application with RESTful API services that enables personnel registration, authentication, daily QR code generation, and entry/exit tracking through mobile application integration. The system integrates with an external read-only MySQL database for personnel master data and maintains a local MySQL database for credentials, QR codes, and tracking records.

### Technology Stack

- **Backend Framework**: Spring Boot 4.0.0 with Java 17
- **Web Framework**: Spring MVC with Thymeleaf templates
- **Security**: Spring Security with JWT authentication
- **Database**: MySQL 8.0+ (Local and External)
- **ORM**: Spring Data JPA with Hibernate
- **Password Hashing**: BCrypt (via Spring Security)
- **QR Code Generation**: ZXing (Zebra Crossing) library
- **SMS Service**: External SMS Gateway API integration
- **Build Tool**: Gradle
- **Testing**: JUnit 5 with Spring Boot Test

## Architecture

### Layered Architecture

The system follows a standard layered architecture pattern:

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (Controllers, REST APIs, Views)        │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│          Service Layer                  │
│  (Business Logic, Validation)           │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│       Data Access Layer                 │
│  (Repositories, JPA Entities)           │
└─────────────────────────────────────────┘
                  ↓
┌──────────────────┬──────────────────────┐
│   Local MySQL    │   External MySQL     │
│   (Read/Write)   │   (Read-Only)        │
└──────────────────┴──────────────────────┘
```

### Component Diagram

```
┌──────────────────────────────────────────────────────┐
│                  Web Application                      │
│                                                       │
│  ┌────────────────┐         ┌──────────────────┐    │
│  │ Web Controllers│         │  REST API        │    │
│  │ (Thymeleaf)    │         │  Controllers     │    │
│  └────────────────┘         └──────────────────┘    │
│          ↓                           ↓               │
│  ┌──────────────────────────────────────────────┐   │
│  │         Security Filter Chain                 │   │
│  │    (JWT Authentication & Authorization)       │   │
│  └──────────────────────────────────────────────┘   │
│          ↓                           ↓               │
│  ┌────────────────┐         ┌──────────────────┐    │
│  │ Registration   │         │  Entry/Exit      │    │
│  │ Service        │         │  Service         │    │
│  └────────────────┘         └──────────────────┘    │
│          ↓                           ↓               │
│  ┌────────────────┐         ┌──────────────────┐    │
│  │ Authentication │         │  QR Code         │    │
│  │ Service        │         │  Service         │    │
│  └────────────────┘         └──────────────────┘    │
│          ↓                           ↓               │
│  ┌──────────────────────────────────────────────┐   │
│  │         Repository Layer (JPA)                │   │
│  └──────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────┘
                          ↓
        ┌─────────────────────────────────┐
        │    External Services            │
        │  - SMS Gateway                  │
        │  - External DB Connection       │
        └─────────────────────────────────┘
```

## Components and Interfaces

### 1. Entity Models

#### User Entity (Local Database)
```java
@Entity
@Table(name = "users")
class User {
    Long id;
    String tcNo;           // TC Kimlik No (unique)
    String personnelNo;    // Sicil No
    String firstName;
    String lastName;
    String mobilePhone;
    String passwordHash;   // BCrypt hashed
    UserRole role;         // NORMAL_USER, ADMIN, SUPER_ADMIN
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
```

#### ExternalPersonnel Entity (External Database - Read-Only)
```java
@Entity
@Table(name = "personnel_master")
class ExternalPersonnel {
    Long userId;
    String tcNo;
    String personnelNo;
    String firstName;
    String lastName;
    String mobilePhone;
}
```

#### OtpVerification Entity (Local Database)
```java
@Entity
@Table(name = "otp_verifications")
class OtpVerification {
    Long id;
    String tcNo;
    String otpCode;        // 6-digit code
    LocalDateTime expiresAt;
    boolean verified;
    LocalDateTime createdAt;
}
```

#### QrCode Entity (Local Database)
```java
@Entity
@Table(name = "qr_codes")
class QrCode {
    Long id;
    Long userId;
    String qrCodeValue;    // Unique daily code
    LocalDate validDate;   // Date of validity
    int usageCount;        // 0, 1, or 2
    LocalDateTime createdAt;
}
```

#### EntryExitRecord Entity (Local Database)
```java
@Entity
@Table(name = "entry_exit_records")
class EntryExitRecord {
    Long id;
    Long userId;
    EntryExitType type;    // ENTRY or EXIT
    LocalDateTime timestamp;
    Double latitude;
    Double longitude;
    String qrCodeValue;
    LocalDateTime createdAt;
}
```

### 2. Service Interfaces

#### RegistrationService
```java
interface RegistrationService {
    // Validates TC and Personnel No against external DB
    ExternalPersonnelDto validatePersonnel(String tcNo, String personnelNo);
    
    // Generates and sends OTP via SMS
    void sendOtpVerification(String tcNo, String mobilePhone);
    
    // Verifies OTP code
    boolean verifyOtp(String tcNo, String otpCode);
    
    // Completes registration with password
    UserDto completeRegistration(String tcNo, String password);
}
```

#### AuthenticationService
```java
interface AuthenticationService {
    // Authenticates user and returns JWT token
    AuthTokenDto login(String tcNo, String password);
    
    // Validates JWT token
    UserDto validateToken(String token);
    
    // Refreshes JWT token
    AuthTokenDto refreshToken(String token);
}
```

#### QrCodeService
```java
interface QrCodeService {
    // Generates or retrieves daily QR code for user
    QrCodeDto getDailyQrCode(Long userId);
    
    // Validates QR code for usage
    QrCodeValidationDto validateQrCode(String qrCodeValue, Long userId);
    
    // Increments usage count
    void incrementUsageCount(String qrCodeValue);
    
    // Generates QR code image
    byte[] generateQrCodeImage(String qrCodeValue);
}
```

#### EntryExitService
```java
interface EntryExitService {
    // Records entry or exit event
    EntryExitRecordDto recordEntryExit(
        Long userId,
        String qrCodeValue,
        LocalDateTime timestamp,
        Double latitude,
        Double longitude
    );
    
    // Determines if next usage is entry or exit
    EntryExitType determineEntryExitType(String qrCodeValue);
}
```

#### SmsService
```java
interface SmsService {
    // Sends SMS via external gateway
    void sendSms(String phoneNumber, String message);
    
    // Generates 6-digit OTP
    String generateOtp();
}
```

### 3. REST API Endpoints

#### Web Application Endpoints
```
POST   /api/register/validate          - Validate TC and Personnel No
POST   /api/register/send-otp          - Send OTP to mobile
POST   /api/register/verify-otp        - Verify OTP code
POST   /api/register/complete          - Complete registration with password
POST   /api/auth/login                 - User login
GET    /api/qrcode/daily               - Get daily QR code
GET    /api/qrcode/image               - Get QR code image
```

#### Mobile Application Endpoints
```
POST   /api/mobil/login                - Mobile authentication
POST   /api/mobil/giris-cikis-kaydet   - Record entry/exit event
```

### 4. Security Components

#### JWT Token Structure
```json
{
  "sub": "12345678901",        // TC No
  "userId": 123,
  "role": "NORMAL_USER",
  "iat": 1638360000,
  "exp": 1638361800            // 30 minutes
}
```

#### Security Filter Chain
- JWT Authentication Filter: Validates JWT tokens on protected endpoints
- CORS Filter: Allows mobile application requests
- HTTPS Enforcement: Redirects HTTP to HTTPS

## Data Models

### Database Schema

#### Local Database (personnel_tracking)

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tc_no VARCHAR(11) UNIQUE NOT NULL,
    personnel_no VARCHAR(20) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    mobile_phone VARCHAR(15) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tc_no (tc_no),
    INDEX idx_personnel_no (personnel_no)
);

CREATE TABLE otp_verifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tc_no VARCHAR(11) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tc_no (tc_no),
    INDEX idx_expires_at (expires_at)
);

CREATE TABLE qr_codes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    qr_code_value VARCHAR(255) UNIQUE NOT NULL,
    valid_date DATE NOT NULL,
    usage_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_date (user_id, valid_date),
    INDEX idx_qr_value (qr_code_value)
);

CREATE TABLE entry_exit_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(10) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    qr_code_value VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_id (user_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_qr_code (qr_code_value)
);
```

#### External Database (Read-Only)

```sql
-- Assumed structure based on requirements
CREATE TABLE personnel_master (
    user_id BIGINT PRIMARY KEY,
    tc_no VARCHAR(11) UNIQUE NOT NULL,
    personnel_no VARCHAR(20) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    mobile_phone VARCHAR(15) NOT NULL,
    INDEX idx_tc_personnel (tc_no, personnel_no)
);
```

### Data Transfer Objects (DTOs)

```java
record ExternalPersonnelDto(
    Long userId,
    String tcNo,
    String personnelNo,
    String firstName,
    String lastName,
    String mobilePhone
) {}

record UserDto(
    Long id,
    String tcNo,
    String personnelNo,
    String firstName,
    String lastName,
    String role
) {}

record AuthTokenDto(
    String token,
    String tokenType,
    Long expiresIn,
    UserDto user
) {}

record QrCodeDto(
    String qrCodeValue,
    LocalDate validDate,
    int usageCount,
    int maxUsage
) {}

record QrCodeValidationDto(
    boolean valid,
    String message,
    EntryExitType nextType
) {}

record EntryExitRecordDto(
    Long id,
    Long userId,
    EntryExitType type,
    LocalDateTime timestamp,
    Double latitude,
    Double longitude
) {}

record EntryExitRequestDto(
    String qrCodeValue,
    LocalDateTime timestamp,
    Double latitude,
    Double longitude
) {}
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: External personnel data extraction completeness
*For any* valid TC ID and Personnel Number combination that exists in the External Database, querying and extracting personnel data should return all required fields: User ID, First Name, Last Name, Personnel Number, and Mobile Phone.
**Validates: Requirements 1.2, 10.3**

### Property 2: OTP format and expiration consistency
*For any* generated OTP, the code should be exactly 6 digits and the expiration time should be exactly 5 minutes from creation time.
**Validates: Requirements 1.4**

### Property 3: OTP verification correctness
*For any* submitted OTP and TC ID combination, verification should succeed if and only if the OTP matches the stored code, has not expired, and has not been previously verified.
**Validates: Requirements 2.1, 2.2**

### Property 4: Password validation rules enforcement
*For any* submitted password, validation should reject passwords that are shorter than 8 characters or lack at least one uppercase letter, one lowercase letter, or one special character.
**Validates: Requirements 3.1, 3.2**

### Property 5: Password hashing irreversibility
*For any* valid password, after BCrypt hashing, the stored hash should never equal the plaintext password and should be verifiable using BCrypt's verification function.
**Validates: Requirements 3.4, 9.2**

### Property 6: Default role assignment
*For any* newly created user account, the assigned role should always be NORMAL_USER.
**Validates: Requirements 3.5, 11.1**

### Property 7: Authentication token generation
*For any* successful login with valid TC ID and password, the system should generate a JWT token containing the user's TC ID, user ID, and role in the payload.
**Validates: Requirements 4.3, 7.2, 11.3**

### Property 8: JWT expiration time consistency
*For any* generated JWT token, the expiration time should be exactly 30 minutes from the issued-at time.
**Validates: Requirements 4.5**

### Property 9: JWT validation strictness
*For any* JWT token with invalid signature or expired timestamp, validation should fail and reject the request.
**Validates: Requirements 9.4**

### Property 10: Daily QR code uniqueness and idempotency
*For any* user and date combination, requesting a QR code multiple times on the same date should return the same QR Code Value, but different dates should produce different values.
**Validates: Requirements 5.1, 5.2**

### Property 11: QR code validity period enforcement
*For any* QR Code Value, validation should succeed only when the current date matches the valid date of the code.
**Validates: Requirements 5.3, 6.4**

### Property 12: QR code initial state
*For any* newly created QR Code Value, the usage counter should be initialized to 0 and the maximum usage limit should be 2.
**Validates: Requirements 5.4**

### Property 13: QR code image round-trip
*For any* QR Code Value, generating a QR code image and then decoding it should produce the original QR Code Value.
**Validates: Requirements 5.5**

### Property 14: QR code usage increment atomicity
*For any* QR Code Value, each usage should increment the counter by exactly 1, and the counter should never exceed 2.
**Validates: Requirements 6.1, 6.2**

### Property 15: Entry/exit type determination
*For any* QR Code Value, when usage counter is 0, the next usage should be classified as ENTRY; when usage counter is 1, the next usage should be classified as EXIT.
**Validates: Requirements 6.5, 8.4**

### Property 16: QR code validation against user ownership
*For any* entry/exit request, QR code validation should verify that the QR Code Value belongs to the user identified in the JWT token.
**Validates: Requirements 8.2**

### Property 17: Entry/exit record completeness
*For any* successful entry/exit event, the stored record should contain all required fields: Personnel ID, Type, Timestamp, GPS Latitude, GPS Longitude, and QR Code Value.
**Validates: Requirements 8.5, 12.1**

### Property 18: GPS coordinate range validation
*For any* GPS coordinates in an entry/exit request, latitude should be rejected if outside the range [-90, 90] and longitude should be rejected if outside the range [-180, 180].
**Validates: Requirements 12.2, 12.3**

### Property 19: Role inclusion in JWT
*For any* JWT token generated for authentication, the token payload should include the user's role field.
**Validates: Requirements 7.4, 11.2**

### Property 20: External database query parameters
*For any* personnel validation request, the query to the External Database should include both TC ID and Personnel Number as search criteria.
**Validates: Requirements 10.2**

## Error Handling

### Error Categories

#### 1. Validation Errors (HTTP 400)
- Invalid TC ID or Personnel Number format
- Personnel not found in External Database
- Invalid OTP code or expired OTP
- Password does not meet complexity requirements
- Invalid QR code format
- GPS coordinates out of valid range
- QR code usage limit exceeded
- QR code date mismatch

#### 2. Authentication Errors (HTTP 401)
- Invalid credentials (TC ID or password incorrect)
- Missing JWT token
- Invalid JWT token signature
- Expired JWT token

#### 3. Authorization Errors (HTTP 403)
- User role insufficient for requested operation
- QR code does not belong to authenticated user

#### 4. Resource Not Found Errors (HTTP 404)
- User not found
- QR code not found

#### 5. External Service Errors (HTTP 503)
- External Database connection failure
- SMS gateway unavailable
- SMS sending failure

#### 6. Internal Server Errors (HTTP 500)
- Database write failure
- Unexpected exceptions
- QR code generation failure

### Error Response Format

All API errors should return a consistent JSON structure:

```json
{
  "timestamp": "2024-12-06T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "QR code has already been used twice",
  "path": "/api/mobil/giris-cikis-kaydet",
  "details": {
    "field": "qrCodeValue",
    "rejectedValue": "ABC123XYZ",
    "reason": "USAGE_LIMIT_EXCEEDED"
  }
}
```

### Exception Handling Strategy

```java
@ControllerAdvice
class GlobalExceptionHandler {
    // Handles validation errors
    @ExceptionHandler(ValidationException.class)
    ResponseEntity<ErrorResponse> handleValidation(ValidationException ex);
    
    // Handles authentication failures
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex);
    
    // Handles external service failures
    @ExceptionHandler(ExternalServiceException.class)
    ResponseEntity<ErrorResponse> handleExternalService(ExternalServiceException ex);
    
    // Handles all other exceptions
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleGeneral(Exception ex);
}
```

### Retry and Circuit Breaker

For external service calls (SMS Gateway, External Database):
- Implement retry logic with exponential backoff (3 attempts)
- Implement circuit breaker pattern to prevent cascading failures
- Use Spring Retry and Resilience4j libraries

## Testing Strategy

### Unit Testing

Unit tests will verify specific examples, edge cases, and error conditions for individual components:

**Service Layer Tests:**
- RegistrationService: Test OTP generation, personnel validation, registration completion
- AuthenticationService: Test login success/failure, token generation, token validation
- QrCodeService: Test daily code generation, usage tracking, validation logic
- EntryExitService: Test entry/exit type determination, record creation

**Repository Layer Tests:**
- Test CRUD operations for all entities
- Test custom query methods
- Test database constraints and indexes

**Controller Layer Tests:**
- Test request/response mapping
- Test validation annotations
- Test error responses

**Edge Cases to Test:**
- Empty or null inputs
- Expired OTPs and JWT tokens
- QR codes at usage limit
- Invalid GPS coordinates
- Concurrent QR code usage attempts
- Database connection failures

### Property-Based Testing

Property-based tests will verify universal properties across all inputs using **JUnit-Quickcheck** library:

**Configuration:**
- Each property test should run a minimum of 100 iterations
- Use custom generators for domain objects (TC IDs, passwords, GPS coordinates)
- Each test must reference its corresponding correctness property using the format: `**Feature: personnel-tracking-system, Property {number}: {property_text}**`

**Property Test Coverage:**
- Password validation rules (Properties 4, 5)
- OTP format and expiration (Properties 2, 3)
- QR code uniqueness and validity (Properties 10, 11, 12, 13, 14)
- JWT token structure and expiration (Properties 7, 8, 9, 19)
- Entry/exit type determination (Property 15)
- GPS coordinate validation (Property 18)
- Role assignment (Property 6)
- Data completeness (Properties 1, 17, 20)

**Custom Generators:**
```java
// Generate valid TC IDs (11 digits)
@Generator
class TcIdGenerator extends Generator<String> {
    String generate(SourceOfRandomness random, GenerationStatus status) {
        // Generate 11-digit TC ID
    }
}

// Generate valid passwords
@Generator
class PasswordGenerator extends Generator<String> {
    String generate(SourceOfRandomness random, GenerationStatus status) {
        // Generate password meeting complexity requirements
    }
}

// Generate GPS coordinates
@Generator
class GpsCoordinateGenerator extends Generator<GpsCoordinate> {
    GpsCoordinate generate(SourceOfRandomness random, GenerationStatus status) {
        // Generate valid lat/long pairs
    }
}
```

### Integration Testing

Integration tests will verify component interactions:
- Database integration with actual MySQL test containers
- External database connection (using test database)
- SMS service integration (using mock SMS gateway)
- End-to-end API flows (registration → login → QR code → entry/exit)

### Security Testing

- Test HTTPS enforcement
- Test JWT token tampering detection
- Test password hashing strength
- Test SQL injection prevention
- Test CORS configuration

## Implementation Phases

### Phase 1: Core Infrastructure
- Database schema creation
- Entity models and repositories
- Security configuration (JWT, BCrypt)
- Exception handling framework

### Phase 2: Registration and Authentication
- External database integration
- OTP generation and SMS service
- Registration flow implementation
- Login and JWT token generation

### Phase 3: QR Code Management
- QR code generation service
- Daily code uniqueness logic
- Usage tracking implementation
- QR code image generation

### Phase 4: Mobile API
- Entry/exit recording endpoint
- QR code validation
- GPS coordinate validation
- Entry/exit type determination

### Phase 5: Web Interface
- Thymeleaf templates for registration
- Login page
- QR code display page
- User dashboard

### Phase 6: Testing and Refinement
- Unit test implementation
- Property-based test implementation
- Integration test implementation
- Security testing
- Performance optimization

## Configuration Management

### Application Properties

```properties
# Database - Local
spring.datasource.url=jdbc:mysql://localhost:3306/personnel_tracking
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Database - External (Read-Only)
external.datasource.url=jdbc:mysql://external-host:3306/personnel_master
external.datasource.username=${EXTERNAL_DB_USERNAME}
external.datasource.password=${EXTERNAL_DB_PASSWORD}

# JWT Configuration
jwt.secret=${JWT_SECRET}
jwt.expiration=1800000  # 30 minutes in milliseconds

# SMS Gateway
sms.gateway.url=${SMS_GATEWAY_URL}
sms.gateway.api-key=${SMS_GATEWAY_API_KEY}
sms.gateway.sender=${SMS_SENDER_ID}

# OTP Configuration
otp.length=6
otp.expiration=300000  # 5 minutes in milliseconds

# QR Code Configuration
qrcode.size=300
qrcode.format=PNG

# Security
server.ssl.enabled=true
server.ssl.key-store=${SSL_KEYSTORE_PATH}
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
```

### Environment Variables

Required environment variables for deployment:
- `DB_USERNAME`, `DB_PASSWORD`: Local database credentials
- `EXTERNAL_DB_USERNAME`, `EXTERNAL_DB_PASSWORD`: External database credentials
- `JWT_SECRET`: Secret key for JWT signing
- `SMS_GATEWAY_URL`, `SMS_GATEWAY_API_KEY`, `SMS_SENDER_ID`: SMS service configuration
- `SSL_KEYSTORE_PATH`, `SSL_KEYSTORE_PASSWORD`: SSL certificate configuration

## Performance Considerations

### Database Optimization
- Index on `users.tc_no` for fast login lookups
- Composite index on `qr_codes(user_id, valid_date)` for daily QR code retrieval
- Index on `entry_exit_records.timestamp` for reporting queries
- Connection pooling with HikariCP (default in Spring Boot)

### Caching Strategy
- Cache daily QR codes in Redis (TTL: 24 hours)
- Cache user details after authentication (TTL: 30 minutes)
- Cache external personnel data during registration (TTL: 5 minutes)

### Concurrency Handling
- Use optimistic locking for QR code usage counter updates
- Implement database-level constraints to prevent duplicate entries
- Use transaction isolation level READ_COMMITTED

### API Rate Limiting
- Implement rate limiting on registration endpoints (5 requests per minute per IP)
- Implement rate limiting on login endpoints (10 requests per minute per IP)
- Implement rate limiting on entry/exit endpoints (20 requests per minute per user)

## Monitoring and Logging

### Logging Strategy
- Use SLF4J with Logback
- Log levels: ERROR for failures, WARN for validation errors, INFO for business events, DEBUG for detailed flow
- Structured logging with JSON format for log aggregation
- Log sensitive data masking (passwords, OTP codes)

### Metrics to Track
- Registration success/failure rate
- Login success/failure rate
- OTP verification success/failure rate
- QR code generation time
- Entry/exit recording success rate
- External database response time
- SMS gateway response time

### Health Checks
- Database connectivity (local and external)
- SMS gateway availability
- Disk space for QR code image storage
- Memory usage
- Thread pool status

## Security Best Practices

1. **Password Security**: BCrypt with cost factor 12
2. **JWT Security**: Use strong secret key (256-bit), short expiration times
3. **HTTPS Only**: Redirect all HTTP traffic to HTTPS
4. **SQL Injection Prevention**: Use JPA parameterized queries
5. **XSS Prevention**: Thymeleaf auto-escaping enabled
6. **CSRF Protection**: Enable for web forms, disable for API endpoints with JWT
7. **CORS Configuration**: Whitelist mobile application origins only
8. **Input Validation**: Validate all inputs at controller level
9. **Error Messages**: Don't expose sensitive information in error messages
10. **Audit Logging**: Log all authentication attempts and entry/exit events
