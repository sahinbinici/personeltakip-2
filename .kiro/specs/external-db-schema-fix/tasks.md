# Implementation Plan

- [x] 1. Fix ExternalPersonnel entity schema mapping
  - Remove the `personnelNoRaw` field and its `@Column(name = "personnel_no_raw")` annotation from ExternalPersonnel.java
  - Update `getPersonnelNo()` method to always derive from `esicno` field (remove null check for personnelNoRaw)
  - Update `setPersonnelNo()` method to only set `esicno` field (remove personnelNoRaw assignment)
  - Remove the `telefo` field and its `@Column(name = "telefo")` annotation (column doesn't exist in external DB)
  - Update `getMobilePhone()` and `setMobilePhone()` to be no-op methods
  - Ensure all other fields and methods remain unchanged
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.3_

- [ ]* 1.1 Write unit tests for ExternalPersonnel helper methods
  - Create ExternalPersonnelTest.java test class
  - Test getPersonnelNo() with valid esicno value
  - Test getPersonnelNo() with null esicno value
  - Test setPersonnelNo() with valid numeric string
  - Test setPersonnelNo() with alphanumeric string
  - Test setPersonnelNo() with null and empty string
  - _Requirements: 2.3, 2.4_

- [ ]* 1.2 Write property-based test for personnel number round trip
  - Create ExternalPersonnelPropertyTest.java test class
  - **Property 2: Personnel Number Derivation**
  - **Validates: Requirements 1.3, 2.3**
  - Generate random Long values for esicno
  - Verify getPersonnelNo() returns correct string representation
  - Verify round-trip conversion maintains value integrity

- [ ]* 1.3 Write property-based test for setter-getter consistency
  - **Property 3: Personnel Number Parsing**
  - **Validates: Requirements 2.3**
  - Generate random numeric strings
  - Verify setPersonnelNo() correctly parses and sets esicno
  - Verify getPersonnelNo() returns consistent value

- [x] 2. Verify existing tests pass without modification
  - Run RegistrationServiceTest to ensure all tests pass
  - Verify tests use builder pattern with actual database columns
  - Confirm no test failures due to entity changes
  - _Requirements: 2.4_

- [x] 3. Test external database query execution
  - Start the application in dev mode
  - Verify no SQL syntax errors in logs during startup
  - Application starts successfully without schema errors
  - _Requirements: 1.1, 1.2, 1.4, 1.5, 3.1, 3.2_
  - _Note: Full validation test requires authentication setup_

- [x] 4. Checkpoint - Ensure all tests pass
  - All unit tests pass successfully
  - Application starts without SQL errors
  - Entity schema now matches external database structure
