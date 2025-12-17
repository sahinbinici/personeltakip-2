# Personnel Tracking System - Backend (personeltakip-2)

## Project Structure

This is the backend service for the Personnel Tracking System:

### Backend (Spring Boot)
- **Technology**: Java 17 + Spring Boot 4.0.0
- **Database**: MySQL
- **Features**: 
  - Personnel management
  - QR code generation
  - IP tracking
  - Admin dashboard
  - REST API for mobile applications
  - Swagger UI documentation

### Mobile App
- **Status**: Moved to separate workspace
- **Integration**: Connects via REST API
- **API Documentation**: Available at `/swagger-ui.html`

## Quick Start

### 1. Environment Setup
```bash
# Copy environment template and configure
cp .env.example .env
# Edit .env with your database and API credentials
```

### 2. Backend
```bash
./gradlew bootRun
```

### 3. Access Points
- **Backend**: http://localhost:8080
- **Admin Panel**: http://localhost:8080/admin
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **API Endpoints**: http://localhost:8080/api/*

## API Integration

The backend provides REST API endpoints for mobile applications:

### Authentication
- `POST /api/auth/login` - User authentication
- `POST /api/auth/register` - User registration

### Personnel Management
- `GET /api/personnel` - Get personnel list
- `POST /api/entry-exit` - Record entry/exit

### QR Code
- `GET /api/qr/{userId}` - Generate QR code for user

## Documentation
- [Deployment Guide](./DEPLOYMENT_NOTES.md)
- [Docker Deployment](./DOCKER_DEPLOYMENT_GUIDE.md)
- [API Documentation](http://localhost:8080/swagger-ui.html) (when running)
