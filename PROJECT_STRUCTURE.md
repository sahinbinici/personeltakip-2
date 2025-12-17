# Project Structure

## Overview
This is the backend service for the Personnel Tracking System. The mobile application is maintained in a separate workspace.

```
personeltakip-2/
├── src/                          # Backend source code (Spring Boot)
│   ├── main/java/                # Java source files
│   │   └── com/bidb/personetakip/
│   │       ├── controller/       # REST API controllers
│   │       ├── service/          # Business logic
│   │       ├── repository/       # Data access layer
│   │       ├── model/            # Entity models
│   │       ├── dto/              # Data transfer objects
│   │       ├── config/           # Configuration classes
│   │       └── exception/        # Custom exceptions
│   ├── main/resources/           # Configuration and static files
│   │   ├── static/               # Web assets (CSS, JS, images)
│   │   ├── templates/            # Thymeleaf templates
│   │   └── db/                   # Database migration scripts
│   └── test/java/                # Backend tests
├── .kiro/                        # Kiro IDE specifications
│   └── specs/                    # Feature specifications
├── build.gradle                  # Backend build configuration
├── docker-compose.yml            # Docker configuration
├── .env.example                  # Environment variables template
└── personeltakip.code-workspace  # VS Code workspace configuration
```

## Development Workflow

### 1. Initial Setup
```bash
# Copy environment template
cp .env.example .env
# Edit .env with your configuration

# Backend dependencies (automatic with Gradle)
./gradlew build
```

### 2. Development Mode
```bash
# Start backend
./gradlew bootRun
```

### 3. VS Code Workspace
Open `personeltakip.code-workspace` in VS Code for optimal development experience with:
- Java language support and debugging
- Proper project structure navigation
- Configured linting and formatting

## Architecture

### Backend (Spring Boot)
- **Port**: 8080
- **API Endpoint**: `/api/*`
- **Admin Panel**: `/admin/*`
- **Database**: MySQL
- **Key Features**:
  - JWT Authentication
  - QR Code Generation
  - IP Tracking
  - Personnel Management
  - REST API for mobile app

### Mobile App Integration
- **Status**: Separate workspace/repository
- **API Integration**: Connects via REST API endpoints
- **Documentation**: Available at `/swagger-ui.html`

## API Endpoints
The backend provides REST API for mobile applications:
- **Development**: `http://localhost:8080/api`
- **Staging**: `https://staging-api.personeltakip.com/api`
- **Production**: `https://api.personeltakip.com/api`

### Key API Routes
- `POST /api/auth/login` - Authentication
- `GET /api/personnel` - Personnel data
- `POST /api/entry-exit` - Entry/exit records
- `GET /api/qr/{userId}` - QR code generation

## Testing

### Backend Tests
```bash
./gradlew test
```

### Property-Based Tests
```bash
./gradlew test --tests "*PropertyTest"
```

## Deployment

### Backend
- Docker: `docker-compose up`
- Manual: See `DEPLOYMENT_NOTES.md`

### Environment Configuration
- Development: `.env` file
- Production: Environment variables or `.env.production`

## Troubleshooting

### Common Issues
1. **Port conflicts**: Check if port 8080 is in use
2. **Database connection**: Verify MySQL credentials in `.env`
3. **API access**: Check CORS settings for cross-origin requests

## Contributing
1. Follow the existing code structure
2. Run tests before committing
3. Update documentation for new features
4. Use the provided workspace configuration for consistent development experience