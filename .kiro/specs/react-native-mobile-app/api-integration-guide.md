# API Integration Guide

## Backend API Endpoints

Bu doküman, React Native mobil uygulamasının mevcut Spring Boot backend API'si ile entegrasyonu için gerekli endpoint'leri ve kullanım örneklerini içerir.

## Base Configuration

```typescript
// API Configuration
const API_CONFIG = {
  BASE_URL: 'http://localhost:8080', // Development
  // BASE_URL: 'https://api.personeltakip.com', // Production
  TIMEOUT: 30000,
  HEADERS: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  }
}
```

## Authentication Endpoints

### 1. Mobile Login
**Endpoint:** `POST /api/mobil/login`

**Request:**
```typescript
interface LoginRequest {
  tcNo: string      // TC Kimlik Numarası
  password: string  // Şifre
}
```

**Response:**
```typescript
interface AuthTokenDto {
  token: string           // JWT Token
  refreshToken: string    // Refresh Token
  expiresIn: number      // Token süre (saniye)
  user: {
    id: number
    tcNo: string
    firstName: string
    lastName: string
    department: string
    role: string
  }
}
```

**Usage Example:**
```typescript
const login = async (tcNo: string, password: string) => {
  try {
    const response = await axios.post('/api/mobil/login', {
      tcNo,
      password
    })
    
    // JWT token'ı güvenli storage'a kaydet
    await SecureStorage.setItem('jwt_token', response.data.token)
    await SecureStorage.setItem('refresh_token', response.data.refreshToken)
    
    return response.data
  } catch (error) {
    throw new Error('Login failed: ' + error.response?.data?.message)
  }
}
```

## Entry/Exit Recording

### 2. Record Entry/Exit
**Endpoint:** `POST /api/mobil/giris-cikis-kaydet`

**Headers:**
```
Authorization: Bearer {JWT_TOKEN}
```

**Request:**
```typescript
interface EntryExitRequestDto {
  qrCodeValue: string    // QR kod değeri
  timestamp: string      // ISO 8601 format (2024-12-16T10:30:00.000Z)
  latitude: number       // GPS enlem (-90 to 90)
  longitude: number      // GPS boylam (-180 to 180)
}
```

**Response:**
```typescript
interface EntryExitRecordDto {
  id: number
  userId: number
  qrCodeValue: string
  timestamp: string
  latitude: number
  longitude: number
  recordType: 'ENTRY' | 'EXIT'
  status: 'SUCCESS' | 'FAILED'
  ipAddress?: string
  message?: string
}
```

**Usage Example:**
```typescript
const recordEntryExit = async (qrCode: string, location: LocationData) => {
  try {
    const token = await SecureStorage.getItem('jwt_token')
    
    const response = await axios.post('/api/mobil/giris-cikis-kaydet', {
      qrCodeValue: qrCode,
      timestamp: new Date().toISOString(),
      latitude: location.latitude,
      longitude: location.longitude
    }, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    return response.data
  } catch (error) {
    if (error.response?.status === 429) {
      throw new Error('Too many requests. Please try again later.')
    }
    throw new Error('Recording failed: ' + error.response?.data?.message)
  }
}
```

## QR Code Validation

### 3. QR Code Validation (Optional - if separate endpoint exists)
**Endpoint:** `POST /api/qrcode/validate`

**Headers:**
```
Authorization: Bearer {JWT_TOKEN}
```

**Request:**
```typescript
interface QrCodeValidationRequest {
  qrCodeValue: string
  userId: number
}
```

**Response:**
```typescript
interface QrCodeValidationDto {
  valid: boolean
  message: string
  expiresAt?: string
}
```

## User Records

### 4. Get User Records (if endpoint exists)
**Endpoint:** `GET /api/mobil/records`

**Headers:**
```
Authorization: Bearer {JWT_TOKEN}
```

**Query Parameters:**
```typescript
interface RecordsQuery {
  startDate?: string    // YYYY-MM-DD
  endDate?: string      // YYYY-MM-DD
  page?: number         // Sayfa numarası (0'dan başlar)
  size?: number         // Sayfa boyutu (varsayılan: 20)
}
```

**Response:**
```typescript
interface RecordsResponse {
  content: EntryExitRecordDto[]
  totalElements: number
  totalPages: number
  currentPage: number
  hasNext: boolean
}
```

## Error Handling

### Common Error Responses

```typescript
interface ErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
}
```

### HTTP Status Codes

- **200 OK**: Başarılı işlem
- **400 Bad Request**: Geçersiz istek (validation hatası)
- **401 Unauthorized**: Kimlik doğrulama gerekli
- **403 Forbidden**: Yetkisiz erişim
- **429 Too Many Requests**: Rate limit aşıldı
- **500 Internal Server Error**: Sunucu hatası

### Error Handling Implementation

```typescript
class APIService {
  private async handleRequest<T>(request: () => Promise<AxiosResponse<T>>): Promise<T> {
    try {
      const response = await request()
      return response.data
    } catch (error) {
      if (axios.isAxiosError(error)) {
        const status = error.response?.status
        const message = error.response?.data?.message || error.message
        
        switch (status) {
          case 401:
            // Token süresi dolmuş, yeniden login gerekli
            await this.logout()
            throw new Error('Session expired. Please login again.')
          
          case 403:
            throw new Error('Access denied. Insufficient permissions.')
          
          case 429:
            throw new Error('Too many requests. Please try again later.')
          
          case 500:
            throw new Error('Server error. Please try again later.')
          
          default:
            throw new Error(message || 'An unexpected error occurred.')
        }
      }
      throw error
    }
  }
}
```

## JWT Token Management

### Token Interceptor Setup

```typescript
// Request interceptor - JWT token'ı otomatik ekle
axios.interceptors.request.use(
  async (config) => {
    const token = await SecureStorage.getItem('jwt_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor - Token yenileme
axios.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      
      try {
        const refreshToken = await SecureStorage.getItem('refresh_token')
        if (refreshToken) {
          // Refresh token ile yeni token al
          const response = await axios.post('/api/auth/refresh', {
            refreshToken
          })
          
          const newToken = response.data.token
          await SecureStorage.setItem('jwt_token', newToken)
          
          // Orijinal isteği yeni token ile tekrarla
          originalRequest.headers.Authorization = `Bearer ${newToken}`
          return axios(originalRequest)
        }
      } catch (refreshError) {
        // Refresh token da geçersiz, logout yap
        await SecureStorage.removeItem('jwt_token')
        await SecureStorage.removeItem('refresh_token')
        // Navigate to login screen
      }
    }
    
    return Promise.reject(error)
  }
)
```

## Network Configuration

### SSL Pinning (Production)

```typescript
// react-native-ssl-pinning configuration
const sslPinningConfig = {
  hostname: 'api.personeltakip.com',
  publicKeyHashes: [
    'sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=' // Actual certificate hash
  ]
}

// SSL pinned request
import { fetch as sslFetch } from 'react-native-ssl-pinning'

const makeSecureRequest = async (url: string, options: RequestInit) => {
  try {
    const response = await sslFetch(url, {
      ...options,
      sslPinning: sslPinningConfig
    })
    return response
  } catch (error) {
    throw new Error('SSL verification failed')
  }
}
```

### Network Status Monitoring

```typescript
import NetInfo from '@react-native-netinfo/netinfo'

class NetworkService {
  static async isConnected(): Promise<boolean> {
    const state = await NetInfo.fetch()
    return state.isConnected && state.isInternetReachable
  }
  
  static subscribeToNetworkChanges(callback: (isConnected: boolean) => void) {
    return NetInfo.addEventListener(state => {
      callback(state.isConnected && state.isInternetReachable)
    })
  }
}
```

## Rate Limiting

Backend'de dakikada 20 istek limiti var. Mobil uygulamada bu limiti aşmamak için:

```typescript
class RateLimiter {
  private requests: number[] = []
  private readonly maxRequests = 20
  private readonly windowMs = 60000 // 1 dakika
  
  canMakeRequest(): boolean {
    const now = Date.now()
    
    // Eski istekleri temizle
    this.requests = this.requests.filter(time => now - time < this.windowMs)
    
    // Limit kontrolü
    if (this.requests.length >= this.maxRequests) {
      return false
    }
    
    this.requests.push(now)
    return true
  }
  
  getWaitTime(): number {
    if (this.requests.length === 0) return 0
    
    const oldestRequest = Math.min(...this.requests)
    const waitTime = this.windowMs - (Date.now() - oldestRequest)
    
    return Math.max(0, waitTime)
  }
}
```

## Admin Endpoints

### 1. Get Department Personnel
**Endpoint:** `GET /api/admin/personnel/department/{departmentId}`

**Headers:** `Authorization: Bearer {jwt_token}`

**Response:**
```typescript
interface PersonnelInfo[] {
  id: number
  tcNo: string
  firstName: string
  lastName: string
  department: string
  position: string
  isActive: boolean
}
```

### 2. Get Excuse Notifications
**Endpoint:** `GET /api/admin/excuses/notifications`

**Headers:** `Authorization: Bearer {jwt_token}`

**Response:**
```typescript
interface ExcuseNotification[] {
  id: number
  excuseId: number
  userId: number
  userName: string
  excuseType: string
  description: string
  submittedAt: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
}
```

### 3. Update Excuse Status
**Endpoint:** `PUT /api/admin/excuses/{excuseId}/status`

**Headers:** `Authorization: Bearer {jwt_token}`

**Request:**
```typescript
interface ExcuseStatusUpdate {
  status: 'APPROVED' | 'REJECTED'
  adminComment?: string
}
```

**Response:**
```typescript
interface ExcuseUpdateResponse {
  success: boolean
  message: string
  updatedExcuse: ExcuseNotification
}
```

## Report Endpoints

### 1. Get Monthly Report (User)
**Endpoint:** `GET /api/reports/monthly/{userId}?year={year}&month={month}`

**Headers:** `Authorization: Bearer {jwt_token}`

**Response:**
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
  location?: {
    latitude: number
    longitude: number
    address?: string
  }
}
```

### 2. Get Department Reports (Admin)
**Endpoint:** `GET /api/admin/reports/department/{departmentId}?year={year}&month={month}`

**Headers:** `Authorization: Bearer {jwt_token}`

**Response:**
```typescript
interface DepartmentReport[] {
  userId: number
  userName: string
  monthlyReport: MonthlyReport
}
```

### 3. Export Report to PDF
**Endpoint:** `POST /api/reports/export/pdf`

**Headers:** `Authorization: Bearer {jwt_token}`

**Request:**
```typescript
interface PDFExportRequest {
  reportType: 'MONTHLY' | 'DEPARTMENT'
  userId?: number
  departmentId?: number
  year: number
  month: number
}
```

**Response:**
```typescript
// Returns PDF file as binary data
Content-Type: application/pdf
Content-Disposition: attachment; filename="report_2024_12.pdf"
```

## Notification Endpoints

### 1. Register for Push Notifications
**Endpoint:** `POST /api/notifications/register`

**Headers:** `Authorization: Bearer {jwt_token}`

**Request:**
```typescript
interface NotificationRegistration {
  deviceToken: string
  platform: 'ios' | 'android'
  userId: number
}
```

### 2. Get Notification History
**Endpoint:** `GET /api/notifications/history?page={page}&size={size}`

**Headers:** `Authorization: Bearer {jwt_token}`

**Response:**
```typescript
interface NotificationHistory {
  content: NotificationItem[]
  totalElements: number
  totalPages: number
  currentPage: number
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
```

### 3. Mark Notification as Read
**Endpoint:** `PUT /api/notifications/{notificationId}/read`

**Headers:** `Authorization: Bearer {jwt_token}`

**Response:**
```typescript
interface NotificationUpdateResponse {
  success: boolean
  message: string
}
```

## WebSocket Integration for Real-time Notifications

### Connection Setup
```typescript
// WebSocket connection for real-time notifications
const wsUrl = 'ws://localhost:8080/ws/notifications'

const connectWebSocket = (token: string) => {
  const ws = new WebSocket(`${wsUrl}?token=${token}`)
  
  ws.onopen = () => {
    console.log('WebSocket connected')
  }
  
  ws.onmessage = (event) => {
    const notification = JSON.parse(event.data)
    handleIncomingNotification(notification)
  }
  
  ws.onclose = () => {
    console.log('WebSocket disconnected')
    // Implement reconnection logic
  }
  
  return ws
}
```

### Notification Message Format
```typescript
interface WebSocketNotification {
  type: 'EXCUSE_NOTIFICATION' | 'SYSTEM_NOTIFICATION'
  data: {
    id: string
    title: string
    body: string
    excuseId?: number
    userId?: number
    timestamp: string
  }
}
```

## Error Handling

### Standard Error Response Format
```typescript
interface APIError {
  error: string
  message: string
  timestamp: string
  path: string
  status: number
}
```

### Common Error Codes
- `401 Unauthorized`: JWT token geçersiz veya süresi dolmuş
- `403 Forbidden`: Yetkisiz erişim (admin endpoint'ine normal user erişimi)
- `404 Not Found`: Kaynak bulunamadı
- `429 Too Many Requests`: Rate limit aşıldı
- `500 Internal Server Error`: Sunucu hatası

## Implementation Examples

### Admin Service Implementation
```typescript
class AdminService {
  private apiClient: APIClient

  async getDepartmentPersonnel(departmentId: number): Promise<PersonnelInfo[]> {
    try {
      const response = await this.apiClient.get(
        `/api/admin/personnel/department/${departmentId}`
      )
      return response.data
    } catch (error) {
      throw new Error(`Failed to fetch department personnel: ${error.message}`)
    }
  }

  async updateExcuseStatus(excuseId: number, status: ExcuseStatus, comment?: string): Promise<void> {
    try {
      await this.apiClient.put(`/api/admin/excuses/${excuseId}/status`, {
        status,
        adminComment: comment
      })
    } catch (error) {
      throw new Error(`Failed to update excuse status: ${error.message}`)
    }
  }
}
```

### Report Service Implementation
```typescript
class ReportService {
  private apiClient: APIClient

  async getMonthlyReport(userId: number, year: number, month: number): Promise<MonthlyReport> {
    try {
      const response = await this.apiClient.get(
        `/api/reports/monthly/${userId}?year=${year}&month=${month}`
      )
      return response.data
    } catch (error) {
      throw new Error(`Failed to fetch monthly report: ${error.message}`)
    }
  }

  async exportToPDF(request: PDFExportRequest): Promise<string> {
    try {
      const response = await this.apiClient.post('/api/reports/export/pdf', request, {
        responseType: 'blob'
      })
      
      // Save PDF to device storage
      const fileName = `report_${request.year}_${request.month}.pdf`
      const filePath = await saveFileToDevice(response.data, fileName)
      return filePath
    } catch (error) {
      throw new Error(`Failed to export PDF: ${error.message}`)
    }
  }
}
```