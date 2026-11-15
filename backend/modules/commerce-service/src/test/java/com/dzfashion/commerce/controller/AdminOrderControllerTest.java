package com.dzfashion.commerce.controller;

import com.dzfashion.commerce.dto.request.ReassignOrderRequest;
import com.dzfashion.commerce.dto.request.ReportShortageRequest;
import com.dzfashion.commerce.dto.request.ResolveShortageRequest;
import com.dzfashion.commerce.dto.response.OrderResponse;
import com.dzfashion.commerce.dto.response.OrderSummaryResponse;
import com.dzfashion.commerce.dto.response.PageResponse;
import com.dzfashion.commerce.dto.response.ShortageIncidentResponse;
import com.dzfashion.commerce.enums.OrderStatus;
import com.dzfashion.commerce.enums.PaymentMethod;
import com.dzfashion.commerce.enums.PaymentStatus;
import com.dzfashion.commerce.enums.Role;
import com.dzfashion.commerce.enums.ShortageIncidentStatus;
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

@WebMvcTest(AdminOrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminOrderControllerTest {

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
    @DisplayName("Manager can view the shared order queue")
    void managerCanViewOrderQueue() throws Exception {
        UUID managerId = UUID.randomUUID();
        OrderSummaryResponse summary = new OrderSummaryResponse(
                UUID.randomUUID(), "DZF-20261115-QUEUE1", UUID.randomUUID(), OrderStatus.CONFIRMED,
                PaymentMethod.SIMULATED_ONLINE, PaymentStatus.PAID, null,
                new BigDecimal("2100.00"), 1, Instant.now()
        );
        when(lifecycleService.getManagerOrderQueue(any())).thenReturn(new PageResponse<>(List.of(summary), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/admin/orders/queue")
                        .with(authentication(createAuth(managerId, "manager1", Role.MANAGER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").value("DZF-20261115-QUEUE1"));
    }

    @Test
    @DisplayName("Manager can claim an order")
    void managerCanClaimOrder() throws Exception {
        UUID managerId = UUID.randomUUID();
        String orderNumber = "DZF-20261115-QUEUE1";

        OrderResponse claimedResponse = new OrderResponse(
                UUID.randomUUID(), orderNumber, null, UUID.randomUUID(),
                OrderStatus.PROCESSING, PaymentMethod.SIMULATED_ONLINE, PaymentStatus.PAID,
                managerId, Instant.now(), new BigDecimal("2000.00"), BigDecimal.ZERO, null,
                new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("2100.00"),
                "Aditi", "9876543210", "Line 1", null, "Surat", "Gujarat", "395001", "India",
                Instant.now(), null, null, null, null, null, Collections.emptyList()
        );
        when(lifecycleService.claimOrder(eq(managerId), eq(orderNumber))).thenReturn(claimedResponse);

        mockMvc.perform(post("/api/v1/admin/orders/" + orderNumber + "/claim")
                        .with(authentication(createAuth(managerId, "manager1", Role.MANAGER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimedBy").value(managerId.toString()))
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    @DisplayName("Manager cannot reassign order; only Owner is permitted")
    void managerCannotReassignOrder() throws Exception {
        UUID managerId = UUID.randomUUID();
        ReassignOrderRequest request = new ReassignOrderRequest(UUID.randomUUID(), "Reassign");

        mockMvc.perform(post("/api/v1/admin/orders/DZF-001/reassign")
                        .with(authentication(createAuth(managerId, "manager1", Role.MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Owner can reassign order")
    void ownerCanReassignOrder() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID newManagerId = UUID.randomUUID();
        ReassignOrderRequest request = new ReassignOrderRequest(newManagerId, "Workload rebalance");

        OrderResponse reassignedResponse = new OrderResponse(
                UUID.randomUUID(), "DZF-001", null, UUID.randomUUID(),
                OrderStatus.PROCESSING, PaymentMethod.SIMULATED_ONLINE, PaymentStatus.PAID,
                newManagerId, Instant.now(), new BigDecimal("2000.00"), BigDecimal.ZERO, null,
                new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("2100.00"),
                "Aditi", "9876543210", "Line 1", null, "Surat", "Gujarat", "395001", "India",
                Instant.now(), null, null, null, null, null, Collections.emptyList()
        );
        when(lifecycleService.reassignOrder(eq(ownerId), eq("DZF-001"), any())).thenReturn(reassignedResponse);

        mockMvc.perform(post("/api/v1/admin/orders/DZF-001/reassign")
                        .with(authentication(createAuth(ownerId, "owner1", Role.OWNER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimedBy").value(newManagerId.toString()));
    }

    @Test
    @DisplayName("Manager can report shortage incident")
    void managerCanReportShortage() throws Exception {
        UUID managerId = UUID.randomUUID();
        ReportShortageRequest request = new ReportShortageRequest(UUID.randomUUID(), 1, 0, "Item damaged in bin");

        ShortageIncidentResponse response = new ShortageIncidentResponse(
                UUID.randomUUID(), "SHT-20261115-001", UUID.randomUUID(), "DZF-001",
                request.variantId(), managerId, 1, 0, "Item damaged in bin",
                ShortageIncidentStatus.REPORTED, null, null, null, Instant.now()
        );
        when(lifecycleService.reportShortageIncident(eq(managerId), eq("DZF-001"), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/orders/DZF-001/shortage")
                        .with(authentication(createAuth(managerId, "manager1", Role.MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentNumber").value("SHT-20261115-001"))
                .andExpect(jsonPath("$.status").value("REPORTED"));
    }
}
