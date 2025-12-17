# React Native Project Structure

## Folder Organization

```
PersonnelTrackingApp/
├── android/                          # Android native code
├── ios/                              # iOS native code
├── src/                              # Main source code
│   ├── components/                   # Reusable UI components
│   │   ├── common/                   # Common components
│   │   │   ├── Button/
│   │   │   │   ├── Button.tsx
│   │   │   │   ├── Button.styles.ts
│   │   │   │   └── index.ts
│   │   │   ├── Input/
│   │   │   ├── Loading/
│   │   │   ├── Modal/
│   │   │   └── index.ts
│   │   ├── forms/                    # Form-specific components
│   │   │   ├── LoginForm/
│   │   │   ├── ExcuseForm/
│   │   │   └── index.ts
│   │   └── index.ts
│   ├── screens/                      # Screen components
│   │   ├── Auth/
│   │   │   ├── LoginScreen/
│   │   │   │   ├── LoginScreen.tsx
│   │   │   │   ├── LoginScreen.styles.ts
│   │   │   │   └── index.ts
│   │   │   ├── BiometricScreen/
│   │   │   └── index.ts
│   │   ├── Main/
│   │   │   ├── DashboardScreen/
│   │   │   ├── QRScannerScreen/
│   │   │   ├── RecordsScreen/
│   │   │   ├── ExcuseScreen/
│   │   │   ├── SettingsScreen/
│   │   │   └── index.ts
│   │   └── index.ts
│   ├── navigation/                   # Navigation configuration
│   │   ├── AppNavigator.tsx
│   │   ├── AuthNavigator.tsx
│   │   ├── MainNavigator.tsx
│   │   ├── types.ts
│   │   └── index.ts
│   ├── services/                     # Business logic services
│   │   ├── api/
│   │   │   ├── apiClient.ts
│   │   │   ├── authApi.ts
│   │   │   ├── recordsApi.ts
│   │   │   ├── excuseApi.ts
│   │   │   └── index.ts
│   │   ├── auth/
│   │   │   ├── AuthService.ts
│   │   │   ├── BiometricService.ts
│   │   │   ├── SecureStorage.ts
│   │   │   └── index.ts
│   │   ├── location/
│   │   │   ├── LocationService.ts
│   │   │   ├── PermissionService.ts
│   │   │   └── index.ts
│   │   ├── qr/
│   │   │   ├── QRScannerService.ts
│   │   │   ├── QRValidationService.ts
│   │   │   └── index.ts
│   │   ├── offline/
│   │   │   ├── OfflineSyncService.ts
│   │   │   ├── DatabaseService.ts
│   │   │   ├── CacheService.ts
│   │   │   └── index.ts
│   │   ├── security/
│   │   │   ├── SecurityService.ts
│   │   │   ├── EncryptionService.ts
│   │   │   └── index.ts
│   │   └── index.ts
│   ├── store/                        # Redux store configuration
│   │   ├── slices/
│   │   │   ├── authSlice.ts
│   │   │   ├── recordsSlice.ts
│   │   │   ├── offlineSlice.ts
│   │   │   ├── settingsSlice.ts
│   │   │   └── index.ts
│   │   ├── api/
│   │   │   ├── authApi.ts
│   │   │   ├── recordsApi.ts
│   │   │   └── index.ts
│   │   ├── store.ts
│   │   ├── rootReducer.ts
│   │   └── index.ts
│   ├── types/                        # TypeScript type definitions
│   │   ├── api.ts
│   │   ├── auth.ts
│   │   ├── records.ts
│   │   ├── navigation.ts
│   │   ├── common.ts
│   │   └── index.ts
│   ├── utils/                        # Utility functions
│   │   ├── constants.ts
│   │   ├── helpers.ts
│   │   ├── validators.ts
│   │   ├── formatters.ts
│   │   ├── permissions.ts
│   │   └── index.ts
│   ├── hooks/                        # Custom React hooks
│   │   ├── useAuth.ts
│   │   ├── useLocation.ts
│   │   ├── useQRScanner.ts
│   │   ├── useOfflineSync.ts
│   │   ├── useBiometric.ts
│   │   └── index.ts
│   ├── styles/                       # Global styles and themes
│   │   ├── colors.ts
│   │   ├── typography.ts
│   │   ├── spacing.ts
│   │   ├── themes.ts
│   │   └── index.ts
│   ├── assets/                       # Static assets
│   │   ├── images/
│   │   ├── icons/
│   │   ├── fonts/
│   │   └── index.ts
│   ├── localization/                 # Multi-language support
│   │   ├── i18n.ts
│   │   ├── locales/
│   │   │   ├── tr.json
│   │   │   ├── en.json
│   │   │   └── index.ts
│   │   └── index.ts
│   └── App.tsx                       # Main App component
├── __tests__/                        # Test files
│   ├── components/
│   ├── screens/
│   ├── services/
│   ├── utils/
│   └── setup.ts
├── android/                          # Android configuration
├── ios/                              # iOS configuration
├── .env                              # Environment variables
├── .env.example                      # Environment variables template
├── babel.config.js                   # Babel configuration
├── metro.config.js                   # Metro bundler configuration
├── react-native.config.js            # React Native configuration
├── tsconfig.json                     # TypeScript configuration
├── package.json                      # Dependencies and scripts
└── README.md                         # Project documentation
```

## Key Files and Their Purposes

### 1. Main App Entry Point

**src/App.tsx**
```typescript
import React from 'react'
import { Provider } from 'react-redux'
import { PersistGate } from 'redux-persist/integration/react'
import { NavigationContainer } from '@react-navigation/native'
import { store, persistor } from './store'
import AppNavigator from './navigation/AppNavigator'
import { LoadingScreen } from './components/common'
import './localization/i18n'

const App: React.FC = () => {
  return (
    <Provider store={store}>
      <PersistGate loading={<LoadingScreen />} persistor={persistor}>
        <NavigationContainer>
          <AppNavigator />
        </NavigationContainer>
      </PersistGate>
    </Provider>
  )
}

export default App
```

### 2. Navigation Structure

**src/navigation/AppNavigator.tsx**
```typescript
import React from 'react'
import { createNativeStackNavigator } from '@react-navigation/native-stack'
import { useAppSelector } from '../hooks'
import AuthNavigator from './AuthNavigator'
import MainNavigator from './MainNavigator'
import { RootStackParamList } from './types'

const Stack = createNativeStackNavigator<RootStackParamList>()

const AppNavigator: React.FC = () => {
  const { isAuthenticated } = useAppSelector(state => state.auth)

  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      {isAuthenticated ? (
        <Stack.Screen name="Main" component={MainNavigator} />
      ) : (
        <Stack.Screen name="Auth" component={AuthNavigator} />
      )}
    </Stack.Navigator>
  )
}

export default AppNavigator
```

### 3. Redux Store Configuration

**src/store/store.ts**
```typescript
import { configureStore } from '@reduxjs/toolkit'
import { persistStore, persistReducer } from 'redux-persist'
import AsyncStorage from '@react-native-async-storage/async-storage'
import { rootReducer } from './rootReducer'
import { authApi, recordsApi } from './api'

const persistConfig = {
  key: 'root',
  storage: AsyncStorage,
  whitelist: ['auth', 'settings', 'offline']
}

const persistedReducer = persistReducer(persistConfig, rootReducer)

export const store = configureStore({
  reducer: persistedReducer,
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        ignoredActions: ['persist/PERSIST', 'persist/REHYDRATE']
      }
    }).concat(authApi.middleware, recordsApi.middleware)
})

export const persistor = persistStore(store)

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch
```

### 4. Service Layer Example

**src/services/auth/AuthService.ts**
```typescript
import { SecureStorage } from './SecureStorage'
import { BiometricService } from './BiometricService'
import { apiClient } from '../api/apiClient'
import { LoginRequest, AuthTokenDto } from '../../types'

export class AuthService {
  static async login(credentials: LoginRequest): Promise<AuthTokenDto> {
    try {
      const response = await apiClient.post<AuthTokenDto>('/api/mobil/login', credentials)
      
      // Store tokens securely
      await SecureStorage.setItem('jwt_token', response.data.token)
      await SecureStorage.setItem('refresh_token', response.data.refreshToken)
      
      return response.data
    } catch (error) {
      throw new Error('Login failed')
    }
  }

  static async logout(): Promise<void> {
    await SecureStorage.removeItem('jwt_token')
    await SecureStorage.removeItem('refresh_token')
    await BiometricService.disableBiometric()
  }

  static async isAuthenticated(): Promise<boolean> {
    const token = await SecureStorage.getItem('jwt_token')
    return !!token
  }
}
```

### 5. Component Structure Example

**src/components/common/Button/Button.tsx**
```typescript
import React from 'react'
import { TouchableOpacity, Text, ActivityIndicator } from 'react-native'
import { styles } from './Button.styles'

interface ButtonProps {
  title: string
  onPress: () => void
  loading?: boolean
  disabled?: boolean
  variant?: 'primary' | 'secondary' | 'outline'
  size?: 'small' | 'medium' | 'large'
}

export const Button: React.FC<ButtonProps> = ({
  title,
  onPress,
  loading = false,
  disabled = false,
  variant = 'primary',
  size = 'medium'
}) => {
  return (
    <TouchableOpacity
      style={[
        styles.button,
        styles[variant],
        styles[size],
        (disabled || loading) && styles.disabled
      ]}
      onPress={onPress}
      disabled={disabled || loading}
    >
      {loading ? (
        <ActivityIndicator color="white" />
      ) : (
        <Text style={[styles.text, styles[`${variant}Text`]]}>{title}</Text>
      )}
    </TouchableOpacity>
  )
}
```

### 6. Custom Hooks Example

**src/hooks/useAuth.ts**
```typescript
import { useAppSelector, useAppDispatch } from './redux'
import { AuthService } from '../services/auth'
import { loginSuccess, loginFailure, logout } from '../store/slices/authSlice'
import { LoginRequest } from '../types'

export const useAuth = () => {
  const dispatch = useAppDispatch()
  const { user, isAuthenticated, loading, error } = useAppSelector(state => state.auth)

  const login = async (credentials: LoginRequest) => {
    try {
      const authData = await AuthService.login(credentials)
      dispatch(loginSuccess(authData))
    } catch (error) {
      dispatch(loginFailure(error.message))
    }
  }

  const handleLogout = async () => {
    await AuthService.logout()
    dispatch(logout())
  }

  return {
    user,
    isAuthenticated,
    loading,
    error,
    login,
    logout: handleLogout
  }
}
```

### 7. Type Definitions

**src/types/auth.ts**
```typescript
export interface LoginRequest {
  tcNo: string
  password: string
  rememberMe?: boolean
}

export interface AuthTokenDto {
  token: string
  refreshToken: string
  expiresIn: number
  user: UserProfile
}

export interface UserProfile {
  id: number
  tcNo: string
  firstName: string
  lastName: string
  department: string
  role: string
}

export interface AuthState {
  user: UserProfile | null
  isAuthenticated: boolean
  loading: boolean
  error: string | null
}
```

### 8. Environment Configuration

**.env**
```
# API Configuration
API_BASE_URL=http://localhost:8080
API_TIMEOUT=30000

# Security
SSL_PINNING_ENABLED=false
BIOMETRIC_ENABLED=true

# Features
OFFLINE_MODE_ENABLED=true
CRASH_REPORTING_ENABLED=true

# Development
DEBUG_MODE=true
LOG_LEVEL=debug
```

### 9. Package.json Scripts

**package.json**
```json
{
  "scripts": {
    "android": "react-native run-android",
    "ios": "react-native run-ios",
    "start": "react-native start",
    "test": "jest",
    "test:watch": "jest --watch",
    "test:coverage": "jest --coverage",
    "lint": "eslint . --ext .js,.jsx,.ts,.tsx",
    "lint:fix": "eslint . --ext .js,.jsx,.ts,.tsx --fix",
    "type-check": "tsc --noEmit",
    "build:android": "cd android && ./gradlew assembleRelease",
    "build:ios": "cd ios && xcodebuild -workspace PersonnelTrackingApp.xcworkspace -scheme PersonnelTrackingApp -configuration Release -destination generic/platform=iOS -archivePath PersonnelTrackingApp.xcarchive archive",
    "clean": "react-native clean-project-auto",
    "postinstall": "cd ios && pod install"
  }
}
```

## Development Guidelines

### 1. Naming Conventions
- **Files**: PascalCase for components, camelCase for utilities
- **Folders**: camelCase
- **Components**: PascalCase
- **Variables/Functions**: camelCase
- **Constants**: UPPER_SNAKE_CASE

### 2. Import Organization
```typescript
// 1. React imports
import React, { useState, useEffect } from 'react'
import { View, Text, TouchableOpacity } from 'react-native'

// 2. Third-party libraries
import { useNavigation } from '@react-navigation/native'
import { useDispatch } from 'react-redux'

// 3. Internal imports (services, utils, etc.)
import { AuthService } from '../../services/auth'
import { validateTCNo } from '../../utils/validators'

// 4. Relative imports (components, styles)
import { Button } from '../common'
import { styles } from './LoginScreen.styles'

// 5. Type imports (always last)
import type { LoginScreenProps } from './types'
```

### 3. Component Structure
```typescript
// 1. Imports
// 2. Type definitions
// 3. Component implementation
// 4. Default export
// 5. Styles (if inline)

interface ComponentProps {
  // props definition
}

const Component: React.FC<ComponentProps> = ({ prop1, prop2 }) => {
  // 1. Hooks
  // 2. State
  // 3. Effects
  // 4. Event handlers
  // 5. Render helpers
  // 6. Return JSX
}

export default Component
```

Bu yapı, büyük ölçekli React Native projelerinde maintainability ve scalability sağlar.