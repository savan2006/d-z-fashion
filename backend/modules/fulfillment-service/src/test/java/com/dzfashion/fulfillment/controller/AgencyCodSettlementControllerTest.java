package com.dzfashion.fulfillment.controller;

import com.dzfashion.fulfillment.dto.request.CreateAgencyCodSettlementRequest;
import com.dzfashion.fulfillment.dto.response.AgencyCodSettlementResponse;
import com.dzfashion.fulfillment.enums.AgencyCodSettlementStatus;
import com.dzfashion.fulfillment.enums.Role;
import com.dzfashion.fulfillment.security.JwtAuthenticationFilter;
import com.dzfashion.fulfillment.security.JwtTokenService;
import com.dzfashion.fulfillment.security.SecurityConfig;
import com.dzfashion.fulfillment.security.UserPrincipal;
import com.dzfashion.fulfillment.service.AgencyCodSettlementService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgencyCodSettlementController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AgencyCodSettlementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private AgencyCodSettlementService agencyCodSettlementService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    private UsernamePasswordAuthenticationToken createAuth(UUID id, String username, Role role) {
        UserPrincipal principal = new UserPrincipal(id, username, username + "@example.com", role);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    @DisplayName("POST /api/v1/fulfillment/cod-settlements/prepare allows MANAGER to prepare reconciliation batch")
    void testPrepareSettlement_ManagerAllowed() throws Exception {
        UUID agencyId = UUID.randomUUID();
        CreateAgencyCodSettlementRequest request = new CreateAgencyCodSettlementRequest(
                agencyId, LocalDate.of(2026, 11, 16), new BigDecimal("5000.00"),
                "NEFT-12345", "Weekly settlement", List.of(UUID.randomUUID())
        );

        AgencyCodSettlementResponse response = new AgencyCodSettlementResponse(
                UUID.randomUUID(), "COD-SETTLE-20261116-0001", agencyId, "Delhivery",
                LocalDate.of(2026, 11, 16), 1, new BigDecimal("5000.00"), new BigDecimal("5000.00"),
                BigDecimal.ZERO, AgencyCodSettlementStatus.PREPARED, UUID.randomUUID(), null,
                "NEFT-12345", "Weekly settlement", Instant.now(), null
        );

        when(agencyCodSettlementService.prepareSettlement(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/fulfillment/cod-settlements/prepare")
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.batchReference").value("COD-SETTLE-20261116-0001"))
                .andExpect(jsonPath("$.status").value("PREPARED"));
    }

    @Test
    @DisplayName("POST /api/v1/fulfillment/cod-settlements/{id}/approve allows OWNER to approve batch")
    void testApproveSettlement_OwnerAllowed() throws Exception {
        UUID settlementId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        AgencyCodSettlementResponse response = new AgencyCodSettlementResponse(
                settlementId, "COD-SETTLE-20261116-0001", UUID.randomUUID(), "Delhivery",
                LocalDate.of(2026, 11, 16), 1, new BigDecimal("5000.00"), new BigDecimal("5000.00"),
                BigDecimal.ZERO, AgencyCodSettlementStatus.APPROVED, UUID.randomUUID(), ownerId,
                "NEFT-12345", "Weekly settlement", Instant.now(), Instant.now()
        );

        when(agencyCodSettlementService.approveSettlement(eq(settlementId), eq(ownerId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/fulfillment/cod-settlements/" + settlementId + "/approve")
                        .with(authentication(createAuth(ownerId, "owner1", Role.OWNER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("POST /api/v1/fulfillment/cod-settlements/{id}/approve strictly FORBIDS MANAGER (403 Owner-Only Gate)")
    void testApproveSettlement_ManagerForbidden() throws Exception {
        UUID settlementId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/fulfillment/cod-settlements/" + settlementId + "/approve")
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/fulfillment/cod-settlements/{id}/approve strictly FORBIDS CUSTOMER (403)")
    void testApproveSettlement_CustomerForbidden() throws Exception {
        UUID settlementId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/fulfillment/cod-settlements/" + settlementId + "/approve")
                        .with(authentication(createAuth(UUID.randomUUID(), "customer1", Role.CUSTOMER))))
                .andExpect(status().isForbidden());
    }
}
