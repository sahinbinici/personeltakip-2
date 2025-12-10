package com.bidb.personetakip.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT Authentication Filter that processes JWT tokens from request headers.
 * Extends OncePerRequestFilter to ensure single execution per request.
 * 
 * Requirements: 7.1, 8.1, 9.4
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * Filters incoming requests to extract and validate JWT tokens.
     * Sets authentication in SecurityContext if token is valid.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain Filter chain to continue processing
     * @throws ServletException if servlet error occurs
     * @throws IOException if I/O error occurs
     * Requirements: 7.1 - Extract JWT from Authorization header
     *               8.1 - Validate token and set authentication
     *               9.4 - Validate token before processing requests
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Extract JWT token from Authorization header
            String jwt = extractJwtFromRequest(request);
            
            // Debug logging
            logger.info("JWT Filter - Request URI: {}", request.getRequestURI());
            logger.info("JWT Filter - Token found: {}", jwt != null ? "YES" : "NO");
            if (jwt != null) {
                logger.info("JWT Filter - Token valid: {}", jwtUtil.validateToken(jwt));
            }
            
            // If token exists and is valid, set authentication
            if (jwt != null && jwtUtil.validateToken(jwt)) {
                // Extract user details from token
                String tcNo = jwtUtil.extractTcNo(jwt);
                Long userId = jwtUtil.extractUserId(jwt);
                String role = jwtUtil.extractRole(jwt);
                
                // Create authentication token with user details
                // Principal is set to userId for easy access in controllers
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userId,  // Principal (user ID)
                        null,    // Credentials (not needed after authentication)
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                
                // Set additional details (TC No, role) in authentication
                JwtAuthenticationDetails details = new JwtAuthenticationDetails(
                    tcNo, userId, role
                );
                authentication.setDetails(details);
                
                // Set authentication in SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.info("JWT Filter - Authentication set for user: {} with role: {}", userId, role);
            }
        } catch (Exception e) {
            // Log the error but don't block the request
            // Invalid tokens will result in unauthenticated requests
            logger.error("Cannot set user authentication: " + e.getMessage());
        }
        
        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extracts JWT token from the Authorization header or cookies.
     * Expected format: "Bearer {token}" in header or "jwt" cookie
     * 
     * @param request HTTP request
     * @return JWT token string or null if not present
     * Requirement: 7.1 - Extract JWT from Authorization header or cookies
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        // First, try to get token from Authorization header
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // If not found in header, try to get from cookies
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }
    
    /**
     * Custom authentication details class to store JWT-specific information.
     */
    public static class JwtAuthenticationDetails {
        private final String tcNo;
        private final Long userId;
        private final String role;
        
        public JwtAuthenticationDetails(String tcNo, Long userId, String role) {
            this.tcNo = tcNo;
            this.userId = userId;
            this.role = role;
        }
        
        public String getTcNo() {
            return tcNo;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public String getRole() {
            return role;
        }
    }
}
