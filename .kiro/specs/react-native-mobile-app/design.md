# React Native Mobile App Design Document

## Overview

Bu doküman, Personnel Tracking System için React Native mobil uygulamasının teknik tasarımını açıklar. Uygulama, cross-platform (iOS/Android) çalışacak şekilde tasarlanmış olup, QR kod tarama, GPS konum takibi, offline çalışma ve güvenli veri saklama özelliklerini içerir.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │   Login Screen  │  │   QR Scanner    │  │   Records   │ │
│  │                 │  │     Screen      │  │   Screen    │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │  Excuse Screen  │  │ Settings Screen │  │Profile Screen│ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │ Notifications   │  │ Monthly Reports │  │Admin Panel  │ │
│  │    Screen       │  │    Screen       │  │   Screen    │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Business Logic Layer                    │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │ Authentication  │  │   QR Scanner    │  │   Location  │ │
│  │    Service      │  │    Service      │  │   Service   │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │   API Service   │  │ Offline Sync    │  │  Security   │ │
│  │                 │  │    Service      │  │   Service   │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │ Notification    │  │   Report        │  │    Admin    │ │
│  │   Service       │  │  Service        │  │   Service   │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                     Data Layer                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │   SQLite DB     │  │  Secure Storage │  │    Cache    │ │
│  │  (Offline Data) │  │  (Credentials)  │  │   Manager   │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    External Services                       │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │   Backend API   │  │   GPS Service   │  │   Camera    │ │
│  │  (Spring Boot)  │  │                 │  │   Service   │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Technology Stack

### Core Framework
- **React Native 0.73+**: Cross-platform mobil uygulama framework'ü
- **TypeScript**: Type-safe JavaScript development
- **React Navigation 6**: Navigation ve routing

### State Management
- **Redux Toolkit**: Global state management
- **RTK Query**: API data fetching ve caching
- **React Hook Form**: Form state management

### Database & Storage
- **SQLite (react-native-sqlite-storage)**: Offline veri saklama
- **React Native Keychain**: Güvenli credential storage
- **AsyncStorage**: Basit key-value storage

### Device Features
- **react-native-qrcode-scanner**: QR kod tarama
- **@react-native-geolocation/geolocation**: GPS konum servisi
- **react-native-biometrics**: Biometric authentication
- **react-native-permissions**: Device permissions yönetimi

### Network & Security
- **Axios**: HTTP client
- **react-native-ssl-pinning**: SSL certificate pinning
- **crypto-js**: Data encryption/decryption

## Components and Interfaces

### 1. Authentication Service
```typescript
interface AuthenticationService {
  login(tcNo: string, password: string): Promise<AuthResult>
  logout(): Promise<void>
  refreshToken(): Promise<string>
  isAuthenticated(): boolean
  getBiometricAuth(): Promise<boolean>
}
```

### 2. QR Scanner Service
```typescript
interface QRScannerService {
  scanQRCode(): Promise<QRScanResult>
  validateQRCode(qrData: string): boolean
  requestCameraPermission(): Promise<boolean>
}
```

### 3. Location Service
```typescript
interface LocationService {
  getCurrentLocation(): Promise<LocationData>
  requestLocationPermission(): Promise<boolean>
  isLocationEnabled(): Promise<boolean>
}
```

### 4. API Service
```typescript
interface APIService {
  login(credentials: LoginRequest): Promise<AuthTokenDto>
  recordEntryExit(data: EntryExitRequest): Promise<EntryExitRecord>
  submitExcuse(excuse: ExcuseRequest): Promise<ExcuseResponse>
  getRecords(dateRange: DateRange): Promise<EntryExitRecord[]>
}
```

### 5. Offline Sync Service
```typescript
interface OfflineSyncService {
  saveOfflineRecord(record: OfflineRecord): Promise<void>
  syncPendingRecords(): Promise<SyncResult>
  getPendingRecordsCount(): Promise<number>
  clearSyncedRecords(): Promise<void>
}
```

### 6. Notification Service
```typescript
interface NotificationService {
  initializePushNotifications(): Promise<void>
  requestNotificationPermission(): Promise<boolean>
  subscribeToExcuseNotifications(adminId: number): Promise<void>
  handleIncomingNotification(notification: PushNotification): void
  getNotificationHistory(): Promise<NotificationItem[]>
  markNotificationAsRead(notificationId: string): Promise<void>
}
```

### 7. Report Service
```typescript
interface ReportService {
  getMonthlyReport(userId: number, year: number, month: number): Promise<MonthlyReport>
  getDepartmentReports(departmentId: number, year: number, month: number): Promise<DepartmentReport[]>
  exportReportToPDF(report: MonthlyReport): Promise<string>
  getReportSummary(userId: number, dateRange: DateRange): Promise<ReportSummary>
}
```

### 8. Admin Service
```typescript
interface AdminService {
  getDepartmentPersonnel(departmentId: number): Promise<PersonnelInfo[]>
  getExcuseNotifications(adminId: number): Promise<ExcuseNotification[]>
  updateExcuseStatus(excuseId: number, status: ExcuseStatus): Promise<void>
  isUserAdmin(userId: number): Promise<boolean>
  getDepartmentStatistics(departmentId: number): Promise<DepartmentStats>
}
```

## Data Models

### Authentication Models
```typescript
interface LoginRequest {
  tcNo: string
  password: string
  rememberMe?: boolean
}

interface AuthTokenDto {
  token: string
  refreshToken: string
  expiresIn: number
  user: UserProfile
}

interface UserProfile {
  id: number
  tcNo: string
  firstName: string
  lastName: string
  department: string
  role: string
}
```

### Entry/Exit Models
```typescript
interface EntryExitRequest {
  qrCodeValue: string
  timestamp: string
  latitude: number
  longitude: number
}

interface EntryExitRecord {
  id: number
  userId: number
  qrCodeValue: string
  timestamp: string
  latitude: number
  longitude: number
  recordType: 'ENTRY' | 'EXIT'
  status: 'SUCCESS' | 'FAILED'
  ipAddress?: string
}

interface OfflineRecord {
  id: string
  qrCodeValue: string
  timestamp: string
  latitude: number
  longitude: number
  synced: boolean
  createdAt: string
}
```

### Excuse Models
```typescript
interface ExcuseRequest {
  excuseType: ExcuseType
  description: string
  date: string
  attachments?: string[]
}

interface ExcuseType {
  id: number
  name: string
  requiresDescription: boolean
  requiresAttachment: boolean
}
```

### Notification Models
```typescript
interface PushNotification {
  id: string
  title: string
  body: string
  data: NotificationData
  timestamp: string
}

interface NotificationData {
  type: 'EXCUSE_NOTIFICATION' | 'SYSTEM_NOTIFICATION'
  excuseId?: number
  userId?: number
  departmentId?: number
}

interface NotificationItem {
  id: string
  title: string
  message: string
  timestamp: string
  isRead: boolean
  type: string
  actionData?: any
}

interface ExcuseNotification {
  id: number
  excuseId: number
  userId: number
  userName: string
  excuseType: string
  description: string
  submittedAt: string
  status: ExcuseStatus
}

enum ExcuseStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED'
}
```

### Report Models
```typescript
interface MonthlyReport {
  userId: number
  userName: string
  year: number
  month: number
  totalWorkingDays: number
  presentDays: number
  absentDays: number
  lateArrivals: number
  totalWorkingHours: number
  averageWorkingHours: number
  dailyRecords: DailyRecord[]
}

interface DailyRecord {
  date: string
  entryTime?: string
  exitTime?: string
  workingHours?: number
  status: 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSE'
  location?: LocationInfo
}

interface DepartmentReport {
  userId: number
  userName: string
  monthlyReport: MonthlyReport
}

interface ReportSummary {
  totalDays: number
  workingDays: number
  presentDays: number
  absentDays: number
  excuseDays: number
  totalHours: number
  averageHours: number
}

interface LocationInfo {
  latitude: number
  longitude: number
  address?: string
}
```

### Admin Models
```typescript
interface PersonnelInfo {
  id: number
  tcNo: string
  firstName: string
  lastName: string
  department: string
  position: string
  isActive: boolean
}

interface DepartmentStats {
  totalPersonnel: number
  presentToday: number
  absentToday: number
  pendingExcuses: number
  monthlyAttendanceRate: number
}
```

## Screen Designs

### 1. Login Screen
- TC Kimlik No input field
- Password input field (secure)
- "Beni Hatırla" checkbox
- Biometric login button (if available)
- Login button
- Forgot password link

### 2. Main Dashboard
- Welcome message with user name
- QR Code Scan button (primary action)
- Quick stats (today's records)
- Pending sync indicator
- Navigation tabs (Records, Excuse, Settings)

### 3. QR Scanner Screen
- Camera preview
- QR code scanning overlay
- Flash toggle button
- Manual entry option
- Cancel button

### 4. Records Screen
- Date range picker
- Records list with filters
- Pull-to-refresh functionality
- Pagination for large datasets
- Export options

### 5. Excuse Screen
- Excuse type dropdown
- Date picker
- Description text area
- Attachment upload
- Submit button

### 6. Settings Screen
- Profile information
- Notification preferences
- Language selection
- Biometric settings
- About/Version info
- Logout button

### 7. Notifications Screen
- Notification list with timestamps
- Mark as read/unread functionality
- Filter by notification type
- Clear all notifications option
- Notification details view

### 8. Monthly Reports Screen
- Month/Year picker
- Report summary cards (total days, working hours, etc.)
- Daily records calendar view
- Export to PDF button
- Share report functionality

### 9. Admin Panel Screen (Admin Users Only)
- Department personnel list
- Excuse notifications badge
- Quick stats dashboard
- Personnel report access
- Notification management

### 10. Admin Personnel Reports Screen
- Personnel selection dropdown
- Month/Year picker
- Individual personnel report view
- Comparison charts
- Export functionality

### 11. Admin Excuse Management Screen
- Pending excuse notifications
- Excuse details with personnel info
- Approve/Reject buttons
- Excuse history
- Bulk actions

## Security Implementation

### 1. Data Encryption
```typescript
class SecurityService {
  encryptData(data: string): string
  decryptData(encryptedData: string): string
  generateSecureKey(): string
  validateSSLCertificate(url: string): boolean
}
```

### 2. Biometric Authentication
```typescript
class BiometricService {
  isBiometricAvailable(): Promise<boolean>
  authenticateWithBiometric(): Promise<boolean>
  enableBiometric(): Promise<void>
  disableBiometric(): Promise<void>
}
```

### 3. SSL Pinning
```typescript
const sslPinningConfig = {
  hostname: 'api.personeltakip.com',
  publicKeyHashes: ['sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=']
}
```

## Offline Functionality

### 1. SQLite Database Schema
```sql
CREATE TABLE offline_records (
  id TEXT PRIMARY KEY,
  qr_code_value TEXT NOT NULL,
  timestamp TEXT NOT NULL,
  latitude REAL NOT NULL,
  longitude REAL NOT NULL,
  synced INTEGER DEFAULT 0,
  created_at TEXT NOT NULL,
  retry_count INTEGER DEFAULT 0
);

CREATE TABLE cached_records (
  id INTEGER PRIMARY KEY,
  user_id INTEGER NOT NULL,
  record_data TEXT NOT NULL,
  cached_at TEXT NOT NULL
);
```

### 2. Sync Strategy
- Background sync when network available
- Retry mechanism with exponential backoff
- Conflict resolution for duplicate records
- User notification for sync status

## Performance Optimization

### 1. Image Optimization
- Lazy loading for record images
- Image compression before upload
- Caching strategy for frequently accessed images

### 2. Network Optimization
- Request batching for multiple operations
- Response caching with TTL
- Compression for API requests/responses

### 3. Memory Management
- Proper cleanup of camera resources
- Efficient list rendering with FlatList
- Image memory management

## Testing Strategy

### Unit Testing
- Service layer testing with Jest
- Component testing with React Native Testing Library
- Mock implementations for device features

### Integration Testing
- API integration tests
- Database operation tests
- Navigation flow tests

### E2E Testing
- Detox for end-to-end testing
- Critical user journey testing
- Device-specific testing

## Deployment Strategy

### Development
- Metro bundler for development
- Hot reloading for faster development
- Debug builds with detailed logging

### Production
- Code obfuscation and minification
- Bundle splitting for optimal loading
- Crash reporting with Crashlytics
- Performance monitoring

### App Store Distribution
- iOS App Store deployment
- Google Play Store deployment
- Over-the-air updates with CodePush