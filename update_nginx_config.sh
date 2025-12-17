#!/bin/bash

# Update nginx configuration for Personnel Tracking System
# This script fixes the static resource routing issue

echo "Updating nginx configuration for Personnel Tracking System..."

# Backup current configuration
sudo cp /etc/nginx/sites-available/hesap.gaziantep.edu.tr /etc/nginx/sites-available/hesap.gaziantep.edu.tr.backup.$(date +%Y%m%d-%H%M%S)

# Create the new configuration
sudo tee /etc/nginx/sites-available/hesap.gaziantep.edu.tr > /dev/null << 'EOF'
# HTTP'den HTTPS'e yönlendirme
server {
    listen 80;
    server_name hesap.gaziantep.edu.tr;
    return 301 https://$host$request_uri;
}

# HTTPS ana yapılandırması
server {
    listen 443 ssl;
    server_name hesap.gaziantep.edu.tr;

    # SSL sertifika ayarları
    ssl_certificate     /etc/nginx/ssl/hesap.crt;
    ssl_certificate_key /etc/nginx/ssl/hesap.key;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # Google doğrulama dosyası
    location = /google558e76a5c8b4c577.html {
        root /var/www/html;
    }

    # Robots.txt - Arama motorları için
    location = /robots.txt {
        root /var/www/html;
        allow all;
        access_log off;
        log_not_found off;
    }

    # Favicon - Tarayıcı ikonu
    location = /favicon.ico {
        proxy_pass http://localhost:8081/bim-basvuru/favicon.ico;
        log_not_found off;
        access_log off;
    }

    # Ana sayfa (/) - bim-basvuru'ya yönlendir
    location = / {
        return 301 https://$host/bim-basvuru/;
    }

    # Personnel Tracking System - Port 8087
    location /personeltakip {
        # Remove /personeltakip from the path when forwarding to backend
        rewrite ^/personeltakip/?(.*) /$1 break;
        
        proxy_pass http://127.0.0.1:8087;
        
        # Proxy headers
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;
        
        # Handle WebSocket connections
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # Timeout ayarları
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Personnel Tracking System - All routes (login, register, etc.)
    location ~ ^/(login|register|qrcode|admin|api|actuator).*$ {
        proxy_pass http://127.0.0.1:8087;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # BIM Başvuru Uygulaması - Port 8081
    location /bim-basvuru/ {
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;

        # CORS ve CSRF için gerekli header'lar
        proxy_set_header Origin $scheme://$host;
        proxy_set_header Referer $scheme://$host$request_uri;
        proxy_redirect off;
        proxy_buffering off;
    }

    # BIM Başvuru - Statik dosyalar (images) - Specific to BIM
    location /bim-basvuru/images/ {
        proxy_pass http://localhost:8081/bim-basvuru/images/;
        proxy_set_header Host $host;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # BIM Başvuru - Statik dosyalar (css) - Specific to BIM
    location /bim-basvuru/css/ {
        proxy_pass http://localhost:8081/bim-basvuru/css/;
        proxy_set_header Host $host;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # BIM Başvuru - Statik dosyalar (js) - Specific to BIM
    location /bim-basvuru/js/ {
        proxy_pass http://localhost:8081/bim-basvuru/js/;
        proxy_set_header Host $host;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Static resources - Default to Personnel Tracking System
    location /images/ {
        proxy_pass http://127.0.0.1:8087/images/;
        proxy_set_header Host $host;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    location /css/ {
        proxy_pass http://127.0.0.1:8087/css/;
        proxy_set_header Host $host;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    location /js/ {
        proxy_pass http://127.0.0.1:8087/js/;
        proxy_set_header Host $host;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # GAUN SMS Servisi - Port 8082
    location /GaunSmsService {
        proxy_pass http://localhost:8082;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_http_version 1.1;
    }

    # Mobile Web Services API - Port 8090
    location /mobilewebservices {
        # CORS Headers - Tüm origin'lere izin ver
        add_header 'Access-Control-Allow-Origin' '*' always;
        add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS, PATCH' always;
        add_header 'Access-Control-Allow-Headers' 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization' always;
        add_header 'Access-Control-Expose-Headers' 'Content-Length,Content-Range' always;

        # OPTIONS preflight
        if ($request_method = 'OPTIONS') {
            add_header 'Access-Control-Allow-Origin' '*';
            add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS, PATCH';
            add_header 'Access-Control-Allow-Headers' 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization';
            add_header 'Access-Control-Max-Age' 1728000;
            add_header 'Content-Type' 'text/plain; charset=utf-8';
            add_header 'Content-Length' 0;
            return 204;
        }

        proxy_pass http://localhost:8090;
        proxy_redirect http:// https://;
        proxy_redirect http://localhost:8090 https://hesap.gaziantep.edu.tr;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;

        # WebSocket desteği
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # Timeout ayarları
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Ders Detay Uygulaması - Port 8086
    location /ders-detay {
        proxy_pass http://localhost:8086;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;

        # WebSocket desteği
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # Timeout ayarları
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Proliz Web Services - Port 8084
    location /ProlizWebServices {
        proxy_pass http://localhost:8084/ProlizWebServices;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;

        # CORS Headers
        add_header 'Access-Control-Allow-Origin' '*' always;
        add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
        add_header 'Access-Control-Allow-Headers' 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization' always;
        add_header 'Access-Control-Expose-Headers' 'Content-Length,Content-Range' always;

        # OPTIONS preflight
        if ($request_method = 'OPTIONS') {
            add_header 'Access-Control-Allow-Origin' '*';
            add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS';
            add_header 'Access-Control-Allow-Headers' 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization';
            add_header 'Access-Control-Max-Age' 1728000;
            add_header 'Content-Type' 'text/plain; charset=utf-8';
            add_header 'Content-Length' 0;
            return 204;
        }

        # WebSocket desteği
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # Timeout ayarları
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Portainer Container Yönetimi - Port 9000
    location /portainer/ {
        proxy_pass http://localhost:9000/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;

        # Portainer WebSocket desteği
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # Portainer özel ayarlar
        proxy_buffering off;
        proxy_request_buffering off;

        # Timeout ayarları
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Diğer tüm istekler için 404 döndür
    # Bu blok EN SONDA olmalı - yukarıdaki spesifik location'lar önce eşleşir
    location / {
        return 404;
    }
}
EOF

echo "Testing nginx configuration..."
sudo nginx -t

if [ $? -eq 0 ]; then
    echo "Configuration is valid. Reloading nginx..."
    sudo systemctl reload nginx
    echo "Nginx configuration updated successfully!"
    echo ""
    echo "Testing the updated configuration:"
    echo "1. Personnel Tracking System: https://hesap.gaziantep.edu.tr/personeltakip"
    echo "2. Login page: https://hesap.gaziantep.edu.tr/login"
    echo "3. Static resources should now load properly"
else
    echo "Configuration test failed. Please check the configuration."
    exit 1
fi