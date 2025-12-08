# Requirements Document

## Introduction

This document specifies the requirements for a Personnel Tracking System that enables personnel registration, daily QR code generation, and secure entry/exit tracking through mobile application integration. The system consists of a web application for personnel management and REST API services for mobile application communication.

## Glossary

- **Personnel Tracking System**: The complete system including web application and mobile API services
- **Normal User**: A personnel member who can register, login, and view daily QR codes
- **Admin**: A system administrator with management and reporting capabilities (role definition only in this phase)
- **Super Admin**: A system administrator with full system configuration rights (role definition only in this phase)
- **Local Database**: The MySQL database that stores personnel credentials, QR codes, and entry/exit records
- **External Database**: A read-only MySQL database containing personnel master data
- **QR Code Value**: A unique daily identifier generated for each personnel member
- **OTP**: One-Time Password sent via SMS for verification
- **JWT**: JSON Web Token used for session management
- **Entry/Exit Record**: A timestamped record of personnel movement with location data

## Requirements

### Requirement 1: Personnel Registration

**User Story:** As a personnel member, I want to register in the system using my TC ID and Personnel Number, so that I can access the QR code generation features.

#### Acceptance Criteria

1. WHEN a user submits TC ID and Personnel Number THEN the Personnel Tracking System SHALL query the External Database for matching personnel master data
2. WHEN the External Database returns valid personnel data THEN the Personnel Tracking System SHALL extract User ID, First Name, Last Name, Personnel Number, and Mobile Phone fields
3. IF the External Database does not return matching data THEN the Personnel Tracking System SHALL reject the registration and display an error message
4. WHEN valid personnel data is retrieved THEN the Personnel Tracking System SHALL generate a six-digit OTP with five-minute expiration time
5. WHEN an OTP is generated THEN the Personnel Tracking System SHALL send the OTP via SMS to the Mobile Phone number from the External Database

### Requirement 2: SMS Verification

**User Story:** As a personnel member, I want to verify my mobile phone number with an SMS code, so that the system can confirm my identity.

#### Acceptance Criteria

1. WHEN a user submits an OTP THEN the Personnel Tracking System SHALL validate the code against the stored OTP and expiration time
2. IF the submitted OTP matches and has not expired THEN the Personnel Tracking System SHALL mark the verification as successful
3. IF the submitted OTP does not match or has expired THEN the Personnel Tracking System SHALL reject the verification and display an error message
4. WHEN OTP verification succeeds THEN the Personnel Tracking System SHALL allow the user to proceed to password creation
5. WHEN an OTP expires THEN the Personnel Tracking System SHALL remove the expired OTP from temporary storage

### Requirement 3: Password Creation

**User Story:** As a personnel member, I want to create a secure password, so that I can protect my account from unauthorized access.

#### Acceptance Criteria

1. WHEN a user submits a password THEN the Personnel Tracking System SHALL validate minimum length of eight characters
2. WHEN a user submits a password THEN the Personnel Tracking System SHALL validate presence of at least one uppercase letter, one lowercase letter, and one special character
3. IF password validation fails THEN the Personnel Tracking System SHALL reject the password and display specific validation error messages
4. WHEN a valid password is submitted THEN the Personnel Tracking System SHALL hash the password using BCrypt with salt
5. WHEN password hashing completes THEN the Personnel Tracking System SHALL store the hashed password in the Local Database with Normal User role assignment

### Requirement 4: User Authentication

**User Story:** As a registered personnel member, I want to login with my TC ID and password, so that I can access my daily QR code.

#### Acceptance Criteria

1. WHEN a user submits TC ID and password THEN the Personnel Tracking System SHALL query the Local Database for matching credentials
2. WHEN credentials are found THEN the Personnel Tracking System SHALL verify the submitted password against the stored BCrypt hash
3. IF password verification succeeds THEN the Personnel Tracking System SHALL generate a JWT token with user identity and role information
4. IF password verification fails THEN the Personnel Tracking System SHALL reject the login attempt and display an error message
5. WHEN a JWT token is generated THEN the Personnel Tracking System SHALL return the token to the user with thirty-minute expiration time

### Requirement 5: Daily QR Code Generation

**User Story:** As a logged-in personnel member, I want to view my daily QR code, so that I can use it for entry and exit tracking.

#### Acceptance Criteria

1. WHEN a Normal User accesses the QR code page THEN the Personnel Tracking System SHALL generate a unique QR Code Value for the current date
2. WHEN generating a QR Code Value THEN the Personnel Tracking System SHALL ensure uniqueness by combining Personnel ID, current date, and random salt
3. WHEN a QR Code Value is generated THEN the Personnel Tracking System SHALL set validity period from 00:00 to 23:59 of the current date
4. WHEN a QR Code Value is created THEN the Personnel Tracking System SHALL initialize usage counter to zero with maximum usage limit of two
5. WHEN displaying the QR code THEN the Personnel Tracking System SHALL render the QR Code Value as a scannable QR code image

### Requirement 6: QR Code Usage Tracking

**User Story:** As a system, I want to track QR code usage, so that each code can only be used twice per day (once for entry, once for exit).

#### Acceptance Criteria

1. WHEN a QR Code Value is used THEN the Personnel Tracking System SHALL increment the usage counter by one
2. WHEN checking QR Code Value validity THEN the Personnel Tracking System SHALL verify the usage counter is less than two
3. IF the usage counter equals two THEN the Personnel Tracking System SHALL reject further usage attempts
4. WHEN the date changes to the next day THEN the Personnel Tracking System SHALL invalidate all previous day QR Code Values
5. WHILE a QR Code Value has usage counter of zero THEN the Personnel Tracking System SHALL classify the next usage as Entry

### Requirement 7: Mobile Application Authentication API

**User Story:** As a mobile application, I want to authenticate users via API, so that authorized personnel can record entry/exit events.

#### Acceptance Criteria

1. WHEN the mobile application sends POST request to /api/mobil/login with TC ID and password THEN the Personnel Tracking System SHALL validate credentials against the Local Database
2. IF credentials are valid THEN the Personnel Tracking System SHALL generate a JWT token and return it in the response
3. IF credentials are invalid THEN the Personnel Tracking System SHALL return HTTP 401 status with error message
4. WHEN generating mobile JWT token THEN the Personnel Tracking System SHALL include Personnel ID and role in token payload
5. WHEN returning authentication response THEN the Personnel Tracking System SHALL use HTTPS protocol for secure transmission

### Requirement 8: Entry/Exit Recording API

**User Story:** As a mobile application, I want to submit entry/exit records with QR code data, so that personnel movements are tracked accurately.

#### Acceptance Criteria

1. WHEN the mobile application sends POST request to /api/mobil/giris-cikis-kaydet THEN the Personnel Tracking System SHALL extract JWT token from request headers
2. WHEN processing entry/exit request THEN the Personnel Tracking System SHALL validate QR Code Value against current date and Personnel ID from token
3. IF QR Code Value is invalid or usage counter equals two THEN the Personnel Tracking System SHALL return HTTP 400 status with error message
4. WHEN QR Code Value validation succeeds THEN the Personnel Tracking System SHALL determine entry or exit type based on usage counter value
5. WHEN recording entry/exit event THEN the Personnel Tracking System SHALL store Personnel ID, Type, Timestamp, GPS Coordinates, and QR Code Value in the Local Database

### Requirement 9: Security and Communication

**User Story:** As a system administrator, I want all API communications to be secure, so that sensitive personnel data is protected.

#### Acceptance Criteria

1. WHEN any API endpoint receives a request THEN the Personnel Tracking System SHALL enforce HTTPS protocol
2. WHEN storing passwords THEN the Personnel Tracking System SHALL apply BCrypt hashing with salt
3. WHEN generating JWT tokens THEN the Personnel Tracking System SHALL sign tokens with secret key and set expiration time
4. WHEN validating JWT tokens THEN the Personnel Tracking System SHALL verify signature and expiration before processing requests
5. WHEN connecting to External Database THEN the Personnel Tracking System SHALL use read-only database credentials

### Requirement 10: External Database Integration

**User Story:** As the system, I want to retrieve personnel master data from an external database, so that registration uses verified personnel information.

#### Acceptance Criteria

1. WHEN querying External Database THEN the Personnel Tracking System SHALL use read-only connection credentials
2. WHEN searching for personnel THEN the Personnel Tracking System SHALL query by TC ID and Personnel Number combination
3. WHEN External Database query succeeds THEN the Personnel Tracking System SHALL extract User ID, First Name, Last Name, Personnel Number, and Mobile Phone fields
4. IF External Database connection fails THEN the Personnel Tracking System SHALL log the error and return service unavailable message
5. WHEN External Database returns no matching records THEN the Personnel Tracking System SHALL return not found error to the user

### Requirement 11: Role-Based Access Control

**User Story:** As a system architect, I want to implement role-based access control, so that future admin and super admin features can be added securely.

#### Acceptance Criteria

1. WHEN creating a new user account THEN the Personnel Tracking System SHALL assign Normal User role by default
2. WHEN storing user data THEN the Personnel Tracking System SHALL include role field in the Local Database user table
3. WHEN generating JWT tokens THEN the Personnel Tracking System SHALL include user role in token payload
4. WHEN validating API requests THEN the Personnel Tracking System SHALL extract role from JWT token for authorization checks
5. WHILE implementing Normal User features THEN the Personnel Tracking System SHALL prepare role structure for Admin and Super Admin roles

### Requirement 12: Data Persistence

**User Story:** As a system, I want to persist all entry/exit records with location data, so that personnel movements can be tracked and reported.

#### Acceptance Criteria

1. WHEN an entry/exit event is recorded THEN the Personnel Tracking System SHALL store Personnel ID, Event Type, Timestamp, GPS Latitude, GPS Longitude, and QR Code Value
2. WHEN storing GPS coordinates THEN the Personnel Tracking System SHALL validate latitude range between -90 and 90 degrees
3. WHEN storing GPS coordinates THEN the Personnel Tracking System SHALL validate longitude range between -180 and 180 degrees
4. WHEN database write operation fails THEN the Personnel Tracking System SHALL return error response to mobile application
5. WHEN entry/exit record is successfully saved THEN the Personnel Tracking System SHALL return HTTP 200 status with success message
