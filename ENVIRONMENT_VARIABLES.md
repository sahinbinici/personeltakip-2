# Environment Variables Documentation

This document lists all required environment variables for the Personnel Tracking System.

## Required Environment Variables

### Local Database Configuration
- `DB_HOST`: MySQL host for local database (default: localhost)
- `DB_NAME`: Local database name (default: personnel_tracking)
- `DB_USERNAME`: Local database username (default: root)
- `DB_PASSWORD`: Local database password (required)

### External Database Configuration (Read-Only)
- `EXTERNAL_DB_HOST`: MySQL host for external personnel database
- `EXTERNAL_DB_NAME`: External database name
- `EXTERNAL_DB_USERNAME`: External database username (read-only credentials)
- `EXTERNAL_DB_PASSWORD`: External database password

### JWT Configuration
- `JWT_SECRET`: Secret key for JWT token signing (minimum 256-bit, base64 encoded)
  - Example generation: `openssl rand -base64 32`
  - **CRITICAL**: Must be kept secret and never committed to version control
- `JWT_EXPIRATION`: JWT token expiration time in milliseconds (default: 1800000 = 30 minutes)
- `JWT_ISSUER`: JWT issuer identifier (default: personnel-tracking-system)

### SMS Gateway Configuration
- `SMS_GATEWAY_URL`: URL endpoint for SMS gateway API
- `SMS_GATEWAY_API_KEY`: API key for SMS gateway authentication
- `SMS_SENDER_ID`: Sender ID displayed in SMS messages (default: PersonelTakip)

### SSL/HTTPS Configuration (Production Only)
- `SSL_ENABLED`: Enable SSL/HTTPS (default: false for dev, true for prod)
- `SSL_KEYSTORE_PATH`: Path to SSL keystore file (e.g., /etc/ssl/keystore.p12)
- `SSL_KEYSTORE_PASSWORD`: Password for SSL keystore
- `SSL_KEYSTORE_TYPE`: Keystore type (default: PKCS12)

### CORS Configuration
- `CORS_ALLOWED_ORIGINS`: Comma-separated list of allowed origins (e.g., https://app.example.com,https://mobile.example.com)
  - Use `*` for development only (not recommended for production)
- `CORS_ALLOWED_METHODS`: Comma-separated list of allowed HTTP methods (default: GET,POST,PUT,DELETE,OPTIONS,PATCH)
- `CORS_ALLOWED_HEADERS`: Comma-separated list of allowed headers (default: Authorization,Content-Type,Accept,X-Requested-With)
- `CORS_ALLOW_CREDENTIALS`: Allow credentials in CORS requests (default: true)
- `CORS_MAX_AGE`: Max age for preflight cache in seconds (default: 3600)

## Development Environment Setup

For local development, create a `.env` file in the project root (DO NOT commit this file):

```bash
# Local Database
DB_USERNAME=root
DB_PASSWORD=your_local_password

# External Database
EXTERNAL_DB_HOST=localhost
EXTERNAL_DB_NAME=personnel_master_dev
EXTERNAL_DB_USERNAME=readonly
EXTERNAL_DB_PASSWORD=readonly_password

# JWT (use weak key for dev only)
JWT_SECRET=dev-secret-key-not-for-production
JWT_EXPIRATION=1800000
JWT_ISSUER=personnel-tracking-system-dev

# SMS Gateway (mock service)
SMS_GATEWAY_URL=http://localhost:9090/mock-sms
SMS_GATEWAY_API_KEY=dev-api-key
SMS_SENDER_ID=DevPersonelTakip

# SSL/HTTPS (disabled for development)
SSL_ENABLED=false

# CORS (allow all for development)
CORS_ALLOWED_ORIGINS=*
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS,PATCH
CORS_ALLOW_CREDENTIALS=true
```

## Production Environment Setup

Set environment variables in your deployment platform:

**Required Production Variables:**
- `JWT_SECRET`: Strong randomly generated secret (minimum 256-bit)
- `SSL_ENABLED`: Must be set to `true`
- `SSL_KEYSTORE_PATH`: Path to SSL certificate keystore
- `SSL_KEYSTORE_PASSWORD`: Keystore password
- `CORS_ALLOWED_ORIGINS`: Specific allowed origins (comma-separated, NO wildcards)

### Docker
```bash
docker run -e DB_USERNAME=prod_user \
           -e DB_PASSWORD=secure_password \
           -e JWT_SECRET=$(openssl rand -base64 32) \
           -e SSL_ENABLED=true \
           -e SSL_KEYSTORE_PATH=/etc/ssl/keystore.p12 \
           -e SSL_KEYSTORE_PASSWORD=secure_keystore_password \
           -e CORS_ALLOWED_ORIGINS=https://app.example.com,https://mobile.example.com \
           ...
```

### Kubernetes
Create a Secret:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: personnel-tracking-secrets
type: Opaque
data:
  db-password: <base64-encoded-password>
  jwt-secret: <base64-encoded-secret>
  ssl-keystore-password: <base64-encoded-password>
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: personnel-tracking-config
data:
  SSL_ENABLED: "true"
  CORS_ALLOWED_ORIGINS: "https://app.example.com,https://mobile.example.com"
  JWT_EXPIRATION: "1800000"
```

### Systemd Service
Add to `/etc/systemd/system/personnel-tracking.service`:
```ini
[Service]
Environment="DB_USERNAME=prod_user"
Environment="DB_PASSWORD=secure_password"
Environment="JWT_SECRET=your-secure-jwt-secret"
Environment="SSL_ENABLED=true"
Environment="SSL_KEYSTORE_PATH=/etc/ssl/keystore.p12"
Environment="SSL_KEYSTORE_PASSWORD=secure_keystore_password"
Environment="CORS_ALLOWED_ORIGINS=https://app.example.com,https://mobile.example.com"
...
```

## Security Best Practices

1. **Never commit secrets to version control**
2. **Use strong, randomly generated JWT secrets** (minimum 256-bit)
   - Generate with: `openssl rand -base64 32`
3. **Rotate JWT secrets periodically** (every 90 days recommended)
4. **Use read-only credentials** for external database
5. **Enable SSL/HTTPS in production** (REQUIRED)
   - Set `SSL_ENABLED=true`
   - Use valid SSL certificates (not self-signed in production)
   - Enable only TLS 1.2 and TLS 1.3
6. **Restrict CORS origins in production**
   - Never use `*` wildcard in production
   - Specify exact allowed origins
   - Include protocol (https://) in origins
7. **Restrict database access** to application servers only
8. **Use environment-specific configurations**
   - Different secrets for dev/staging/prod
   - Different CORS origins per environment
9. **Monitor and audit** environment variable access
10. **Keep JWT expiration short** (30 minutes recommended)
11. **Use strong SSL ciphers** (configured in application.properties)

## SSL Certificate Generation

For production deployment, you need a valid SSL certificate. Here are the options:

### Option 1: Let's Encrypt (Recommended for Production)
```bash
# Install certbot
sudo apt-get install certbot

# Generate certificate
sudo certbot certonly --standalone -d yourdomain.com

# Convert to PKCS12 format for Spring Boot
sudo openssl pkcs12 -export \
  -in /etc/letsencrypt/live/yourdomain.com/fullchain.pem \
  -inkey /etc/letsencrypt/live/yourdomain.com/privkey.pem \
  -out /etc/ssl/keystore.p12 \
  -name tomcat \
  -passout pass:your_keystore_password
```

### Option 2: Self-Signed Certificate (Development/Testing Only)
```bash
# Generate self-signed certificate
keytool -genkeypair \
  -alias personnel-tracking \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore keystore.p12 \
  -validity 365 \
  -storepass changeit

# Place in src/main/resources/ for development
```

### Option 3: Commercial Certificate
Purchase from a Certificate Authority (CA) and convert to PKCS12 format.

## Verification

To verify your environment variables are set correctly:

```bash
# Check if variables are set
echo $DB_USERNAME
echo $JWT_SECRET

# Test database connection
mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD -e "SELECT 1"

# Test external database connection
mysql -h $EXTERNAL_DB_HOST -u $EXTERNAL_DB_USERNAME -p$EXTERNAL_DB_PASSWORD -e "SELECT 1"
```
