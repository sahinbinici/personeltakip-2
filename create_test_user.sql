-- Test kullanıcısı oluşturma scripti
-- Bu script test amaçlı normal bir kullanıcı oluşturur

-- Test kullanıcısı bilgileri:
-- TC No: 12345678901
-- Sicil No: TEST001
-- Ad: Test
-- Soyad: Kullanıcı
-- Telefon: 05551234567
-- Şifre: test123 (hash'lenmiş hali)
-- Rol: NORMAL_USER

INSERT INTO users (
    tc_no, 
    personnel_no, 
    first_name, 
    last_name, 
    mobile_phone, 
    department_code, 
    department_name, 
    title_code, 
    password_hash, 
    role, 
    is_active, 
    created_at, 
    updated_at
) VALUES (
    '12345678901',
    'TEST001', 
    'Test',
    'Kullanıcı',
    '05551234567',
    'BIL',
    'Bilgisayar Mühendisliği',
    'ARS',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- test123
    'NORMAL_USER',
    true,
    NOW(),
    NOW()
);

-- Test kullanıcısı için günlük QR kod oluştur
INSERT INTO qr_codes (
    user_id,
    qr_code_value,
    valid_date,
    usage_count,
    max_usage_per_day,
    is_active,
    created_at,
    updated_at
) VALUES (
    (SELECT id FROM users WHERE tc_no = '12345678901'),
    'TEST_QR_CODE_' || DATE_FORMAT(CURDATE(), '%Y%m%d') || '_12345678901',
    CURDATE(),
    0,
    999, -- Geliştirme modunda sınırsız
    true,
    NOW(),
    NOW()
);

-- Kullanıcı oluşturuldu mesajı
SELECT 
    'Test kullanıcısı başarıyla oluşturuldu!' as message,
    'TC No: 12345678901' as tc_no,
    'Şifre: test123' as password,
    'Rol: NORMAL_USER' as role;