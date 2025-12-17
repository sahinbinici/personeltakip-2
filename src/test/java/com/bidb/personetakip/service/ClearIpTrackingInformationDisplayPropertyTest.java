package com.bidb.personetakip.service;

import com.bidb.personetakip.config.IpTrackingConfig;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for clear IP tracking information display functionality.
 * 
 * **Feature: ip-tracking, Property 26: Clear IP Tracking Information Display**
 * **Validates: Requirements 6.4**
 */
@RunWith(JUnitQuickcheck.class)
@SpringBootTest
@TestPropertySource(properties = {
    "ip.tracking.enabled=true",
    "ip.tracking.privacy.enabled=false"
})
class ClearIpTrackingInformationDisplayPropertyTest {

    @Property(trials = 100)
    public void ipTrackingStatusShouldBeDisplayedClearly(boolean enabled, boolean privacyEnabled, boolean anonymizeReports) {
        IpTrackingConfig config = createConfig(enabled, privacyEnabled, anonymizeReports);
        IpTrackingInformationService service = new IpTrackingInformationService(config);
        
        // For any IP tracking configuration, the status should be clearly displayable
        String statusDisplay = service.getIpTrackingStatusDisplay();
        
        // The status display should contain clear information
        assertThat(statusDisplay).isNotNull();
        assertThat(statusDisplay).isNotEmpty();
        assertThat(statusDisplay).containsAnyOf("Aktif", "Pasif", "Enabled", "Disabled");
        
        // Should indicate the current state
        if (config.isEnabled()) {
            assertThat(statusDisplay).satisfiesAnyOf(
                s -> assertThat(s).containsIgnoringCase("aktif"),
                s -> assertThat(s).containsIgnoringCase("enabled")
            );
        } else {
            assertThat(statusDisplay).satisfiesAnyOf(
                s -> assertThat(s).containsIgnoringCase("pasif"),
                s -> assertThat(s).containsIgnoringCase("disabled")
            );
        }
    }

    @Property(trials = 100)
    public void ipPrivacySettingsInformationShouldBeClear(boolean enabled, boolean privacyEnabled, boolean anonymizeReports) {
        IpTrackingConfig config = createConfig(enabled, privacyEnabled, anonymizeReports);
        IpTrackingInformationService service = new IpTrackingInformationService(config);
        
        // For any privacy configuration, the information should be clearly displayable
        String privacyInfo = service.getPrivacyInformationDisplay();
        
        // Privacy information should be clear and understandable
        assertThat(privacyInfo).isNotNull();
        assertThat(privacyInfo).isNotEmpty();
        
        if (config.getPrivacy().isEnabled()) {
            assertThat(privacyInfo).containsAnyOf("Gizlilik", "Privacy", "Maskeleme", "Masking");
        }
        
        if (config.getPrivacy().isAnonymizeReports()) {
            assertThat(privacyInfo).containsAnyOf("Anonimleştirme", "Anonymization", "Rapor", "Report");
        }
    }

    @Property(trials = 100)
    public void ipAssignmentHelpTextShouldBeInformative(boolean enabled, boolean privacyEnabled, boolean anonymizeReports) {
        IpTrackingConfig config = createConfig(enabled, privacyEnabled, anonymizeReports);
        IpTrackingInformationService service = new IpTrackingInformationService(config);
        
        // For any configuration, IP assignment help text should be informative
        String helpText = service.getIpAssignmentHelpText();
        
        // Help text should contain useful information
        assertThat(helpText).isNotNull();
        assertThat(helpText).isNotEmpty();
        assertThat(helpText.length()).isGreaterThan(20); // Should be substantial
        
        // Should contain key information about IP assignment
        assertThat(helpText).containsAnyOf(
            "IP adresi", "IP address", "atama", "assignment", 
            "virgül", "comma", "IPv4", "IPv6"
        );
    }

    @Property(trials = 100)
    public void ipTrackingNoticesShouldBeVisible(boolean trackingEnabled, boolean privacyEnabled, boolean anonymizeReports) {
        IpTrackingConfig config = createConfig(trackingEnabled, privacyEnabled, anonymizeReports);
        IpTrackingInformationService service = new IpTrackingInformationService(config);
        
        // For any tracking and privacy settings, notices should be visible
        String notice = service.getIpTrackingNotice();
        
        // Notice should be present and informative
        assertThat(notice).isNotNull();
        assertThat(notice).isNotEmpty();
        
        if (trackingEnabled) {
            assertThat(notice.toLowerCase()).containsAnyOf("takip", "tracking", "kayit", "record", "aktif");
        }
        
        if (privacyEnabled) {
            assertThat(notice.toLowerCase()).containsAnyOf("gizlilik", "privacy", "korunmakta", "protected");
        }
    }

    @Property(trials = 100)
    public void statusIndicatorsShouldBeConsistent(boolean enabled, boolean privacyEnabled, boolean anonymizeReports) {
        IpTrackingConfig config = createConfig(enabled, privacyEnabled, anonymizeReports);
        IpTrackingInformationService service = new IpTrackingInformationService(config);
        
        // For any configuration, status indicators should be consistent
        IpTrackingInformationService.IpTrackingStatusIndicator statusIndicator = service.getStatusIndicator();
        IpTrackingInformationService.IpTrackingStatusIndicator privacyIndicator = service.getPrivacyStatusIndicator();
        
        // Status indicators should have all required fields
        assertThat(statusIndicator.getIconClass()).isNotNull().isNotEmpty();
        assertThat(statusIndicator.getColorClass()).isNotNull().isNotEmpty();
        assertThat(statusIndicator.getTitle()).isNotNull().isNotEmpty();
        assertThat(statusIndicator.getDescription()).isNotNull().isNotEmpty();
        
        assertThat(privacyIndicator.getIconClass()).isNotNull().isNotEmpty();
        assertThat(privacyIndicator.getColorClass()).isNotNull().isNotEmpty();
        assertThat(privacyIndicator.getTitle()).isNotNull().isNotEmpty();
        assertThat(privacyIndicator.getDescription()).isNotNull().isNotEmpty();
        
        // Status should be consistent with configuration
        if (enabled) {
            assertThat(statusIndicator.getTitle().toLowerCase()).containsAnyOf("aktif", "enabled");
        } else {
            assertThat(statusIndicator.getTitle().toLowerCase()).containsAnyOf("pasif", "disabled");
        }
    }

    @Test
    void ipTrackingInformationShouldBeConsistent() {
        // Test with specific configuration
        IpTrackingConfig config = new IpTrackingConfig();
        config.setEnabled(true);
        config.getPrivacy().setEnabled(true);
        config.getPrivacy().setAnonymizeReports(true);
        
        IpTrackingInformationService service = new IpTrackingInformationService(config);
        
        String statusDisplay = service.getIpTrackingStatusDisplay();
        String privacyInfo = service.getPrivacyInformationDisplay();
        String notice = service.getIpTrackingNotice();
        
        // All information should be consistent
        assertThat(statusDisplay).containsIgnoringCase("aktif");
        assertThat(privacyInfo).containsAnyOf("Gizlilik", "Privacy");
        assertThat(notice.toLowerCase()).containsAnyOf("takip", "tracking", "aktif");
    }

    // Helper method to create configuration
    private IpTrackingConfig createConfig(boolean enabled, boolean privacyEnabled, boolean anonymizeReports) {
        IpTrackingConfig config = new IpTrackingConfig();
        config.setEnabled(enabled);
        config.getPrivacy().setEnabled(privacyEnabled);
        config.getPrivacy().setAnonymizeReports(anonymizeReports);
        return config;
    }
}