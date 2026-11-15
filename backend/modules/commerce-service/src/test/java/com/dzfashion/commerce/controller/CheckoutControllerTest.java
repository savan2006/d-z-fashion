package com.dzfashion.commerce.controller;

import com.dzfashion.commerce.dto.request.CheckoutPreviewRequest;
import com.dzfashion.commerce.dto.request.ConfirmOrderRequest;
import com.dzfashion.commerce.dto.response.CheckoutPreviewResponse;
import com.dzfashion.commerce.dto.response.OrderResponse;
import com.dzfashion.commerce.enums.OrderStatus;
import com.dzfashion.commerce.enums.PaymentMethod;
import com.dzfashion.commerce.enums.PaymentStatus;
import com.dzfashion.commerce.enums.Role;
import com.dzfashion.commerce.security.JwtAuthenticationFilter;
import com.dzfashion.commerce.security.JwtTokenService;
import com.dzfashion.commerce.security.SecurityConfig;
import com.dzfashion.commerce.security.UserPrincipal;
import com.dzfashion.commerce.service.CheckoutOrchestrationService;
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
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CheckoutController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CheckoutOrchestrationService orchestrationService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    private UsernamePasswordAuthenticationToken createAuth(UUID id, String username, Role role) {
        UserPrincipal principal = new UserPrincipal(id, username, username + "@example.com", role);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    @DisplayName("Unauthenticated checkout preview returns 401 Unauthorized")
    void previewCheckoutUnauthenticated() throws Exception {
        CheckoutPreviewRequest request = new CheckoutPreviewRequest(PaymentMethod.SIMULATED_ONLINE, null, null, "395001");

        mockMvc.perform(post("/api/v1/checkout/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Customer preview checkout returns authoritative breakdown")
    void customerPreviewCheckoutSuccess() throws Exception {
        UUID customerId = UUID.randomUUID();
        CheckoutPreviewRequest request = new CheckoutPreviewRequest(PaymentMethod.SIMULATED_ONLINE, "WELCOME10", null, "395001");

        CheckoutPreviewResponse preview = new CheckoutPreviewResponse(
                new BigDecimal("2500.00"), 2, true, BigDecimal.ZERO, new BigDecimal("250.00"), "WELCOME10",
                new BigDecimal("112.50"), new BigDecimal("5.00"), BigDecimal.ZERO, new BigDecimal("2362.50"),
                true, false, Collections.emptyList()
        );
        when(orchestrationService.previewCheckout(eq(customerId), any())).thenReturn(preview);

        mockMvc.perform(post("/api/v1/checkout/preview")
                        .with(authentication(createAuth(customerId, "customer1", Role.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalBeforeDiscountAndTax").value(2500.00))
                .andExpect(jsonPath("$.promotionalDiscountAmount").value(250.00))
                .andExpect(jsonPath("$.freeDeliveryEligible").value(true))
                .andExpect(jsonPath("$.finalPayableAmount").value(2362.50));
    }

    @Test
    @DisplayName("Customer confirm order creates order with status 201 Created")
    void customerConfirmOrderSuccess() throws Exception {
        UUID customerId = UUID.randomUUID();
        ConfirmOrderRequest request = new ConfirmOrderRequest(
                PaymentMethod.SIMULATED_ONLINE, null, null, false,
                "Aditi", "9876543210", "Flat 401", "Ring Rd", "Surat", "Gujarat", "395001", "India"
        );

        OrderResponse orderResponse = new OrderResponse(
                UUID.randomUUID(), "DZF-20261115-TEST1", "idemp-test", customerId,
                OrderStatus.CONFIRMED, PaymentMethod.SIMULATED_ONLINE, PaymentStatus.PAID,
                null, null, new BigDecimal("2500.00"), BigDecimal.ZERO, null,
                new BigDecimal("125.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("2625.00"),
                "Aditi", "9876543210", "Flat 401", "Ring Rd", "Surat", "Gujarat", "395001", "India",
                Instant.now(), null, null, null, null, null, Collections.emptyList()
        );
        when(orchestrationService.confirmOrder(eq(customerId), any(), eq("idemp-test"))).thenReturn(orderResponse);

        mockMvc.perform(post("/api/v1/checkout/confirm")
                        .with(authentication(createAuth(customerId, "customer1", Role.CUSTOMER)))
                        .header("Idempotency-Key", "idemp-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("DZF-20261115-TEST1"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
    }
}
