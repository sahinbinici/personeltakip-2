# Implementation Plan

- [x] 1. Set up project dependencies and database configuration
  - Add Spring Security, JWT, BCrypt, JPA, MySQL connector dependencies to build.gradle
  - Add ZXing library for QR code generation
  - Add JUnit-Quickcheck for property-based testing
  - Add Resilience4j for circuit breaker and retry logic
  - Configure local and external database connections in application.properties
  - Create database schema SQL scripts for local database
  - _Requirements: 9.1, 9.2, 10.1_

- [x] 2. Implement core entity models and repositories
  - [x] 2.1 Create User entity with JPA annotations
    - Define User entity with all fields (tcNo, personnelNo, firstName, lastName, mobilePhone, passwordHash, role)
    - Add validation annotations and indexes
    - Create UserRole enum (NORMAL_USER, ADMIN, SUPER_ADMIN)
    - _Requirements: 3.5, 11.1, 11.2_
  
  - [x] 2.2 Create OtpVerification entity
    - Define OtpVerification entity with tcNo, otpCode, expiresAt, verified fields
    - Add indexes for efficient lookup
    - _Requirements: 1.4, 2.1_
  
  - [x] 2.3 Create QrCode entity
    - Define QrCode entity with userId, qrCodeValue, validDate, usageCount fields
    - Add unique constraint on qrCodeValue
    - Add composite index on (userId, validDate)
    - _Requirements: 5.1, 5.4, 6.1_
  
  - [x] 2.4 Create EntryExitRecord entity
    - Define EntryExitRecord entity with userId, type, timestamp, latitude, longitude, qrCodeValue
    - Create EntryExitType enum (ENTRY, EXIT)
    - Add indexes for reporting queries
    - _Requirements: 8.5, 12.1_
  
  - [x] 2.5 Create ExternalPersonnel entity for read-only external database
    - Define ExternalPersonnel entity mapped to external database
    - Configure separate datasource for external database
    - _Requirements: 1.1, 10.1_
  
  - [x] 2.6 Create Spring Data JPA repositories
    - Create UserRepository with findByTcNo method
    - Create OtpVerificationRepository with findByTcNoAndVerifiedFalse method
    - Create QrCodeRepository with findByUserIdAndValidDate method
    - Create EntryExitRecordRepository
    - Create ExternalPersonnelRepository with findByTcNoAndPersonnelNo method
    - _Requirements: 1.1, 4.1, 5.1_

- [x] 2.7 Write property test for default role assignment
  - **Property 6: Default role assignment**
  - **Validates: Requirements 3.5, 11.1**

- [x] 2.8 Write unit tests for entity models
  - Test entity creation and field validation
  - Test enum values
  - Test entity relationships
  - _Requirements: 2.1, 3.5, 5.4_

- [x] 3. Implement security configuration and JWT handling
  - [x] 3.1 Configure Spring Security
    - Create SecurityConfig class with HTTP security configuration
    - Configure password encoder (BCrypt with cost factor 12)
    - Disable CSRF for API endpoints
    - Enable CORS for mobile application
    - _Requirements: 9.2, 9.3_
  
  - [x] 3.2 Implement JWT utility service
    - Create JwtUtil class for token generation and validation
    - Implement generateToken method with user details and 30-minute expiration
    - Implement validateToken method with signature and expiration checks
    - Implement extractClaims method to get userId, tcNo, and role from token
    - _Requirements: 4.3, 4.5, 9.3, 9.4_
  
  - [x] 3.3 Create JWT authentication filter
    - Implement JwtAuthenticationFilter extending OncePerRequestFilter
    - Extract JWT from Authorization header
    - Validate token and set authentication in SecurityContext
    - _Requirements: 7.1, 8.1, 9.4_
  
  - [x] 3.4 Configure filter chain
    - Register JwtAuthenticationFilter in security filter chain
    - Configure public endpoints (registration, login)
    - Configure protected endpoints (QR code, entry/exit)
    - _Requirements: 4.1, 7.1_

- [x] 3.5 Write property test for JWT expiration time consistency
  - **Property 8: JWT expiration time consistency**
  - **Validates: Requirements 4.5**

- [x] 3.6 Write property test for JWT validation strictness
  - **Property 9: JWT validation strictness**
  - **Validates: Requirements 9.4**

- [-] 3.7 Write property test for password hashing irreversibility
  - **Property 5: Password hashing irreversibility**
  - **Validates: Requirements 3.4, 9.2**

- [x] 3.8 Write unit tests for JWT utility and security configuration
  - Test token generation with various user details
  - Test token validation with valid and invalid tokens
  - Test expired token rejection
  - Test claim extraction
  - _Requirements: 4.3, 4.5, 9.3, 9.4_

- [x] 4. Implement SMS service integration
  - [x] 4.1 Create SmsService interface and implementation
    - Define sendSms method for sending SMS via external gateway
    - Implement generateOtp method for 6-digit code generation
    - Configure SMS gateway URL and API key from properties
    - _Requirements: 1.5_
  
  - [x] 4.2 Add retry and circuit breaker for SMS gateway
    - Configure Resilience4j retry with 3 attempts and exponential backoff
    - Configure circuit breaker to prevent cascading failures
    - _Requirements: 1.5_

- [x] 4.3 Write property test for OTP format and expiration consistency
  - **Property 2: OTP format and expiration consistency**
  - **Validates: Requirements 1.4**

- [x] 4.4 Write unit tests for SMS service
  - Test OTP generation format (6 digits)
  - Test SMS sending with mock gateway
  - Test retry logic on failure
  - Test circuit breaker activation
  - _Requirements: 1.4, 1.5_

- [x] 5. Implement registration service and flow
  - [x] 5.1 Create RegistrationService
    - Implement validatePersonnel method to query external database
    - Implement sendOtpVerification method to generate and send OTP
    - Implement verifyOtp method to validate OTP code and expiration
    - Implement completeRegistration method to hash password and save user
    - Add OTP cleanup logic for expired codes
    - _Requirements: 1.1, 1.2, 1.4, 1.5, 2.1, 2.2, 2.5, 3.4, 3.5_
  
  - [x] 5.2 Create DTOs for registration flow
    - Create PersonnelValidationRequest DTO (tcNo, personnelNo)
    - Create ExternalPersonnelDto (userId, tcNo, personnelNo, firstName, lastName, mobilePhone)
    - Create OtpVerificationRequest DTO (tcNo, otpCode)
    - Create RegistrationCompleteRequest DTO (tcNo, password)
    - Create UserDto response DTO
    - _Requirements: 1.1, 2.1, 3.5_
  
  - [x] 5.3 Implement password validation utility
    - Create PasswordValidator class
    - Validate minimum 8 characters
    - Validate at least one uppercase, one lowercase, one special character
    - Return specific error messages for each validation failure
    - _Requirements: 3.1, 3.2_

- [x] 5.4 Write property test for external personnel data extraction completeness
  - **Property 1: External personnel data extraction completeness**
  - **Validates: Requirements 1.2, 10.3**

- [x] 5.5 Write property test for OTP verification correctness
  - **Property 3: OTP verification correctness**
  - **Validates: Requirements 2.1, 2.2**

- [x] 5.6 Write property test for password validation rules enforcement
  - **Property 4: Password validation rules enforcement**
  - **Validates: Requirements 3.1, 3.2**

- [x] 5.7 Write unit tests for registration service
  - Test personnel validation with valid and invalid data
  - Test OTP generation and expiration
  - Test OTP verification success and failure cases
  - Test password validation with various inputs
  - Test registration completion
  - _Requirements: 1.1, 1.2, 1.4, 2.1, 3.1, 3.2, 3.4_

- [x] 6. Implement authentication service
  - [x] 6.1 Create AuthenticationService
    - Implement login method to validate credentials and generate JWT
    - Implement validateToken method to verify JWT and return user details
    - Query user by tcNo from local database
    - Verify password using BCrypt
    - Generate JWT token with userId, tcNo, and role
    - _Requirements: 4.1, 4.2, 4.3, 4.5_
  
  - [x] 6.2 Create authentication DTOs
    - Create LoginRequest DTO (tcNo, password)
    - Create AuthTokenDto (token, tokenType, expiresIn, user)
    - _Requirements: 4.3_

- [x] 6.3 Write property test for authentication token generation
  - **Property 7: Authentication token generation**
  - **Validates: Requirements 4.3, 7.2, 11.3**

- [x] 6.4 Write property test for role inclusion in JWT
  - **Property 19: Role inclusion in JWT**
  - **Validates: Requirements 7.4, 11.2**

- [x] 6.5 Write unit tests for authentication service
  - Test successful login with valid credentials
  - Test login failure with invalid credentials
  - Test JWT token generation
  - Test token validation
  - _Requirements: 4.1, 4.2, 4.3, 4.5_

- [x] 7. Implement QR code service
  - [x] 7.1 Create QrCodeService
    - Implement getDailyQrCode method to generate or retrieve QR code for current date
    - Implement generateUniqueQrCodeValue using userId, date, and random salt
    - Implement validateQrCode method to check validity, date, and usage count
    - Implement incrementUsageCount method with optimistic locking
    - Implement generateQrCodeImage method using ZXing library
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2, 6.4_
  
  - [x] 7.2 Create QR code DTOs
    - Create QrCodeDto (qrCodeValue, validDate, usageCount, maxUsage)
    - Create QrCodeValidationDto (valid, message, nextType)
    - _Requirements: 5.1_

- [x] 7.3 Write property test for daily QR code uniqueness and idempotency
  - **Property 10: Daily QR code uniqueness and idempotency**
  - **Validates: Requirements 5.1, 5.2**

- [x] 7.4 Write property test for QR code validity period enforcement
  - **Property 11: QR code validity period enforcement**
  - **Validates: Requirements 5.3, 6.4**

- [x] 7.5 Write property test for QR code initial state
  - **Property 12: QR code initial state**
  - **Validates: Requirements 5.4**

- [x] 7.6 Write property test for QR code image round-trip
  - **Property 13: QR code image round-trip**
  - **Validates: Requirements 5.5**

- [x] 7.7 Write property test for QR code usage increment atomicity
  - **Property 14: QR code usage increment atomicity**
  - **Validates: Requirements 6.1, 6.2**

- [x] 7.8 Write unit tests for QR code service
  - Test daily QR code generation
  - Test QR code uniqueness for same user and date
  - Test QR code validation logic
  - Test usage count increment
  - Test QR code image generation and decoding
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2_

- [x] 8. Implement entry/exit service
  - [x] 8.1 Create EntryExitService
    - Implement recordEntryExit method to save entry/exit records
    - Implement determineEntryExitType based on QR code usage count
    - Validate GPS coordinates (latitude: -90 to 90, longitude: -180 to 180)
    - Validate QR code ownership against authenticated user
    - Increment QR code usage count after successful recording
    - _Requirements: 6.5, 8.2, 8.4, 8.5, 12.1, 12.2, 12.3_
  
  - [x] 8.2 Create entry/exit DTOs
    - Create EntryExitRequestDto (qrCodeValue, timestamp, latitude, longitude)
    - Create EntryExitRecordDto (id, userId, type, timestamp, latitude, longitude)
    - _Requirements: 8.5_

- [x] 8.3 Write property test for entry/exit type determination
  - **Property 15: Entry/exit type determination**
  - **Validates: Requirements 6.5, 8.4**

- [x] 8.4 Write property test for QR code validation against user ownership
  - **Property 16: QR code validation against user ownership**
  - **Validates: Requirements 8.2**

- [x] 8.5 Write property test for entry/exit record completeness
  - **Property 17: Entry/exit record completeness**
  - **Validates: Requirements 8.5, 12.1**

- [x] 8.6 Write property test for GPS coordinate range validation
  - **Property 18: GPS coordinate range validation**
  - **Validates: Requirements 12.2, 12.3**

- [x] 8.7 Write unit tests for entry/exit service
  - Test entry/exit type determination
  - Test GPS coordinate validation
  - Test QR code ownership validation
  - Test record creation
  - _Requirements: 6.5, 8.2, 8.4, 8.5, 12.2, 12.3_

- [x] 9. Implement REST API controllers
  - [x] 9.1 Create RegistrationController
    - POST /api/register/validate - Validate personnel against external DB
    - POST /api/register/send-otp - Generate and send OTP
    - POST /api/register/verify-otp - Verify OTP code
    - POST /api/register/complete - Complete registration with password
    - Add request validation annotations
    - Add error handling with appropriate HTTP status codes
    - _Requirements: 1.1, 1.5, 2.1, 3.5_
  
  - [x] 9.2 Create AuthenticationController
    - POST /api/auth/login - User login with TC and password
    - Add rate limiting (10 requests per minute per IP)
    - Return JWT token on successful authentication
    - _Requirements: 4.1, 4.3_
  
  - [x] 9.3 Create QrCodeController
    - GET /api/qrcode/daily - Get daily QR code for authenticated user
    - GET /api/qrcode/image - Get QR code image as PNG
    - Require JWT authentication
    - _Requirements: 5.1, 5.5_
  
  - [x] 9.4 Create MobileApiController
    - POST /api/mobil/login - Mobile authentication
    - POST /api/mobil/giris-cikis-kaydet - Record entry/exit event
    - Extract user from JWT token
    - Validate QR code and GPS coordinates
    - Add rate limiting (20 requests per minute per user)
    - _Requirements: 7.1, 7.2, 8.1, 8.2, 8.5_

- [x] 9.5 Write property test for external database query parameters
  - **Property 20: External database query parameters**
  - **Validates: Requirements 10.2**

- [x] 9.6 Write integration tests for REST API endpoints
  - Test registration flow end-to-end
  - Test authentication flow
  - Test QR code generation and retrieval
  - Test entry/exit recording
  - Test error responses for invalid inputs
  - _Requirements: 1.1, 4.1, 5.1, 8.5_

- [x] 10. Implement global exception handling
  - [x] 10.1 Create custom exception classes
    - Create ValidationException for validation errors
    - Create AuthenticationException for auth failures
    - Create ExternalServiceException for external service failures
    - Create ResourceNotFoundException for not found errors
    - _Requirements: 1.3, 2.3, 3.3, 4.4_
  
  - [x] 10.2 Create GlobalExceptionHandler
    - Handle ValidationException → HTTP 400
    - Handle AuthenticationException → HTTP 401
    - Handle ResourceNotFoundException → HTTP 404
    - Handle ExternalServiceException → HTTP 503
    - Handle general Exception → HTTP 500
    - Return consistent error response format with timestamp, status, message, path
    - _Requirements: 1.3, 4.4, 7.3, 8.3, 10.4, 12.4_

- [x] 10.3 Write unit tests for exception handling
  - Test each exception type returns correct HTTP status
  - Test error response format
  - Test error message content
  - _Requirements: 1.3, 4.4, 7.3, 8.3_

- [x] 11. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 12. Implement web UI with Thymeleaf
  - [x] 12.1 Create registration page
    - Create registration.html with TC and Personnel No input form
    - Add OTP verification form
    - Add password creation form with validation feedback
    - Add client-side validation
    - _Requirements: 1.1, 2.1, 3.1_
  
  - [x] 12.2 Create login page
    - Create login.html with TC and password input
    - Add error message display
    - Redirect to QR code page on success
    - _Requirements: 4.1_
  
  - [x] 12.3 Create QR code display page
    - Create qrcode.html to display daily QR code
    - Show QR code image
    - Display usage count and validity date
    - Add auto-refresh for new day
    - _Requirements: 5.1, 5.5_
  
  - [x] 12.4 Create web controller for pages
    - Create WebController with GET mappings for pages
    - Handle authentication redirects
    - _Requirements: 4.1, 5.1_

- [x] 12.5 Write UI integration tests
  - Test registration page flow
  - Test login page
  - Test QR code display
  - _Requirements: 1.1, 4.1, 5.1_

- [x] 13. Configure application properties and environment
  - [x] 13.1 Configure database connections
    - Set up local MySQL datasource configuration
    - Set up external MySQL datasource configuration (read-only)
    - Configure JPA properties and Hibernate settings
    - _Requirements: 9.5, 10.1_
  
  - [x] 13.2 Configure security properties
    - Set JWT secret and expiration time
    - Configure SSL/HTTPS settings
    - Configure CORS allowed origins
    - _Requirements: 9.1, 9.3_
  
  - [x] 13.3 Configure external service properties
    - Set SMS gateway URL and API key
    - Configure OTP length and expiration
    - Configure QR code image size and format
    - _Requirements: 1.4, 1.5, 5.5_
  
  - [x] 13.4 Create environment-specific profiles
    - Create application-dev.properties for development
    - Create application-prod.properties for production
    - Document required environment variables
    - _Requirements: 9.1, 10.1_

- [x] 14. Add logging and monitoring
  - [x] 14.1 Configure logging
    - Set up Logback configuration with JSON format
    - Configure log levels for different packages
    - Add sensitive data masking for passwords and OTP codes
    - _Requirements: 9.2, 10.4_
  
  - [x] 14.2 Add business event logging
    - Log registration attempts and completions
    - Log authentication attempts
    - Log QR code generation
    - Log entry/exit recordings
    - _Requirements: 1.1, 4.1, 5.1, 8.5_

- [x] 15. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
