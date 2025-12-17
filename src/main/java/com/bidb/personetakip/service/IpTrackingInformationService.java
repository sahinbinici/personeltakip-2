package com.bidb.personetakip.service;

import com.bidb.personetakip.config.IpTrackingConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for providing IP tracking information and help text to the UI.
 * Handles generation of clear information displays, help text, and privacy notices.
 * 
 * Requirements: 6.4 - Clear IP tracking information display
 */
@Service
public class IpTrackingInformationService {

    private final IpTrackingConfig ipTrackingConfig;

    @Autowired
    public IpTrackingInformationService(IpTrackingConfig ipTrackingConfig) {
        this.ipTrackingConfig = ipTrackingConfig;
    }

    /**
     * Generate clear IP tracking status display text.
     * 
     * @return Status display text in Turkish
     */
    public String getIpTrackingStatusDisplay() {
        if (ipTrackingConfig.isEnabled()) {
            return "IP Takibi: Aktif";
        } else {
            return "IP Takibi: Pasif";
        }
    }

    /**
     * Generate privacy settings information display.
     * 
     * @return Privacy information text in Turkish
     */
    public String getPrivacyInformationDisplay() {
        StringBuilder info = new StringBuilder();
        
        if (ipTrackingConfig.getPrivacy().isEnabled()) {
            info.append("Gizlilik koruması aktif. ");
        }
        
        if (ipTrackingConfig.getPrivacy().isAnonymizeReports()) {
            info.append("Raporlarda IP adresleri anonimleştirilir. ");
        }
        
        if (info.length() == 0) {
            info.append("Standart IP görüntüleme aktif.");
        }
        
        return info.toString().trim();
    }

    /**
     * Generate IP assignment help text for user interface.
     * 
     * @return Help text in Turkish explaining IP assignment functionality
     */
    public String getIpAssignmentHelpText() {
        return "IP adresi atama işlemi için birden fazla IP adresi girebilirsiniz. " +
               "IPv4 (192.168.1.100) ve IPv6 (2001:db8::1) formatları desteklenir. " +
               "Birden fazla adres için virgül (,) veya noktalı virgül (;) kullanın. " +
               "Örnek: 192.168.1.100, 10.0.0.50";
    }

    /**
     * Generate IP tracking notice for users.
     * 
     * @return Notice text in Turkish about IP tracking
     */
    public String getIpTrackingNotice() {
        StringBuilder notice = new StringBuilder();
        
        if (ipTrackingConfig.isEnabled()) {
            notice.append("IP adresi takibi aktif. Giriş/çıkış işlemlerinizde IP adresiniz kaydedilmektedir. ");
        }
        
        if (ipTrackingConfig.getPrivacy().isEnabled()) {
            notice.append("Gizliliğiniz korunmaktadır ve IP bilgileriniz güvenli şekilde işlenmektedir. ");
        }
        
        if (!ipTrackingConfig.isEnabled()) {
            notice.append("IP adresi takibi şu anda devre dışı.");
        }
        
        return notice.toString().trim();
    }

    /**
     * Get IP tracking status indicator for UI display.
     * 
     * @return Status indicator object with icon and text
     */
    public IpTrackingStatusIndicator getStatusIndicator() {
        if (ipTrackingConfig.isEnabled()) {
            return new IpTrackingStatusIndicator(
                "fas fa-shield-alt", 
                "success", 
                "IP Takibi Aktif",
                "Giriş/çıkış işlemlerinde IP adresleri kaydediliyor"
            );
        } else {
            return new IpTrackingStatusIndicator(
                "fas fa-shield-alt", 
                "secondary", 
                "IP Takibi Pasif",
                "IP adresi takibi şu anda devre dışı"
            );
        }
    }

    /**
     * Get privacy status indicator for UI display.
     * 
     * @return Privacy status indicator object
     */
    public IpTrackingStatusIndicator getPrivacyStatusIndicator() {
        if (ipTrackingConfig.getPrivacy().isEnabled()) {
            return new IpTrackingStatusIndicator(
                "fas fa-user-shield", 
                "info", 
                "Gizlilik Koruması Aktif",
                "IP adresleri gizlilik ayarlarına uygun şekilde görüntüleniyor"
            );
        } else {
            return new IpTrackingStatusIndicator(
                "fas fa-eye", 
                "warning", 
                "Standart Görüntüleme",
                "IP adresleri tam olarak görüntüleniyor"
            );
        }
    }

    /**
     * Check if IP tracking is enabled.
     * 
     * @return true if IP tracking is enabled
     */
    public boolean isIpTrackingEnabled() {
        return ipTrackingConfig.isEnabled();
    }

    /**
     * Check if privacy mode is enabled.
     * 
     * @return true if privacy mode is enabled
     */
    public boolean isPrivacyModeEnabled() {
        return ipTrackingConfig.getPrivacy().isEnabled();
    }

    /**
     * Check if report anonymization is enabled.
     * 
     * @return true if report anonymization is enabled
     */
    public boolean isReportAnonymizationEnabled() {
        return ipTrackingConfig.getPrivacy().isAnonymizeReports();
    }

    /**
     * Status indicator class for UI display.
     */
    public static class IpTrackingStatusIndicator {
        private final String iconClass;
        private final String colorClass;
        private final String title;
        private final String description;

        public IpTrackingStatusIndicator(String iconClass, String colorClass, String title, String description) {
            this.iconClass = iconClass;
            this.colorClass = colorClass;
            this.title = title;
            this.description = description;
        }

        public String getIconClass() {
            return iconClass;
        }

        public String getColorClass() {
            return colorClass;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }
    }
}