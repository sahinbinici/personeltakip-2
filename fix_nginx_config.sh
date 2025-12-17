#!/bin/bash

# Backup current config
sudo cp /etc/nginx/sites-available/hesap.gaziantep.edu.tr /etc/nginx/sites-available/hesap.gaziantep.edu.tr.backup.$(date +%Y%m%d-%H%M%S)

# Add personeltakip location block before Portainer section
sudo sed -i '/# Portainer Container Yönetimi/i\
    # Personnel Tracking System - Port 8087\
    location /personeltakip {\
        # Remove /personeltakip from the path when forwarding to backend\
        rewrite ^/personeltakip/?(.*) /$1 break;\
        \
        proxy_pass http://127.0.0.1:8087;\
        \
        # Proxy headers\
        proxy_set_header Host $host;\
        proxy_set_header X-Real-IP $remote_addr;\
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\
        proxy_set_header X-Forwarded-Proto $scheme;\
        proxy_set_header X-Forwarded-Host $host;\
        proxy_set_header X-Forwarded-Port $server_port;\
        \
        # Handle WebSocket connections\
        proxy_http_version 1.1;\
        proxy_set_header Upgrade $http_upgrade;\
        proxy_set_header Connection "upgrade";\
        \
        # Timeout ayarları\
        proxy_connect_timeout 60s;\
        proxy_send_timeout 60s;\
        proxy_read_timeout 60s;\
    }\
    \
    # Personnel Tracking System - Additional routes\
    location ~ ^/personeltakip/(login|register|qrcode|admin|api|static|css|js|images|actuator) {\
        proxy_pass http://127.0.0.1:8087;\
        \
        # Proxy headers\
        proxy_set_header Host $host;\
        proxy_set_header X-Real-IP $remote_addr;\
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\
        proxy_set_header X-Forwarded-Proto $scheme;\
        proxy_set_header X-Forwarded-Host $host;\
        proxy_set_header X-Forwarded-Port $server_port;\
        \
        # Handle WebSocket connections\
        proxy_http_version 1.1;\
        proxy_set_header Upgrade $http_upgrade;\
        proxy_set_header Connection "upgrade";\
        \
        # Timeout ayarları\
        proxy_connect_timeout 60s;\
        proxy_send_timeout 60s;\
        proxy_read_timeout 60s;\
    }\
' /etc/nginx/sites-available/hesap.gaziantep.edu.tr

# Test nginx configuration
sudo nginx -t

# If test passes, reload nginx
if [ $? -eq 0 ]; then
    echo "Nginx configuration test passed. Reloading nginx..."
    sudo systemctl reload nginx
    echo "Nginx reloaded successfully."
else
    echo "Nginx configuration test failed. Please check the configuration."
    exit 1
fi

# Test the application
echo "Testing application access..."
curl -I https://hesap.gaziantep.edu.tr/personeltakip -k