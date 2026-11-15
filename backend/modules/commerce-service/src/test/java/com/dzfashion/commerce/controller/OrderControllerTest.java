package com.dzfashion.commerce.controller;

import com.dzfashion.commerce.dto.request.CancelOrderRequest;
import com.dzfashion.commerce.dto.response.OrderResponse;
import com.dzfashion.commerce.dto.response.OrderSummaryResponse;
import com.dzfashion.commerce.dto.response.PageResponse;
import com.dzfashion.commerce.enums.OrderStatus;
import com.dzfashion.commerce.enums.PaymentMethod;
import com.dzfashion.commerce.enums.PaymentStatus;
import com.dzfashion.commerce.enums.Role;
import com.dzfashion.commerce.security.JwtAuthenticationFilter;
import com.dzfashion.commerce.security.JwtTokenService;
import com.dzfashion.commerce.security.SecurityConfig;
import com.dzfashion.commerce.security.UserPrincipal;
import com.dzfashion.commerce.service.OrderLifecycleService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private OrderLifecycleService lifecycleService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    private UsernamePasswordAuthenticationToken createAuth(UUID id, String username, Role role) {
        UserPrincipal principal = new UserPrincipal(id, username, username + "@example.com", role);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    @DisplayName("Customer can list their orders")
    void customerGetMyOrders() throws Exception {
        UUID customerId = UUID.randomUUID();
        OrderSummaryResponse summary = new OrderSummaryResponse(
                UUID.randomUUID(), "DZF-20261115-001", customerId, OrderStatus.CONFIRMED,
                PaymentMethod.SIMULATED_ONLINE, PaymentStatus.PAID, null,
                new BigDecimal("2100.00"), 2, Instant.now()
        );
        PageResponse<OrderSummaryResponse> page = new PageResponse<>(List.of(summary), 0, 10, 1, 1, true, true);
        when(lifecycleService.getCustomerOrders(eq(customerId), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/orders")
                        .with(authentication(createAuth(customerId, "customer1", Role.CUSTOMER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").value("DZF-20261115-001"));
    }

    @Test
    @DisplayName("Customer can cancel order pre-dispatch")
    void customerCancelOrderSuccess() throws Exception {
        UUID customerId = UUID.randomUUID();
        String orderNumber = "DZF-20261115-002";
        CancelOrderRequest request = new CancelOrderRequest("Customer requested cancellation");

        OrderResponse cancelledOrder = new OrderResponse(
                UUID.randomUUID(), orderNumber, null, customerId,
                OrderStatus.CANCELLED, PaymentMethod.SIMULATED_ONLINE, PaymentStatus.REFUNDED,
                null, null, new BigDecimal("2000.00"), BigDecimal.ZERO, null,
                new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("2100.00"),
                "Aditi", "9876543210", "Line 1", null, "Surat", "Gujarat", "395001", "India",
                Instant.now(), null, null, Instant.now(), customerId, "Customer requested cancellation",
                Collections.emptyList()
        );
        when(lifecycleService.cancelOrder(eq(customerId), eq(Role.CUSTOMER), eq(orderNumber), any())).thenReturn(cancelledOrder);

        mockMvc.perform(post("/api/v1/orders/" + orderNumber + "/cancel")
                        .with(authentication(createAuth(customerId, "customer1", Role.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.paymentStatus").value("REFUNDED"));
    }
}
