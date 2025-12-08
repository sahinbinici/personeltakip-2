package com.bidb.personetakip.controller;

import com.bidb.personetakip.dto.EntryExitRequestDto;
import com.bidb.personetakip.model.*;
import com.bidb.personetakip.repository.EntryExitRecordRepository;
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
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for entry/exit recording.
 * Requirements: 8.5 - Store entry/exit record with all required fields
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MobileApiControllerIntegrationTest {

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
    private EntryExitRecordRepository entryExitRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;
    private String jwtToken;
    private QrCode testQrCode;

    @BeforeEach
    void setUp() {
        // Set up MockMvc with Spring Security
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();

        // Clean up
        entryExitRecordRepository.deleteAll();
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

        // Create test QR code
        testQrCode = new QrCode();
        testQrCode.setUserId(testUser.getId());
        testQrCode.setQrCodeValue("TEST-QR-CODE-123");
        testQrCode.setValidDate(LocalDate.now());
        testQrCode.setUsageCount(0);
        testQrCode = qrCodeRepository.save(testQrCode);
    }

    @Test
    void testRecordEntry_Success() throws Exception {
        EntryExitRequestDto request = new EntryExitRequestDto(
            "TEST-QR-CODE-123",
            LocalDateTime.now(),
            41.0082,  // Valid latitude
            28.9784   // Valid longitude
        );

        mockMvc.perform(post("/api/mobil/giris-cikis-kaydet")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(testUser.getId()))
            .andExpect(jsonPath("$.type").value("ENTRY"))
            .andExpect(jsonPath("$.latitude").value(41.0082))
            .andExpect(jsonPath("$.longitude").value(28.9784));

        // Verify record was created
        var records = entryExitRecordRepository.findAll();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getType()).isEqualTo(EntryExitType.ENTRY);

        // Verify QR code usage count was incremented
        var qrCode = qrCodeRepository.findByQrCodeValue("TEST-QR-CODE-123");
        assertThat(qrCode).isPresent();
        assertThat(qrCode.get().getUsageCount()).isEqualTo(1);
    }

    @Test
    void testRecordExit_Success() throws Exception {
        // Set usage count to 1 (already used for entry)
        testQrCode.setUsageCount(1);
        qrCodeRepository.save(testQrCode);

        EntryExitRequestDto request = new EntryExitRequestDto(
            "TEST-QR-CODE-123",
            LocalDateTime.now(),
            41.0082,
            28.9784
        );

        mockMvc.perform(post("/api/mobil/giris-cikis-kaydet")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("EXIT"));

        // Verify QR code usage count was incremented to 2
        var qrCode = qrCodeRepository.findByQrCodeValue("TEST-QR-CODE-123");
        assertThat(qrCode).isPresent();
        assertThat(qrCode.get().getUsageCount()).isEqualTo(2);
    }

    @Test
    void testRecordEntryExit_InvalidQrCode() throws Exception {
        EntryExitRequestDto request = new EntryExitRequestDto(
            "INVALID-QR-CODE",
            LocalDateTime.now(),
            41.0082,
            28.9784
        );

        mockMvc.perform(post("/api/mobil/giris-cikis-kaydet")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testRecordEntryExit_QrCodeExceededUsageLimit() throws Exception {
        // Set usage count to 2 (already used twice)
        testQrCode.setUsageCount(2);
        qrCodeRepository.save(testQrCode);

        EntryExitRequestDto request = new EntryExitRequestDto(
            "TEST-QR-CODE-123",
            LocalDateTime.now(),
            41.0082,
            28.9784
        );

        mockMvc.perform(post("/api/mobil/giris-cikis-kaydet")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testRecordEntryExit_InvalidGpsCoordinates_LatitudeTooHigh() throws Exception {
        EntryExitRequestDto request = new EntryExitRequestDto(
            "TEST-QR-CODE-123",
            LocalDateTime.now(),
            91.0,  // Invalid latitude (> 90)
            28.9784
        );

        mockMvc.perform(post("/api/mobil/giris-cikis-kaydet")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid GPS coordinates"));
    }

    @Test
    void testRecordEntryExit_InvalidGpsCoordinates_LatitudeTooLow() throws Exception {
        EntryExitRequestDto request = new EntryExitRequestDto(
            "TEST-QR-CODE-123",
            LocalDateTime.now(),
            -91.0,  // Invalid latitude (< -90)
            28.9784
        );

        mockMvc.perform(post("/api/mobil/giris-cikis-kaydet")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid GPS coordinates"));
    }

    @Test
    void testRecordEntryExit_InvalidGpsCoordinates_LongitudeTooHigh() throws Exception {
        EntryExitRequestDto request = new EntryExitRequestDto(
            "TEST-QR-CODE-123",
            LocalDateTime.now(),
            41.0082,
            181.0  // Invalid longitude (> 180)
        );

        mockMvc.perform(post("/api/mobil/giris-cikis-kaydet")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid GPS coordinates"));
    }

    @Test
    void testRecordEntryExit_InvalidGpsCoordinates_LongitudeTooLow() throws Exception {
        EntryExitRequestDto request = new EntryExitRequestDto(
            "TEST-QR-CODE-123",
            LocalDateTime.now(),
            41.0082,
            -181.0  // Invalid longitude (< -180)
        );

        mockMvc.perform(post("/api/mobil/giris-cikis-kaydet")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid GPS coordinates"));
    }

    @Test
    void testRecordEntryExit_Unauthorized() throws Exception {
        EntryExitRequestDto request = new EntryExitRequestDto(
            "TEST-QR-CODE-123",
            LocalDateTime.now(),
            41.0082,
            28.9784
        );

        mockMvc.perform(post("/api/mobil/giris-cikis-kaydet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testRecordEntryExit_QrCodeBelongsToAnotherUser() throws Exception {
        // Create another user
        User anotherUser = new User();
        anotherUser.setTcNo("98765432109");
        anotherUser.setPersonnelNo("P002");
        anotherUser.setFirstName("Another");
        anotherUser.setLastName("User");
        anotherUser.setMobilePhone("5559876543");
        anotherUser.setPasswordHash(passwordEncoder.encode("Test@1234"));
        anotherUser.setRole(UserRole.NORMAL_USER);
        anotherUser = userRepository.save(anotherUser);

        // Create QR code for another user
        QrCode anotherQrCode = new QrCode();
        anotherQrCode.setUserId(anotherUser.getId());
        anotherQrCode.setQrCodeValue("ANOTHER-QR-CODE");
        anotherQrCode.setValidDate(LocalDate.now());
        anotherQrCode.setUsageCount(0);
        qrCodeRepository.save(anotherQrCode);

        // Try to use another user's QR code
        EntryExitRequestDto request = new EntryExitRequestDto(
            "ANOTHER-QR-CODE",
            LocalDateTime.now(),
            41.0082,
            28.9784
        );

        mockMvc.perform(post("/api/mobil/giris-cikis-kaydet")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testRecordEntryExit_RateLimiting() throws Exception {
        EntryExitRequestDto request = new EntryExitRequestDto(
            "TEST-QR-CODE-123",
            LocalDateTime.now(),
            41.0082,
            28.9784
        );

        // Make 20 requests (should all succeed, but only first 2 will actually record due to QR code limit)
        // We need to create new QR codes for each request
        for (int i = 0; i < 20; i++) {
            // Create a new QR code for each request
            QrCode qrCode = new QrCode();
            qrCode.setUserId(testUser.getId());
            qrCode.setQrCodeValue("TEST-QR-CODE-" + i);
            qrCode.setValidDate(LocalDate.now());
            qrCode.setUsageCount(0);
            qrCodeRepository.save(qrCode);

            EntryExitRequestDto req = new EntryExitRequestDto(
                "TEST-QR-CODE-" + i,
                LocalDateTime.now(),
                41.0082,
                28.9784
            );

            mockMvc.perform(post("/api/mobil/giris-cikis-kaydet")
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
        }

        // 21st request should be rate limited
        QrCode qrCode = new QrCode();
        qrCode.setUserId(testUser.getId());
        qrCode.setQrCodeValue("TEST-QR-CODE-21");
        qrCode.setValidDate(LocalDate.now());
        qrCode.setUsageCount(0);
        qrCodeRepository.save(qrCode);

        EntryExitRequestDto req = new EntryExitRequestDto(
            "TEST-QR-CODE-21",
            LocalDateTime.now(),
            41.0082,
            28.9784
        );

        mockMvc.perform(post("/api/mobil/giris-cikis-kaydet")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.message").value("Too many requests. Please try again later."));
    }

    @Test
    void testEntryExitFlow_EndToEnd() throws Exception {
        // Step 1: Record entry
        EntryExitRequestDto entryRequest = new EntryExitRequestDto(
            "TEST-QR-CODE-123",
            LocalDateTime.now(),
            41.0082,
            28.9784
        );

        mockMvc.perform(post("/api/mobil/giris-cikis-kaydet")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(entryRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("ENTRY"));

        // Step 2: Record exit
        EntryExitRequestDto exitRequest = new EntryExitRequestDto(
            "TEST-QR-CODE-123",
            LocalDateTime.now(),
            41.0082,
            28.9784
        );

        mockMvc.perform(post("/api/mobil/giris-cikis-kaydet")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(exitRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("EXIT"));

        // Step 3: Verify both records exist
        var records = entryExitRecordRepository.findAll();
        assertThat(records).hasSize(2);
        assertThat(records.get(0).getType()).isEqualTo(EntryExitType.ENTRY);
        assertThat(records.get(1).getType()).isEqualTo(EntryExitType.EXIT);

        // Step 4: Try to use QR code again (should fail)
        mockMvc.perform(post("/api/mobil/giris-cikis-kaydet")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(exitRequest)))
            .andExpect(status().isBadRequest());
    }
}
