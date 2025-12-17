package com.bidb.personetakip.service;

import com.bidb.personetakip.config.IpTrackingConfig;
import com.bidb.personetakip.repository.IpAddressLogRepository;
import org.mockito.Mockito;

/**
 * Helper class for creating test configurations and mock objects
 */
public class TestConfigurationHelper {
    
    /**
     * Creates a default IP tracking configuration for testing
     */
    public static IpTrackingConfig createDefaultConfig() {
        IpTrackingConfig config = new IpTrackingConfig();
        config.setEnabled(true);
        
        // Set privacy defaults
        config.getPrivacy().setEnabled(false);
        config.getPrivacy().setAnonymizeReports(false);
        config.getPrivacy().setAuditLogging(true);
        config.getPrivacy().setRetentionDays(365);
        config.getPrivacy().setRequireConsent(false);
        
        // Set anonymization defaults
        config.getAnonymization().setEnabled(true);
        config.getAnonymization().setMethod(IpTrackingConfig.AnonymizationMethod.MASK);
        config.getAnonymization().setIpv4PreserveOctets(3);
        config.getAnonymization().setIpv6PreserveGroups(4);
        config.getAnonymization().setMaskCharacter("x");
        
        // Set performance defaults
        config.getPerformance().setCaptureTimeoutMs(1000);
        config.getPerformance().setAsyncLogging(true);
        config.getPerformance().setCacheSize(1000);
        config.getPerformance().setCacheTtlSeconds(300);
        config.getPerformance().setCacheValidation(true);
        config.getPerformance().setBatchSize(100);
        
        return config;
    }
    
    /**
     * Creates a configuration service with default settings
     */
    public static IpTrackingConfigurationService createConfigService() {
        return new IpTrackingConfigurationService(createDefaultConfig());
    }
    
    /**
     * Creates an IP address service with mocked security service
     */
    public static IpAddressService createIpAddressService() {
        IpSecurityService mockSecurityService = Mockito.mock(IpSecurityService.class);
        
        // Configure mock behavior for security service
        Mockito.when(mockSecurityService.sanitizeIpAddress(Mockito.anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.doNothing().when(mockSecurityService).validateSecureStorage(Mockito.anyString());
        
        return new IpAddressServiceImpl(createConfigService(), mockSecurityService);
    }
    
    /**
     * Creates an IP security service with mocked dependencies
     */
    public static IpSecurityService createIpSecurityService() {
        IpAddressLogRepository mockRepository = Mockito.mock(IpAddressLogRepository.class);
        IpAddressService mockIpAddressService = Mockito.mock(IpAddressService.class);
        IpTrackingConfigurationService configService = createConfigService();
        
        // Configure mock behavior
        Mockito.when(mockIpAddressService.getUnknownIpDefault()).thenReturn("Unknown");
        Mockito.when(mockIpAddressService.isValidIpAddress(Mockito.anyString())).thenReturn(true);
        
        return new IpSecurityServiceImpl(mockRepository, mockIpAddressService, configService);
    }
    
    /**
     * Creates an IP privacy service with mocked dependencies
     */
    public static IpPrivacyService createIpPrivacyService() {
        IpAddressLogRepository mockRepository = Mockito.mock(IpAddressLogRepository.class);
        IpAddressService ipAddressService = createIpAddressService();
        IpTrackingConfigurationService configService = createConfigService();
        
        return new IpPrivacyServiceImpl(mockRepository, ipAddressService, configService);
    }
}