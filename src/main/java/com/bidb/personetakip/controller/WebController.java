package com.bidb.personetakip.controller;

import com.bidb.personetakip.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web controller for serving HTML pages.
 * Handles authentication redirects for protected pages.
 * 
 * Requirements: 4.1, 5.1
 */
@Controller
public class WebController {
    
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Serves the registration page.
     * Public endpoint - no authentication required.
     * 
     * @return registration template name
     */
    @GetMapping("/register")
    public String registration() {
        return "registration";
    }

    /**
     * Serves the login page.
     * Public endpoint - no authentication required.
     * Authentication check is handled client-side to prevent redirect loops.
     * 
     * @return login template name
     * Requirement: 4.1 - Handle authentication redirects
     */
    @GetMapping("/login")
    public String login() {
        // Just serve the page - authentication check is handled client-side
        // login.js will check localStorage for token and redirect to qrcode if needed
        return "login";
    }

    /**
     * Serves the QR code page.
     * Public endpoint - authentication is checked client-side via JavaScript.
     * This prevents redirect loops and allows proper token handling.
     * 
     * @return qrcode template name
     * Requirements: 4.1 - Handle authentication redirects
     *               5.1 - Protected QR code page
     */
    @GetMapping("/qrcode")
    public String qrcode() {
        // Just serve the page - authentication is handled client-side
        // qrcode.js will check localStorage for token and redirect to login if needed
        return "qrcode";
    }

    /**
     * Redirects root to login page.
     * 
     * @return redirect to login
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }
    
    /**
     * Test page for debugging authentication.
     * 
     * @return test-auth template name
     */
    @GetMapping("/test-auth")
    public String testAuth() {
        return "test-auth";
    }
    
    /**
     * Extracts JWT token from the request.
     * Checks Authorization header first, then falls back to cookies.
     * 
     * @param request HTTP request
     * @return JWT token or null if not found
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        // Check Authorization header first
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // Fall back to checking cookies
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName()) || "token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }
}
