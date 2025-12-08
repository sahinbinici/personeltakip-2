package com.bidb.personetakip.controller;

import com.bidb.personetakip.model.QrCode;
import com.bidb.personetakip.model.User;
import com.bidb.personetakip.model.UserRole;
import com.bidb.personetakip.repository.QrCodeRepository;
import com.bidb.personetakip.repository.UserRepository;
import com.bidb.personetakip.security.JwtUtil;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for QR code generation and retrieval.
 * Requirements: 5.1 - Daily QR code generation
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QrCodeControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QrCodeRepository qrCodeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        // Set up MockMvc with Spring Security
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();

        // Clean up
        qrCodeRepository.deleteAll();
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
        testUser = userRepository.save(testUser);

        // Generate JWT token
        jwtToken = jwtUtil.generateToken(testUser);
    }

    @Test
    void testGetDailyQrCode_Success() throws Exception {
        mockMvc.perform(get("/api/qrcode/daily")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.qrCodeValue").isNotEmpty())
            .andExpect(jsonPath("$.validDate").value(LocalDate.now().toString()))
            .andExpect(jsonPath("$.usageCount").value(0))
            .andExpect(jsonPath("$.maxUsage").value(2));

        // Verify QR code was created in database
        var qrCodes = qrCodeRepository.findByUserIdAndValidDate(testUser.getId(), LocalDate.now());
        assertThat(qrCodes).isNotEmpty();
    }

    @Test
    void testGetDailyQrCode_Idempotent() throws Exception {
        // First request
        String response1 = mockMvc.perform(get("/api/qrcode/daily")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String qrCodeValue1 = objectMapper.readTree(response1).get("qrCodeValue").asText();

        // Second request - should return same QR code
        String response2 = mockMvc.perform(get("/api/qrcode/daily")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String qrCodeValue2 = objectMapper.readTree(response2).get("qrCodeValue").asText();

        assertThat(qrCodeValue1).isEqualTo(qrCodeValue2);
    }

    @Test
    void testGetDailyQrCode_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/qrcode/daily"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetDailyQrCode_InvalidToken() throws Exception {
        mockMvc.perform(get("/api/qrcode/daily")
                .header("Authorization", "Bearer invalid-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetQrCodeImage_Success() throws Exception {
        // First create a QR code
        QrCode qrCode = new QrCode();
        qrCode.setUserId(testUser.getId());
        qrCode.setQrCodeValue("TEST-QR-CODE-123");
        qrCode.setValidDate(LocalDate.now());
        qrCode.setUsageCount(0);
        qrCodeRepository.save(qrCode);

        byte[] imageBytes = mockMvc.perform(get("/api/qrcode/image")
                .param("qrCodeValue", "TEST-QR-CODE-123")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        // Verify image is not empty
        assertThat(imageBytes).isNotEmpty();
        assertThat(imageBytes.length).isGreaterThan(100);
    }

    @Test
    void testGetQrCodeImage_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/qrcode/image")
                .param("qrCodeValue", "TEST-QR-CODE-123"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetQrCodeImage_MissingParameter() throws Exception {
        mockMvc.perform(get("/api/qrcode/image")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testQrCodeFlow_EndToEnd() throws Exception {
        // Step 1: Get daily QR code
        String response = mockMvc.perform(get("/api/qrcode/daily")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String qrCodeValue = objectMapper.readTree(response).get("qrCodeValue").asText();

        // Step 2: Get QR code image
        byte[] imageBytes = mockMvc.perform(get("/api/qrcode/image")
                .param("qrCodeValue", qrCodeValue)
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        assertThat(imageBytes).isNotEmpty();

        // Step 3: Verify same QR code is returned on subsequent requests
        String response2 = mockMvc.perform(get("/api/qrcode/daily")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String qrCodeValue2 = objectMapper.readTree(response2).get("qrCodeValue").asText();
        assertThat(qrCodeValue).isEqualTo(qrCodeValue2);
    }
}
