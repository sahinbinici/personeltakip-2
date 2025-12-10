# Requirements Document

## Introduction

The personnel tracking system is experiencing a critical database error when attempting to validate personnel during registration. The application's JPA entity `ExternalPersonnel` is attempting to query a column `personnel_no_raw` that does not exist in the external database's `person` table, causing SQL syntax errors and triggering circuit breaker fallbacks.

## Glossary

- **External Database**: The read-only MySQL database at 193.140.136.45 containing personnel master data
- **ExternalPersonnel Entity**: JPA entity class that maps to the external database's `person` table
- **Circuit Breaker**: Resilience4j pattern that opens after repeated failures to prevent cascading failures
- **Personnel Number**: The employee identifier (esicno) stored as a Long in the database
- **TC Number**: Turkish Citizen ID Number (tckiml), an 11-digit identifier

## Requirements

### Requirement 1

**User Story:** As a system administrator, I want the application to correctly map to the external database schema, so that personnel validation queries succeed without errors.

#### Acceptance Criteria

1. WHEN the system queries the external database THEN the system SHALL only reference columns that exist in the actual database schema
2. WHEN the ExternalPersonnel entity is used in queries THEN the system SHALL NOT include the personnel_no_raw column in SELECT statements
3. WHEN personnel data is retrieved THEN the system SHALL derive the personnel number from the esicno column
4. WHEN the application starts THEN the system SHALL successfully connect to the external database without schema validation errors
5. WHEN a user attempts to register THEN the system SHALL successfully validate their TC number and personnel number against the external database

### Requirement 2

**User Story:** As a developer, I want the ExternalPersonnel entity to accurately reflect the external database schema, so that the application remains maintainable and error-free.

#### Acceptance Criteria

1. WHEN the ExternalPersonnel entity is defined THEN the system SHALL only include @Column annotations for columns that exist in the external person table
2. WHEN test data is created THEN the system SHALL use the actual database columns (esicno, tckiml, peradi, soyadi, brkodu, unvkod, telefo)
3. WHEN the personnelNoRaw field is removed THEN the system SHALL maintain backward compatibility through the getPersonnelNo() method
4. WHEN existing tests are run THEN the system SHALL pass all tests without modification to test logic

### Requirement 3

**User Story:** As a user attempting to register, I want the system to validate my credentials quickly and reliably, so that I can complete registration without delays or errors.

#### Acceptance Criteria

1. WHEN a user submits their TC number and personnel number THEN the system SHALL query the external database within 2 seconds
2. WHEN the external database query succeeds THEN the system SHALL return validation results without triggering the circuit breaker
3. WHEN validation fails due to incorrect credentials THEN the system SHALL provide clear error messages to the user
4. WHEN the circuit breaker is currently open THEN the system SHALL provide a user-friendly message about temporary unavailability
