package com.dzfashion.payment.controller;

import com.dzfashion.payment.dto.request.RecordCodCollectedRequest;
import com.dzfashion.payment.dto.request.RecordCodPendingRequest;
import com.dzfashion.payment.dto.request.ReversePaymentRequest;
import com.dzfashion.payment.dto.request.SimulatePaymentRequest;
import com.dzfashion.payment.dto.response.PaymentResponse;
import com.dzfashion.payment.enums.PaymentMethod;
import com.dzfashion.payment.enums.PaymentStatus;
import com.dzfashion.payment.enums.Role;
import com.dzfashion.payment.security.JwtAuthenticationFilter;
import com.dzfashion.payment.security.JwtTokenService;
import com.dzfashion.payment.security.SecurityConfig;
import com.dzfashion.payment.security.UserPrincipal;
import com.dzfashion.payment.service.PaymentRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PaymentRecordService paymentRecordService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    private UsernamePasswordAuthenticationToken createAuth(UUID id, String username, Role role) {
        UserPrincipal principal = new UserPrincipal(id, username, username + "@example.com", role);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    @DisplayName("POST /api/v1/payments/simulate simulates online payment successfully")
    void simulatePayment_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        SimulatePaymentRequest request = new SimulatePaymentRequest(orderId, customerId, new BigDecimal("1999.00"));

        PaymentResponse response = new PaymentResponse(
                UUID.randomUUID(),
                orderId,
                customerId,
                new BigDecimal("1999.00"),
                "INR",
                PaymentMethod.SIMULATED_ONLINE,
                PaymentStatus.SUCCESS,
                "SIM-20261116-123456",
                true,
                null,
                Instant.now(),
                null,
                Instant.now()
        );

        when(paymentRecordService.simulateOnlinePayment(any(SimulatePaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/simulate")
                        .with(authentication(createAuth(customerId, "customer1", Role.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.simulated").value(true))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.method").value("SIMULATED_ONLINE"))
                .andExpect(jsonPath("$.amount").value(1999.00));
    }

    @Test
    @DisplayName("POST /api/v1/payments/cod-pending records pending COD payment")
    void recordCodPending_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        RecordCodPendingRequest request = new RecordCodPendingRequest(orderId, customerId, new BigDecimal("1499.00"), "395002");

        PaymentResponse response = new PaymentResponse(
                UUID.randomUUID(),
                orderId,
                customerId,
                new BigDecimal("1499.00"),
                "INR",
                PaymentMethod.COD,
                PaymentStatus.PENDING,
                "COD-20261116-654321",
                false,
                "395002",
                null,
                null,
                Instant.now()
        );

        when(paymentRecordService.recordCodPending(any(RecordCodPendingRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/cod-pending")
                        .with(authentication(createAuth(customerId, "customer1", Role.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.simulated").value(false))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.method").value("COD"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/cod-collected records collected COD payment")
    void recordCodCollected_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        RecordCodCollectedRequest request = new RecordCodCollectedRequest(orderId, new BigDecimal("1499.00"));

        PaymentResponse response = new PaymentResponse(
                UUID.randomUUID(),
                orderId,
                UUID.randomUUID(),
                new BigDecimal("1499.00"),
                "INR",
                PaymentMethod.COD,
                PaymentStatus.SUCCESS,
                "COD-20261116-654321",
                false,
                "395002",
                Instant.now(),
                UUID.randomUUID(),
                Instant.now().minusSeconds(3600)
        );

        when(paymentRecordService.recordCodCollected(any(RecordCodCollectedRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/cod-collected")
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/reverse allowed for OWNER only")
    void reversePayment_Owner_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        ReversePaymentRequest request = new ReversePaymentRequest(orderId, "Defective product returned");

        PaymentResponse response = new PaymentResponse(
                UUID.randomUUID(),
                orderId,
                UUID.randomUUID(),
                new BigDecimal("1499.00"),
                "INR",
                PaymentMethod.SIMULATED_ONLINE,
                PaymentStatus.REVERSED,
                "SIM-12345",
                true,
                null,
                null,
                null,
                Instant.now()
        );

        when(paymentRecordService.reversePayment(any(ReversePaymentRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/reverse")
                        .with(authentication(createAuth(UUID.randomUUID(), "owner1", Role.OWNER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVERSED"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/reverse forbidden for CUSTOMER")
    void reversePayment_Customer_Forbidden() throws Exception {
        ReversePaymentRequest request = new ReversePaymentRequest(UUID.randomUUID(), "Cancellation");

        mockMvc.perform(post("/api/v1/payments/reverse")
                        .with(authentication(createAuth(UUID.randomUUID(), "customer1", Role.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/payments/order/{orderId} returns payment details")
    void getPaymentByOrderId_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse(
                UUID.randomUUID(),
                orderId,
                UUID.randomUUID(),
                new BigDecimal("999.00"),
                "INR",
                PaymentMethod.SIMULATED_ONLINE,
                PaymentStatus.SUCCESS,
                "SIM-111",
                true,
                null,
                Instant.now(),
                null,
                Instant.now()
        );

        when(paymentRecordService.getPaymentByOrderId(orderId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/order/" + orderId)
                        .with(authentication(createAuth(UUID.randomUUID(), "customer1", Role.CUSTOMER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.amount").value(999.00));
    }
}
