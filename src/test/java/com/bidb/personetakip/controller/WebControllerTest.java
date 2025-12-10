package com.bidb.personetakip.controller;

import com.bidb.personetakip.model.User;
import com.bidb.personetakip.model.UserRole;
import com.bidb.personetakip.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for WebController.
 * Verifies that web pages return correct view names and handle authentication redirects.
 * 
 * Requirements: 4.1, 5.1
 */
class WebControllerTest {

    @Mock
    private JwtUtil jwtUtil;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private Model model;
    
    @InjectMocks
    private WebController controller;
    
    private String validToken;
    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setTcNo("12345678901");
        testUser.setPersonnelNo("P001");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(UserRole.NORMAL_USER);
        
        validToken = "valid.jwt.token";
    }

    @Test
    void testRegistrationPageReturnsCorrectView() {
        String viewName = controller.registration();
        assertEquals("registration", viewName);
    }

    @Test
    void testLoginPageReturnsCorrectView() {
        // Login page now always returns "login" - authentication is handled client-side
        String viewName = controller.login();
        assertEquals("login", viewName);
    }

    @Test
    void testQrCodePageReturnsCorrectView() {
        // QR code page now always returns "qrcode" - authentication is handled client-side
        String viewName = controller.qrcode();
        assertEquals("qrcode", viewName);
    }

    @Test
    void testRootRedirectsToLogin() {
        String redirect = controller.index();
        assertEquals("redirect:/login", redirect);
    }
}
