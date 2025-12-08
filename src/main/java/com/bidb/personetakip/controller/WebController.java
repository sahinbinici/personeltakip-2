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
     * If user is already authenticated, redirects to QR code page.
     * 
     * @param request HTTP request to check for existing authentication
     * @return login template name or redirect to QR code page
     * Requirement: 4.1 - Handle authentication redirects
     */
    @GetMapping("/login")
    public String login(HttpServletRequest request) {
        // Check if user is already authenticated
        String token = extractTokenFromRequest(request);
        if (token != null && jwtUtil.validateToken(token)) {
            return "redirect:/qrcode";
        }
        return "login";
    }

    /**
     * Serves the QR code page.
     * Protected endpoint - requires authentication.
     * If user is not authenticated, redirects to login page.
     * 
     * @param request HTTP request to check authentication
     * @param model Model to pass user information to the view
     * @return qrcode template name or redirect to login page
     * Requirements: 4.1 - Handle authentication redirects
     *               5.1 - Protected QR code page
     */
    @GetMapping("/qrcode")
    public String qrcode(HttpServletRequest request, Model model) {
        // Check if user is authenticated
        String token = extractTokenFromRequest(request);
        
        if (token == null || !jwtUtil.validateToken(token)) {
            // Not authenticated - redirect to login
            return "redirect:/login";
        }
        
        // Extract user information from token and add to model
        try {
            String tcNo = jwtUtil.extractTcNo(token);
            Long userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.extractRole(token);
            
            model.addAttribute("tcNo", tcNo);
            model.addAttribute("userId", userId);
            model.addAttribute("role", role);
            model.addAttribute("token", token);
        } catch (Exception e) {
            // Token is invalid - redirect to login
            return "redirect:/login";
        }
        
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
