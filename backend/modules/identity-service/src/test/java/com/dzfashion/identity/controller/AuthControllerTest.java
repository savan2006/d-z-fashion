package com.dzfashion.identity.controller;

import com.dzfashion.identity.dto.request.ForgotPasswordInitiateRequest;
import com.dzfashion.identity.dto.request.LoginRequest;
import com.dzfashion.identity.dto.request.SignupCompleteRequest;
import com.dzfashion.identity.dto.request.SignupInitiateRequest;
import com.dzfashion.identity.dto.request.VerifyOtpRequest;
import com.dzfashion.identity.dto.response.ActionResponse;
import com.dzfashion.identity.dto.response.AuthResponse;
import com.dzfashion.identity.dto.response.OtpInitiateResponse;
import com.dzfashion.identity.dto.response.UserSummaryResponse;
import com.dzfashion.identity.dto.response.VerifyOtpResponse;
import com.dzfashion.identity.enums.Role;
import com.dzfashion.identity.enums.UserStatus;
import com.dzfashion.identity.security.JwtAuthenticationFilter;
import com.dzfashion.identity.security.JwtTokenService;
import com.dzfashion.identity.security.SecurityConfig;
import com.dzfashion.identity.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("Initiate signup with valid payload returns 200 OK")
    void testInitiateSignupValid() throws Exception {
        SignupInitiateRequest request = new SignupInitiateRequest("sita_m", "sita@example.com");
        when(authService.initiateSignup(any(SignupInitiateRequest.class)))
                .thenReturn(new OtpInitiateResponse("OTP sent", "sita@example.com", 60));

        mockMvc.perform(post("/api/v1/auth/signup/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("sita@example.com"))
                .andExpect(jsonPath("$.resendAvailableInSeconds").value(60));
    }

    @Test
    @DisplayName("Initiate signup with invalid email format returns 400 Bad Request")
    void testInitiateSignupInvalidEmail() throws Exception {
        SignupInitiateRequest request = new SignupInitiateRequest("sita_m", "not-an-email");

        mockMvc.perform(post("/api/v1/auth/signup/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("Verify signup OTP returns verification ticket (200 OK)")
    void testVerifySignupOtp() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest("sita@example.com", "1234", null);
        when(authService.verifySignupOtp(any(VerifyOtpRequest.class)))
                .thenReturn(new VerifyOtpResponse("OTP verified", "sita@example.com", "ticket-jwt-token", 900));

        mockMvc.perform(post("/api/v1/auth/signup/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationTicket").value("ticket-jwt-token"));
    }

    @Test
    @DisplayName("Complete signup creates customer account (201 Created)")
    void testCompleteSignup() throws Exception {
        SignupCompleteRequest request = new SignupCompleteRequest(
                "sita@example.com",
                "ticket-jwt-token",
                "Password123!",
                "Sita Mohanty"
        );
        UUID userId = UUID.randomUUID();
        UserSummaryResponse summary = new UserSummaryResponse(userId, "sita_m", "sita@example.com", Role.CUSTOMER, UserStatus.ACTIVE);
        AuthResponse authResponse = new AuthResponse("access-token-123", "refresh-token-456", "Bearer", 900, summary);

        when(authService.completeSignup(any(SignupCompleteRequest.class), any())).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/signup/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("Login with valid credentials returns tokens (200 OK)")
    void testLoginValid() throws Exception {
        LoginRequest request = new LoginRequest("sita@example.com", "Password123!");
        UUID userId = UUID.randomUUID();
        UserSummaryResponse summary = new UserSummaryResponse(userId, "sita_m", "sita@example.com", Role.CUSTOMER, UserStatus.ACTIVE);
        AuthResponse authResponse = new AuthResponse("access-token-123", "refresh-token-456", "Bearer", 900, summary);

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("Forgot password initiate returns generic safe response (200 OK)")
    void testForgotPasswordInitiate() throws Exception {
        ForgotPasswordInitiateRequest request = new ForgotPasswordInitiateRequest("unknown@example.com");
        when(authService.initiateForgotPassword(any(ForgotPasswordInitiateRequest.class)))
                .thenReturn(new ActionResponse("If an account exists, a password reset code has been sent."));

        mockMvc.perform(post("/api/v1/auth/forgot-password/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}
