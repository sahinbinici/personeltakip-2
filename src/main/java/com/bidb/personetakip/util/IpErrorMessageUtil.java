package com.bidb.personetakip.util;

import com.bidb.personetakip.exception.IpAssignmentException;
import com.bidb.personetakip.exception.IpCaptureException;
import com.bidb.personetakip.exception.IpPrivacyConfigurationException;
import com.bidb.personetakip.exception.IpValidationException;

/**
 * Utility class for generating user-friendly IP operation error messages.
 * Requirements: 1.3, 6.2
 */
public class IpErrorMessageUtil {
    
    private IpErrorMessageUtil() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Generates user-friendly error message for IP validation failures.
     * Requirements: 1.3
     */
    public static String getIpValidationErrorMessage(IpValidationException ex) {
        if (ex.getInvalidIpAddress() != null && ex.getValidationReason() != null) {
            return String.format("IP adresi geçersiz: '%s' - %s", 
                ex.getInvalidIpAddress(), 
                translateValidationReason(ex.getValidationReason()));
        }
        return "IP adresi formatı geçersiz. Lütfen geçerli bir IPv4 veya IPv6 adresi girin.";
    }
    
    /**
     * Generates user-friendly error message for IP capture failures.
     * Requirements: 6.2
     */
    public static String getIpCaptureErrorMessage(IpCaptureException ex) {
        String context = ex.getCaptureContext();
        
        if ("null_request".equals(context)) {
            return "İstek bilgisi alınamadı, IP adresi tespit edilemedi.";
        } else if ("tracking_disabled".equals(context)) {
            return "IP takibi devre dışı bırakılmış durumda.";
        } else if ("timeout_exceeded".equals(context)) {
            return "IP adresi tespiti zaman aşımına uğradı. İşlem devam ediyor.";
        } else if ("no_valid_ip_found".equals(context)) {
            return "Geçerli IP adresi bulunamadı. İşlem 'Bilinmeyen' IP ile kaydedildi.";
        } else if ("unexpected_error".equals(context)) {
            return "IP adresi tespitinde beklenmeyen hata oluştu.";
        }
        
        return "IP adresi yakalanamadı: " + ex.getMessage();
    }
    
    /**
     * Generates user-friendly error message for IP assignment failures.
     * Requirements: 3.1, 3.2, 3.5
     */
    public static String getIpAssignmentErrorMessage(IpAssignmentException ex) {
        String operation = ex.getAssignmentOperation();
        String userId = ex.getUserId();
        String invalidAssignment = ex.getInvalidAssignment();
        
        if ("validate".equals(operation)) {
            if (invalidAssignment != null) {
                if (invalidAssignment.contains(",") || invalidAssignment.contains(";")) {
                    return String.format("IP atama listesinde geçersiz adres bulundu: %s", invalidAssignment);
                } else {
                    return String.format("Geçersiz IP adresi formatı: %s", invalidAssignment);
                }
            }
            return "IP atama doğrulaması başarısız. Lütfen IP adreslerini kontrol edin.";
        } else if ("remove".equals(operation)) {
            if (invalidAssignment != null) {
                return String.format("Kaldırılacak IP adresi '%s' mevcut atamalar arasında bulunamadı.", invalidAssignment);
            }
            return "IP adresi kaldırma işlemi başarısız.";
        }
        
        return "IP atama işlemi başarısız: " + ex.getMessage();
    }
    
    /**
     * Generates user-friendly error message for IP privacy configuration failures.
     * Requirements: 5.2, 5.5, 6.5
     */
    public static String getIpPrivacyConfigurationErrorMessage(IpPrivacyConfigurationException ex) {
        String configKey = ex.getConfigurationKey();
        String configOperation = ex.getConfigurationOperation();
        
        if ("anonymization.method".equals(configKey)) {
            return "IP anonimleştirme yöntemi yapılandırması geçersiz.";
        } else if ("anonymization.maskCharacter".equals(configKey)) {
            return "IP maskeleme karakteri yapılandırılmamış.";
        } else if ("anonymization.ipv4PreserveOctets".equals(configKey)) {
            return "IPv4 koruma oktet sayısı geçersiz (0-4 arası olmalı).";
        } else if ("anonymization.ipv6PreserveGroups".equals(configKey)) {
            return "IPv6 koruma grup sayısı geçersiz (0-8 arası olmalı).";
        } else if ("audit.action".equals(configKey)) {
            return "Denetim log işlem türü geçersiz.";
        } else if ("display".equals(configOperation)) {
            return "IP adresi görüntüleme gizlilik ayarları hatası.";
        } else if ("log".equals(configOperation)) {
            return "IP adresi denetim kayıt ayarları hatası.";
        }
        
        return "IP gizlilik yapılandırma hatası: " + ex.getMessage();
    }
    
    /**
     * Translates technical validation reasons to user-friendly Turkish messages.
     */
    private static String translateValidationReason(String reason) {
        switch (reason) {
            case "null value":
                return "boş değer";
            case "empty string":
                return "boş metin";
            case "exceeds maximum length of 45 characters":
                return "maksimum 45 karakter uzunluğunu aşıyor";
            case "spaces not allowed in IP addresses":
                return "IP adreslerinde boşluk karakteri kullanılamaz";
            case "must be in format x.x.x.x where x is 0-255":
                return "x.x.x.x formatında olmalı (x: 0-255 arası)";
            case "must be valid IPv4 or IPv6 address":
                return "geçerli IPv4 veya IPv6 adresi olmalı";
            default:
                if (reason.startsWith("invalid IPv6 format")) {
                    return "geçersiz IPv6 formatı";
                }
                return reason;
        }
    }
    
    /**
     * Generates generic IP operation error message.
     */
    public static String getGenericIpErrorMessage(String operation) {
        switch (operation) {
            case "capture":
                return "IP adresi yakalama işlemi başarısız oldu.";
            case "validate":
                return "IP adresi doğrulama işlemi başarısız oldu.";
            case "assign":
                return "IP adresi atama işlemi başarısız oldu.";
            case "remove":
                return "IP adresi kaldırma işlemi başarısız oldu.";
            case "display":
                return "IP adresi görüntüleme işlemi başarısız oldu.";
            case "anonymize":
                return "IP adresi anonimleştirme işlemi başarısız oldu.";
            case "audit":
                return "IP adresi denetim kayıt işlemi başarısız oldu.";
            default:
                return "IP işlemi başarısız oldu.";
        }
    }
}