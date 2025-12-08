package com.bidb.personetakip.controller;

import com.bidb.personetakip.dto.LoginRequest;
import com.bidb.personetakip.model.User;
import com.bidb.personetakip.model.UserRole;
import com.bidb.personetakip.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for authentication flow.
 * Requirements: 4.1 - User authentication with TC and password
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthenticationControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Set up MockMvc
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Clean up
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setTcNo("12345678901");
        testUser.setPersonnelNo("P001");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setMobilePhone("5551234567");
        testUser.setPasswordHash(passwordEncoder.encode("Test@1234"));
        testUser.setRole(UserRole.NORMAL_USER);
        userRepository.save(testUser);
    }

    @Test
    void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest(
            "12345678901",
            "Test@1234"
        );

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(1800))
            .andExpect(jsonPath("$.user.tcNo").value("12345678901"))
            .andExpect(jsonPath("$.user.role").value("NORMAL_USER"));
    }

    @Test
    void testLogin_InvalidPassword() throws Exception {
        LoginRequest request = new LoginRequest(
            "12345678901",
            "WrongPassword"
        );

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogin_UserNotFound() throws Exception {
        LoginRequest request = new LoginRequest(
            "99999999999",
            "Test@1234"
        );

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogin_InvalidTcNoFormat() throws Exception {
        LoginRequest request = new LoginRequest(
            "123",  // Invalid format
            "Test@1234"
        );

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testLogin_MissingPassword() throws Exception {
        LoginRequest request = new LoginRequest(
            "12345678901",
            ""  // Empty password
        );

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testLogin_RateLimiting() throws Exception {
        LoginRequest request = new LoginRequest(
            "12345678901",
            "Test@1234"
        );

        // Make 10 successful requests (should all succeed)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        }

        // 11th request should be rate limited
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.message").value("Too many login attempts. Please try again later."));
    }

    @Test
    void testLogin_JwtTokenContainsUserInfo() throws Exception {
        LoginRequest request = new LoginRequest(
            "12345678901",
            "Test@1234"
        );

        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        // Verify response structure
        var authToken = objectMapper.readTree(response);
        assert authToken.has("token");
        assert authToken.has("user");
        assert authToken.get("user").has("id");
        assert authToken.get("user").has("tcNo");
        assert authToken.get("user").has("firstName");
        assert authToken.get("user").has("lastName");
        assert authToken.get("user").has("role");
    }
}
