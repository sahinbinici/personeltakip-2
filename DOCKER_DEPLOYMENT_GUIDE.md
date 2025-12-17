# Personnel Tracking System - Docker Deployment Guide

## Deployment Overview

Bu rehber, Personnel Tracking System'i Docker container olarak 193.140.136.26 sunucusunda deploy etmek için gerekli adımları içerir.

## Deployment Bilgileri

- **Sunucu IP**: 193.140.136.26
- **Uygulama URL**: hesap.gaziantep.edu.tr/personeltakip
- **Container Port**: 8084 (host) -> 8080 (container)
- **Container Name**: personeltakip-app

## Deployment Adımları

### 1. Gerekli Dosyalar

Deployment için aşağıdaki dosyalar hazırlanmıştır:
- `Dockerfile` - Multi-stage build ile optimize edilmiş container image
- `docker-compose.yml` - Container orchestration
- `.env.example` - Environment variables template
- `deploy.sh` - Otomatik deployment script (Linux/Mac)
- `nginx-config-example.conf` - Nginx reverse proxy örneği

### 2. Environment Variables

`.env.example` dosyasını `.env` olarak kopyalayın ve production değerlerini güncelleyin:

```bash
cp .env.example .env
```

Önemli değişkenler:
- `DB_HOST`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` - Ana veritabanı
- `JWT_SECRET` - Production için güçlü bir secret kullanın
- `CORS_ALLOWED_ORIGINS` - https://hesap.gaziantep.edu.tr

### 3. Manual Deployment (Windows)

Windows'tan manual deployment için:

```powershell
# 1. Docker image build et
docker build -t personeltakip:latest .

# 2. Image'ı tar dosyasına kaydet
docker save personeltakip:latest > personeltakip.tar

# 3. Dosyaları sunucuya transfer et
scp personeltakip.tar docker-compose.yml .env root@193.140.136.26:/opt/personeltakip/

# 4. Sunucuda deployment
ssh root@193.140.136.26
cd /opt/personeltakip
docker load < personeltakip.tar
docker-compose up -d
```

### 4. Otomatik Deployment (Linux/Mac)

Linux veya Mac'ten otomatik deployment için:

```bash
chmod +x deploy.sh
./deploy.sh
```

## Sunucu Konfigürasyonu

### Container Yönetimi

```bash
# Container durumunu kontrol et
docker-compose ps

# Logları görüntüle
docker-compose logs -f

# Container'ı yeniden başlat
docker-compose restart

# Container'ı durdur
docker-compose down

# Container'ı güncelle
docker-compose pull && docker-compose up -d
```

### Health Check

Uygulama health check endpoint'i:
```
http://193.140.136.26:8084/actuator/health
```

### Database Migration

**ÖNEMLİ**: İlk deployment'tan önce veritabanı migration'ını uygulayın:

```bash
# Sunucuda migration script'i çalıştır
cd /opt/personeltakip
powershell -File migrate_ip_tracking.ps1
```

## Nginx Reverse Proxy

Nginx konfigürasyonunda aşağıdaki location block'unu ekleyin:

```nginx
location /personeltakip {
    rewrite ^/personeltakip/?(.*) /$1 break;
    proxy_pass http://127.0.0.1:8084;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Host $server_name;
}
```

## Monitoring ve Maintenance

### Log Monitoring

```bash
# Application logs
docker-compose logs -f personeltakip-app

# Nginx logs
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log
```

### Backup

```bash
# Database backup (if using local MySQL)
docker exec mysql-container mysqldump -u root -p personnel_tracking > backup_$(date +%Y%m%d).sql

# Application logs backup
tar -czf logs_backup_$(date +%Y%m%d).tar.gz /opt/personeltakip/logs/
```

### Updates

```bash
# Pull latest changes
git pull origin main

# Rebuild and redeploy
docker-compose down
docker build -t personeltakip:latest .
docker-compose up -d
```

## Troubleshooting

### Common Issues

1. **Container başlamıyor**
   ```bash
   docker-compose logs personeltakip-app
   ```

2. **Database connection error**
   - Environment variables'ları kontrol edin
   - Database migration'ının uygulandığından emin olun

3. **502 Bad Gateway (Nginx)**
   - Container'ın çalıştığından emin olun: `docker-compose ps`
   - Port 8080'in açık olduğunu kontrol edin

4. **IP Tracking errors**
   - Migration script'inin çalıştırıldığından emin olun
   - Database'de `assigned_ip_addresses` column'unun varlığını kontrol edin

### Performance Tuning

Container resource limits'leri `docker-compose.yml`'de ayarlanabilir:

```yaml
deploy:
  resources:
    limits:
      memory: 1G
      cpus: '1.0'
```

## Security Considerations

1. **Environment Variables**: Sensitive bilgileri `.env` dosyasında saklayın
2. **JWT Secret**: Production'da güçlü, unique bir secret kullanın
3. **Database Passwords**: Güçlü şifreler kullanın
4. **CORS**: Sadece gerekli origin'lere izin verin
5. **Nginx**: Rate limiting ve security headers ekleyin

## Support

Deployment sorunları için:
1. Container logs'larını kontrol edin
2. Health check endpoint'ini test edin
3. Database connection'ını doğrulayın
4. Nginx configuration'ını kontrol edin