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
    void testLoginPageReturnsCorrectViewWhenNotAuthenticated() {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(null);
        
        String viewName = controller.login(request);
        assertEquals("login", viewName);
    }
    
    @Test
    void testLoginPageRedirectsToQrCodeWhenAuthenticated() {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        
        String viewName = controller.login(request);
        assertEquals("redirect:/qrcode", viewName);
    }
    
    @Test
    void testLoginPageRedirectsToQrCodeWhenAuthenticatedViaCookie() {
        Cookie jwtCookie = new Cookie("jwt", validToken);
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});
        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        
        String viewName = controller.login(request);
        assertEquals("redirect:/qrcode", viewName);
    }

    @Test
    void testQrCodePageRedirectsToLoginWhenNotAuthenticated() {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(null);
        
        String viewName = controller.qrcode(request, model);
        assertEquals("redirect:/login", viewName);
    }
    
    @Test
    void testQrCodePageRedirectsToLoginWhenTokenInvalid() {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtUtil.validateToken(validToken)).thenReturn(false);
        
        String viewName = controller.qrcode(request, model);
        assertEquals("redirect:/login", viewName);
    }
    
    @Test
    void testQrCodePageReturnsCorrectViewWhenAuthenticated() {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.extractTcNo(validToken)).thenReturn(testUser.getTcNo());
        when(jwtUtil.extractUserId(validToken)).thenReturn(testUser.getId());
        when(jwtUtil.extractRole(validToken)).thenReturn(testUser.getRole().name());
        
        String viewName = controller.qrcode(request, model);
        
        assertEquals("qrcode", viewName);
        verify(model).addAttribute("tcNo", testUser.getTcNo());
        verify(model).addAttribute("userId", testUser.getId());
        verify(model).addAttribute("role", testUser.getRole().name());
        verify(model).addAttribute("token", validToken);
    }
    
    @Test
    void testQrCodePageRedirectsToLoginWhenTokenExtractionFails() {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.extractTcNo(validToken)).thenThrow(new RuntimeException("Invalid token"));
        
        String viewName = controller.qrcode(request, model);
        assertEquals("redirect:/login", viewName);
    }

    @Test
    void testRootRedirectsToLogin() {
        String redirect = controller.index();
        assertEquals("redirect:/login", redirect);
    }
}
