package com.bidb.personetakip.config;

import com.bidb.personetakip.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for the Personnel Tracking System.
 * Configures Spring Security with JWT authentication, CORS, and endpoint protection.
 * 
 * Requirements: 9.2, 9.3, 4.1, 7.1
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;
    
    @Value("${cors.allowed-methods}")
    private String allowedMethods;
    
    @Value("${cors.allowed-headers}")
    private String allowedHeaders;
    
    @Value("${cors.allow-credentials}")
    private boolean allowCredentials;
    
    @Value("${cors.max-age}")
    private long maxAge;
    
    /**
     * Configures the password encoder using BCrypt with cost factor 12.
     * BCrypt is a strong hashing algorithm that includes salt automatically.
     * 
     * @return BCryptPasswordEncoder with strength 12
     * Requirement: 9.2 - BCrypt hashing with salt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    
    /**
     * Configures CORS to allow mobile application requests.
     * Configuration is loaded from application properties for environment-specific settings.
     * 
     * @return CorsConfigurationSource with allowed origins and methods
     * Requirement: 9.3 - Enable CORS for mobile application
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Parse allowed origins from comma-separated property
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        
        // If wildcard is specified, use pattern matching instead
        if (origins.contains("*")) {
            configuration.setAllowedOriginPatterns(List.of("*"));
        } else {
            configuration.setAllowedOrigins(origins);
        }
        
        // Parse and set allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        
        // Parse and set allowed headers
        configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        
        // Expose headers that clients can access
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type"
        ));
        
        // Set credentials flag from properties
        configuration.setAllowCredentials(allowCredentials);
        
        // Set max age for preflight cache from properties
        configuration.setMaxAge(maxAge);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    /**
     * Configures the security filter chain with HTTP security settings.
     * Disables CSRF for API endpoints and configures public/protected endpoints.
     * Registers JWT authentication filter in the filter chain.
     * 
     * @param http HttpSecurity to configure
     * @return SecurityFilterChain
     * @throws Exception if configuration fails
     * Requirements: 9.2, 9.3 - CSRF disabled for APIs, CORS enabled
     *               4.1 - Configure public endpoints (registration, login)
     *               7.1 - Configure protected endpoints (QR code, entry/exit)
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Enable CORS with the configuration defined above
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Disable CSRF for API endpoints (using JWT tokens instead)
            // CSRF protection is not needed for stateless JWT authentication
            .csrf(csrf -> csrf.disable())
            
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - registration and login
                .requestMatchers(
                    "/api/register/**",
                    "/api/auth/login",
                    "/api/mobil/login"
                ).permitAll()
                
                // Public web pages
                .requestMatchers(
                    "/",
                    "/register",
                    "/login",
                    "/error"
                ).permitAll()
                
                // Static resources
                .requestMatchers(
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico"
                ).permitAll()
                
                // Health check endpoints
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/info"
                ).permitAll()
                
                // Protected web pages - require authentication
                // QR code page (handled by WebController with redirect)
                .requestMatchers(
                    "/qrcode"
                ).permitAll()  // Allow access but WebController handles authentication redirect
                
                // Protected API endpoints - require authentication
                // QR code API endpoints
                .requestMatchers(
                    "/api/qrcode/**"
                ).authenticated()
                
                // Entry/exit endpoints
                .requestMatchers(
                    "/api/mobil/giris-cikis-kaydet"
                ).authenticated()
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            // Use stateless session management (no server-side sessions)
            // JWT tokens are used for authentication instead
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Add JWT authentication filter before UsernamePasswordAuthenticationFilter
            // This ensures JWT tokens are processed before standard authentication
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
