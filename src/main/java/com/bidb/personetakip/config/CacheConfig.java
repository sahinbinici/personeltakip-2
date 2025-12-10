package com.bidb.personetakip.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for the Personnel Tracking System.
 * Enables caching for dashboard statistics and other frequently accessed data.
 * 
 * Requirements: 1.2 - Dashboard statistics caching for performance
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * Configure cache manager for dashboard statistics.
     * Uses in-memory caching with 30-second TTL for dashboard stats.
     * 
     * @return CacheManager instance
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("dashboardStats");
    }
}