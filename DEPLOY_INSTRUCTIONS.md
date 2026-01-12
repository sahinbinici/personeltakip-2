# Personeltakip Deployment Instructions

## Sunucu: 193.140.136.26
## Port: 8081

### Adım 1: Dosyayı sunucuya kopyalayın
```bash
scp personeltakip-deploy.tar cekec@193.140.136.26:/tmp/
```

### Adım 2: Sunucuya SSH ile bağlanın
```bash
ssh cekec@193.140.136.26
```

### Adım 3: Sunucuda aşağıdaki komutları çalıştırın

```bash
# Deployment dizini oluştur
sudo mkdir -p /opt/personeltakip
cd /opt/personeltakip

# Dosyaları çıkart
sudo tar -xvf /tmp/personeltakip-deploy.tar

# Network oluştur (yoksa)
docker network create personeltakip-network 2>/dev/null || echo "Network exists"

# MySQL'i network'e bağla
docker network connect personeltakip-network personeltakip-mysql 2>/dev/null || echo "Already connected"

# Eski container'ı durdur ve sil
docker stop personeltakip-app 2>/dev/null || true
docker rm personeltakip-app 2>/dev/null || true

# Docker image'ı build et
sudo docker build -t personeltakip-app:latest .

# Uygulamayı başlat
sudo docker-compose up -d

# Logları kontrol et
docker logs -f personeltakip-app
```

### Adım 4: Test edin
Tarayıcıda açın: http://193.140.136.26:8081

### Sorun Giderme

Container durumunu kontrol:
```bash
docker ps -a | grep personeltakip
```

Logları görüntüle:
```bash
docker logs personeltakip-app --tail 100
```

Container'ı yeniden başlat:
```bash
docker restart personeltakip-app
```
