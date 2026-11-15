package com.dzfashion.inventory.controller;

import com.dzfashion.inventory.dto.request.CommitReservationRequest;
import com.dzfashion.inventory.dto.request.ReleaseStockRequest;
import com.dzfashion.inventory.dto.request.ReserveStockRequest;
import com.dzfashion.inventory.dto.response.ActionResponse;
import com.dzfashion.inventory.dto.response.ReservationResponse;
import com.dzfashion.inventory.enums.ReservationStatus;
import com.dzfashion.inventory.enums.Role;
import com.dzfashion.inventory.security.JwtAuthenticationFilter;
import com.dzfashion.inventory.security.JwtTokenService;
import com.dzfashion.inventory.security.SecurityConfig;
import com.dzfashion.inventory.security.UserPrincipal;
import com.dzfashion.inventory.service.ReservationService;
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

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("Unauthenticated request to reserve stock returns 401 Unauthorized")
    void reserveStockUnauthenticated() throws Exception {
        ReserveStockRequest request = new ReserveStockRequest(UUID.randomUUID(), UUID.randomUUID(), 2, "IDEM-100");

        mockMvc.perform(post("/api/v1/inventory/reservations/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authenticated checkout can reserve stock (201 Created)")
    void reserveStockAuthenticated() throws Exception {
        UUID customerId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(customerId, "customer1", "cust@dzfashion.com", Role.CUSTOMER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        UUID orderId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        ReserveStockRequest request = new ReserveStockRequest(orderId, variantId, 2, "IDEM-101");

        UUID reservationId = UUID.randomUUID();
        when(reservationService.reserveStock(any(), eq(customerId))).thenReturn(
                new ReservationResponse(reservationId, orderId, variantId, 2, ReservationStatus.RESERVED, "IDEM-101", Instant.now())
        );

        mockMvc.perform(post("/api/v1/inventory/reservations/reserve")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.idempotencyKey").value("IDEM-101"));
    }

    @Test
    @DisplayName("Authenticated checkout can release stock on cancellation (200 OK)")
    void releaseStockAuthenticated() throws Exception {
        UUID customerId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(customerId, "customer1", "cust@dzfashion.com", Role.CUSTOMER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        UUID orderId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        ReleaseStockRequest request = new ReleaseStockRequest(orderId, variantId, "IDEM-102");

        when(reservationService.releaseStock(any(), eq(customerId))).thenReturn(
                new ActionResponse("Stock reservation successfully released")
        );

        mockMvc.perform(post("/api/v1/inventory/reservations/release")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Stock reservation successfully released"));
    }

    @Test
    @DisplayName("Manager can commit reservation upon order dispatch (200 OK)")
    void commitReservationManager() throws Exception {
        UUID managerId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(managerId, "manager1", "mgr@dzfashion.com", Role.MANAGER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        CommitReservationRequest request = new CommitReservationRequest(UUID.randomUUID(), UUID.randomUUID());

        when(reservationService.commitReservation(any(), eq(managerId))).thenReturn(
                new ActionResponse("Reservation successfully committed to dispatch")
        );

        mockMvc.perform(post("/api/v1/inventory/reservations/commit")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reservation successfully committed to dispatch"));
    }
}
