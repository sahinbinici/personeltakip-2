# Requirements Document

## Introduction

Bu doküman, Personnel Tracking System için React Native mobil uygulamasının gereksinimlerini tanımlar. Uygulama, kullanıcıların QR kod okutarak giriş-çıkış kayıtlarını yapmasını ve mazeret bildirmelerini sağlar.

## Glossary

- **React Native**: Cross-platform mobil uygulama geliştirme framework'ü
- **QR Code Scanner**: QR kodlarını okuyabilen kamera tabanlı tarayıcı
- **GPS Location**: Cihazın coğrafi konum bilgisi (enlem/boylam)
- **JWT Token**: JSON Web Token - kimlik doğrulama için kullanılan güvenlik token'ı
- **Entry/Exit Record**: Giriş/çıkış kayıt bilgisi
- **Excuse Report**: Mazeret bildirimi
- **Personnel Tracking System**: Ana web tabanlı personel takip sistemi
- **Offline Mode**: İnternet bağlantısı olmadığında çalışma modu
- **Push Notification**: Mobil cihaza gönderilen anlık bildirim mesajı
- **Department**: Kullanıcının bağlı olduğu organizasyonel birim
- **Admin User**: ADMIN veya SUPER_ADMIN rolüne sahip yönetici kullanıcı
- **Monthly Report**: Aylık giriş-çıkış kayıtlarının özet raporu

## Requirements

### Requirement 1

**User Story:** Bir personel olarak, mobil uygulamaya giriş yapabilmek istiyorum, böylece QR kod okutarak giriş-çıkış kayıtlarımı yapabilirim.

#### Acceptance Criteria

1. WHEN kullanıcı TC kimlik numarası ve şifre girer THEN mobil uygulama SHALL web API'sine authentication isteği gönder
2. WHEN authentication başarılı olur THEN mobil uygulama SHALL JWT token'ı güvenli şekilde sakla
3. WHEN authentication başarısız olur THEN mobil uygulama SHALL kullanıcıya hata mesajı göster
4. WHEN kullanıcı "Beni Hatırla" seçeneğini işaretler THEN mobil uygulama SHALL login bilgilerini güvenli şekilde sakla
5. WHEN JWT token süresi dolar THEN mobil uygulama SHALL otomatik olarak yeniden login ekranına yönlendir

### Requirement 2

**User Story:** Bir personel olarak, QR kod okutarak giriş-çıkış kaydı yapabilmek istiyorum, böylece çalışma saatlerimi takip edebilirim.

#### Acceptance Criteria

1. WHEN kullanıcı QR kod tarama butonuna basar THEN mobil uygulama SHALL kamera izni iste
2. WHEN kamera izni verilir THEN mobil uygulama SHALL QR kod tarayıcısını aç
3. WHEN geçerli QR kod taranır THEN mobil uygulama SHALL otomatik olarak GPS konumunu al
4. WHEN GPS konumu alınır THEN mobil uygulama SHALL giriş-çıkış kaydını web API'sine gönder
5. WHEN kayıt başarılı olur THEN mobil uygulama SHALL başarı mesajı ve kayıt detaylarını göster

### Requirement 3

**User Story:** Bir personel olarak, QR kod okutamadığım durumlarda mazeret bildirebilmek istiyorum, böylece durumumu yöneticilerime iletebilirim.

#### Acceptance Criteria

1. WHEN kullanıcı "Mazeret Bildir" butonuna basar THEN mobil uygulama SHALL mazeret formu göster
2. WHEN kullanıcı mazeret türünü seçer THEN mobil uygulama SHALL ilgili açıklama alanlarını göster
3. WHEN kullanıcı mazeret açıklaması yazar THEN mobil uygulama SHALL minimum 10 karakter kontrolü yap
4. WHEN mazeret formu doldurulur THEN mobil uygulama SHALL mazeret bilgisini web API'sine gönder
5. WHEN mazeret kaydedilir THEN mobil uygulama SHALL onay mesajı göster

### Requirement 4

**User Story:** Bir personel olarak, geçmiş giriş-çıkış kayıtlarımı görebilmek istiyorum, böylece çalışma geçmişimi takip edebilirim.

#### Acceptance Criteria

1. WHEN kullanıcı "Kayıtlarım" sekmesine tıklar THEN mobil uygulama SHALL son 30 günlük kayıtları getir
2. WHEN kayıtlar yüklenir THEN mobil uygulama SHALL tarih, saat, konum ve durum bilgilerini göster
3. WHEN kullanıcı tarih filtresi uygular THEN mobil uygulama SHALL seçilen tarih aralığındaki kayıtları göster
4. WHEN kullanıcı kayıt detayına tıklar THEN mobil uygulama SHALL detaylı kayıt bilgilerini göster
5. WHEN internet bağlantısı yoksa THEN mobil uygulama SHALL önbelleğe alınmış kayıtları göster

### Requirement 5

**User Story:** Bir personel olarak, internet bağlantısı olmadığında da uygulama kullanabilmek istiyorum, böylece bağlantı sorunları yaşadığımda kayıtlarımı kaybetmem.

#### Acceptance Criteria

1. WHEN internet bağlantısı kesilir THEN mobil uygulama SHALL offline moda geç
2. WHEN offline modda QR kod taranır THEN mobil uygulama SHALL kaydı yerel veritabanına sakla
3. WHEN internet bağlantısı geri gelir THEN mobil uygulama SHALL bekleyen kayıtları otomatik olarak senkronize et
4. WHEN senkronizasyon başarısız olur THEN mobil uygulama SHALL kullanıcıya hata mesajı göster ve yeniden deneme seçeneği sun
5. WHEN offline kayıtlar var THEN mobil uygulama SHALL ana ekranda senkronize edilmemiş kayıt sayısını göster

### Requirement 6

**User Story:** Bir personel olarak, uygulama ayarlarını yapılandırabilmek istiyorum, böylece kişisel tercihlerime göre uygulamayı kullanabilirim.

#### Acceptance Criteria

1. WHEN kullanıcı ayarlar menüsüne girer THEN mobil uygulama SHALL mevcut ayarları göster
2. WHEN kullanıcı bildirim ayarlarını değiştirir THEN mobil uygulama SHALL yeni ayarları kaydet
3. WHEN kullanıcı dil ayarını değiştirir THEN mobil uygulama SHALL uygulamayı yeniden başlat ve yeni dili uygula
4. WHEN kullanıcı çıkış yapar THEN mobil uygulama SHALL tüm oturum verilerini temizle
5. WHEN kullanıcı hesap bilgilerini görüntüler THEN mobil uygulama SHALL kullanıcı profil bilgilerini göster

### Requirement 7

**User Story:** Bir personel olarak, güvenli bir şekilde uygulama kullanabilmek istiyorum, böylece kişisel verilerim korunur.

#### Acceptance Criteria

1. WHEN uygulama başlatılır THEN mobil uygulama SHALL biometric authentication (parmak izi/yüz tanıma) seçeneği sun
2. WHEN biometric authentication başarısız olur THEN mobil uygulama SHALL PIN kodu girişi iste
3. WHEN uygulama arka plana alınır THEN mobil uygulama SHALL 5 dakika sonra otomatik kilit ekranı göster
4. WHEN hassas veriler saklanır THEN mobil uygulama SHALL verileri şifreleyerek sakla
5. WHEN SSL sertifikası geçersiz THEN mobil uygulama SHALL bağlantıyı reddet ve güvenlik uyarısı göster

### Requirement 8

**User Story:** Bir admin olarak, birimdeki personellerin mazeret bildirimlerini mobil uygulamada görebilmek istiyorum, böylece hızlı şekilde durumları takip edebilirim.

#### Acceptance Criteria

1. WHEN admin kullanıcısı mobil uygulamaya giriş yapar THEN mobil uygulama SHALL kullanıcının admin rolünü tespit et
2. WHEN birimindeki bir personel mazeret bildirir THEN mobil uygulama SHALL admin'e push notification gönder
3. WHEN admin bildirimler sekmesine tıklar THEN mobil uygulama SHALL birimindeki tüm mazeret bildirimlerini listele
4. WHEN admin mazeret detayına tıklar THEN mobil uygulama SHALL mazeret açıklaması, tarih ve personel bilgilerini göster
5. WHEN admin mazeret durumunu günceller THEN mobil uygulama SHALL durumu web API'sine kaydet

### Requirement 9

**User Story:** Bir admin olarak, birimdeki personellerin aylık giriş-çıkış raporlarını mobil uygulamada görebilmek istiyorum, böylece saha çalışması sırasında da takip yapabilirim.

#### Acceptance Criteria

1. WHEN admin "Personel Raporları" sekmesine tıklar THEN mobil uygulama SHALL birimindeki personel listesini göster
2. WHEN admin bir personel seçer THEN mobil uygulama SHALL o personelin aylık giriş-çıkış raporunu getir
3. WHEN rapor yüklenir THEN mobil uygulama SHALL günlük giriş-çıkış saatleri, toplam çalışma süresi ve devamsızlık bilgilerini göster
4. WHEN admin farklı ay seçer THEN mobil uygulama SHALL seçilen aya ait raporu güncelle
5. WHEN admin raporu paylaş butonuna basar THEN mobil uygulama SHALL raporu PDF formatında dışa aktar

### Requirement 10

**User Story:** Bir personel olarak, kendi aylık giriş-çıkış raporumu mobil uygulamada görebilmek istiyorum, böylece çalışma performansımı takip edebilirim.

#### Acceptance Criteria

1. WHEN kullanıcı "Aylık Raporlarım" sekmesine tıklar THEN mobil uygulama SHALL mevcut ayın raporunu göster
2. WHEN rapor yüklenir THEN mobil uygulama SHALL günlük giriş-çıkış saatleri, toplam çalışma süresi ve geç kalma bilgilerini göster
3. WHEN kullanıcı farklı ay seçer THEN mobil uygulama SHALL seçilen aya ait kendi raporunu getir
4. WHEN kullanıcı günlük detaya tıklar THEN mobil uygulama SHALL o günün giriş-çıkış saatleri ve konum bilgilerini göster
5. WHEN kullanıcı raporu kaydet butonuna basar THEN mobil uygulama SHALL raporu cihaza PDF olarak kaydet