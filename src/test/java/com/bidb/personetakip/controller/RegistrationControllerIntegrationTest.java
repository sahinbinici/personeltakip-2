package com.bidb.personetakip.controller;

import com.bidb.personetakip.dto.ExternalPersonnelDto;
import com.bidb.personetakip.dto.OtpVerificationRequest;
import com.bidb.personetakip.dto.PersonnelValidationRequest;
import com.bidb.personetakip.dto.RegistrationCompleteRequest;
import com.bidb.personetakip.dto.UserDto;
import com.bidb.personetakip.model.ExternalPersonnel;
import com.bidb.personetakip.model.OtpVerification;
import com.bidb.personetakip.model.User;
import com.bidb.personetakip.repository.OtpVerificationRepository;
import com.bidb.personetakip.repository.UserRepository;
import com.bidb.personetakip.repository.external.ExternalPersonnelRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for registration flow end-to-end.
 * Requirements: 1.1 - Personnel validation against external database
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RegistrationControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExternalPersonnelRepository externalPersonnelRepository;

    @Autowired
    private OtpVerificationRepository otpVerificationRepository;

    @Autowired
    private UserRepository userRepository;

    private ExternalPersonnel testPersonnel;

    @BeforeEach
    void setUp() {
        // Set up MockMvc
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Clean up
        otpVerificationRepository.deleteAll();
        userRepository.deleteAll();
        externalPersonnelRepository.deleteAll();

        // Create test personnel in external database
        testPersonnel = new ExternalPersonnel();
        testPersonnel.setUserId(12345L);
        testPersonnel.setTcNo("12345678901");
        testPersonnel.setPersonnelNo("P001");
        testPersonnel.setFirstName("Test");
        testPersonnel.setLastName("User");
        testPersonnel.setMobilePhone("5551234567");
        externalPersonnelRepository.save(testPersonnel);
    }

    @Test
    void testValidatePersonnel_Success() throws Exception {
        PersonnelValidationRequest request = new PersonnelValidationRequest(
            "12345678901",
            "P001"
        );

        mockMvc.perform(post("/api/register/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(12345))
            .andExpect(jsonPath("$.tcNo").value("12345678901"))
            .andExpect(jsonPath("$.personnelNo").value("P001"))
            .andExpect(jsonPath("$.firstName").value("Test"))
            .andExpect(jsonPath("$.lastName").value("User"))
            .andExpect(jsonPath("$.mobilePhone").value("5551234567"));
    }

    @Test
    void testValidatePersonnel_NotFound() throws Exception {
        PersonnelValidationRequest request = new PersonnelValidationRequest(
            "99999999999",
            "P999"
        );

        mockMvc.perform(post("/api/register/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void testValidatePersonnel_InvalidTcNo() throws Exception {
        PersonnelValidationRequest request = new PersonnelValidationRequest(
            "123",  // Invalid TC No
            "P001"
        );

        mockMvc.perform(post("/api/register/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testSendOtp_Success() throws Exception {
        Map<String, String> request = Map.of(
            "tcNo", "12345678901",
            "mobilePhone", "5551234567"
        );

        mockMvc.perform(post("/api/register/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("OTP sent successfully"));

        // Verify OTP was created in database
        var otpList = otpVerificationRepository.findByTcNoAndVerifiedFalse("12345678901");
        assertThat(otpList).isNotEmpty();
        assertThat(otpList.get(0).getOtpCode()).hasSize(6);
    }

    @Test
    void testSendOtp_MissingTcNo() throws Exception {
        Map<String, String> request = Map.of(
            "mobilePhone", "5551234567"
        );

        mockMvc.perform(post("/api/register/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("TC No is required"));
    }

    @Test
    void testVerifyOtp_Success() throws Exception {
        // Create OTP in database
        OtpVerification otp = new OtpVerification();
        otp.setTcNo("12345678901");
        otp.setOtpCode("123456");
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setVerified(false);
        otpVerificationRepository.save(otp);

        OtpVerificationRequest request = new OtpVerificationRequest(
            "12345678901",
            "123456"
        );

        mockMvc.perform(post("/api/register/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.verified").value(true))
            .andExpect(jsonPath("$.message").value("OTP verified successfully"));
    }

    @Test
    void testVerifyOtp_InvalidCode() throws Exception {
        // Create OTP in database
        OtpVerification otp = new OtpVerification();
        otp.setTcNo("12345678901");
        otp.setOtpCode("123456");
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setVerified(false);
        otpVerificationRepository.save(otp);

        OtpVerificationRequest request = new OtpVerificationRequest(
            "12345678901",
            "999999"  // Wrong code
        );

        mockMvc.perform(post("/api/register/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testVerifyOtp_ExpiredCode() throws Exception {
        // Create expired OTP in database
        OtpVerification otp = new OtpVerification();
        otp.setTcNo("12345678901");
        otp.setOtpCode("123456");
        otp.setExpiresAt(LocalDateTime.now().minusMinutes(1));  // Expired
        otp.setVerified(false);
        otpVerificationRepository.save(otp);

        OtpVerificationRequest request = new OtpVerificationRequest(
            "12345678901",
            "123456"
        );

        mockMvc.perform(post("/api/register/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testCompleteRegistration_Success() throws Exception {
        // Create verified OTP
        OtpVerification otp = new OtpVerification();
        otp.setTcNo("12345678901");
        otp.setOtpCode("123456");
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setVerified(true);
        otpVerificationRepository.save(otp);

        RegistrationCompleteRequest request = new RegistrationCompleteRequest(
            "12345678901",
            "Test@1234"  // Valid password
        );

        mockMvc.perform(post("/api/register/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tcNo").value("12345678901"))
            .andExpect(jsonPath("$.role").value("NORMAL_USER"));

        // Verify user was created in database
        var userOpt = userRepository.findByTcNo("12345678901");
        assertThat(userOpt).isPresent();
        assertThat(userOpt.get().getPasswordHash()).isNotBlank();
    }

    @Test
    void testCompleteRegistration_WeakPassword() throws Exception {
        // Create verified OTP
        OtpVerification otp = new OtpVerification();
        otp.setTcNo("12345678901");
        otp.setOtpCode("123456");
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setVerified(true);
        otpVerificationRepository.save(otp);

        RegistrationCompleteRequest request = new RegistrationCompleteRequest(
            "12345678901",
            "weak"  // Invalid password
        );

        mockMvc.perform(post("/api/register/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testCompleteRegistration_OtpNotVerified() throws Exception {
        // Create unverified OTP
        OtpVerification otp = new OtpVerification();
        otp.setTcNo("12345678901");
        otp.setOtpCode("123456");
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setVerified(false);  // Not verified
        otpVerificationRepository.save(otp);

        RegistrationCompleteRequest request = new RegistrationCompleteRequest(
            "12345678901",
            "Test@1234"
        );

        mockMvc.perform(post("/api/register/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testFullRegistrationFlow_EndToEnd() throws Exception {
        // Step 1: Validate personnel
        PersonnelValidationRequest validateRequest = new PersonnelValidationRequest(
            "12345678901",
            "P001"
        );

        mockMvc.perform(post("/api/register/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validateRequest)))
            .andExpect(status().isOk());

        // Step 2: Send OTP
        Map<String, String> otpRequest = Map.of(
            "tcNo", "12345678901",
            "mobilePhone", "5551234567"
        );

        mockMvc.perform(post("/api/register/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(otpRequest)))
            .andExpect(status().isOk());

        // Get the OTP from database
        var otpList = otpVerificationRepository.findByTcNoAndVerifiedFalse("12345678901");
        assertThat(otpList).isNotEmpty();
        String otpCode = otpList.get(0).getOtpCode();

        // Step 3: Verify OTP
        OtpVerificationRequest verifyRequest = new OtpVerificationRequest(
            "12345678901",
            otpCode
        );

        mockMvc.perform(post("/api/register/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.verified").value(true));

        // Step 4: Complete registration
        RegistrationCompleteRequest completeRequest = new RegistrationCompleteRequest(
            "12345678901",
            "Test@1234"
        );

        mockMvc.perform(post("/api/register/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(completeRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tcNo").value("12345678901"))
            .andExpect(jsonPath("$.role").value("NORMAL_USER"));

        // Verify final state
        var userOpt = userRepository.findByTcNo("12345678901");
        assertThat(userOpt).isPresent();
        User user = userOpt.get();
        assertThat(user.getFirstName()).isEqualTo("Test");
        assertThat(user.getLastName()).isEqualTo("User");
        assertThat(user.getPersonnelNo()).isEqualTo("P001");
        assertThat(user.getMobilePhone()).isEqualTo("5551234567");
    }
}
