package com.dzfashion.payment.controller;

import com.dzfashion.payment.dto.request.CreateCodSettlementBatchRequest;
import com.dzfashion.payment.dto.response.CodSettlementBatchResponse;
import com.dzfashion.payment.enums.CodSettlementStatus;
import com.dzfashion.payment.enums.Role;
import com.dzfashion.payment.security.JwtAuthenticationFilter;
import com.dzfashion.payment.security.JwtTokenService;
import com.dzfashion.payment.security.SecurityConfig;
import com.dzfashion.payment.security.UserPrincipal;
import com.dzfashion.payment.service.CodSettlementService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CodSettlementController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CodSettlementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private CodSettlementService codSettlementService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    private UsernamePasswordAuthenticationToken createAuth(UUID id, String username, Role role) {
        UserPrincipal principal = new UserPrincipal(id, username, username + "@example.com", role);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    @DisplayName("POST /api/v1/admin/cod-settlements allows MANAGER to create settlement batch")
    void createBatch_Manager_Success() throws Exception {
        UUID managerId = UUID.randomUUID();
        CreateCodSettlementBatchRequest request = new CreateCodSettlementBatchRequest(
                "Delhivery Logistics",
                LocalDate.now(),
                new BigDecimal("10000.00"),
                new BigDecimal("10000.00"),
                "Fully reconciled"
        );

        CodSettlementBatchResponse response = new CodSettlementBatchResponse(
                UUID.randomUUID(),
                "COD-20261116-XYZ",
                "Delhivery Logistics",
                LocalDate.now(),
                new BigDecimal("10000.00"),
                new BigDecimal("10000.00"),
                BigDecimal.ZERO,
                CodSettlementStatus.SUBMITTED,
                managerId,
                null,
                "Fully reconciled",
                Instant.now(),
                null
        );

        when(codSettlementService.createBatch(any(CreateCodSettlementBatchRequest.class), eq(managerId)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/cod-settlements")
                        .with(authentication(createAuth(managerId, "manager1", Role.MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.batchReference").value("COD-20261116-XYZ"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/cod-settlements forbidden for CUSTOMER")
    void createBatch_Customer_Forbidden() throws Exception {
        CreateCodSettlementBatchRequest request = new CreateCodSettlementBatchRequest(
                "Agency", LocalDate.now(), BigDecimal.TEN, BigDecimal.TEN, null
        );

        mockMvc.perform(post("/api/v1/admin/cod-settlements")
                        .with(authentication(createAuth(UUID.randomUUID(), "customer1", Role.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/admin/cod-settlements/{batchId}/approve allows OWNER to approve batch")
    void approveBatch_Owner_Success() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();

        CodSettlementBatchResponse response = new CodSettlementBatchResponse(
                batchId,
                "COD-20261116-XYZ",
                "Delhivery",
                LocalDate.now(),
                new BigDecimal("10000.00"),
                new BigDecimal("10000.00"),
                BigDecimal.ZERO,
                CodSettlementStatus.APPROVED,
                UUID.randomUUID(),
                ownerId,
                "Approved",
                Instant.now().minusSeconds(1800),
                Instant.now()
        );

        when(codSettlementService.approveBatch(eq(batchId), eq(ownerId))).thenReturn(response);

        mockMvc.perform(put("/api/v1/admin/cod-settlements/" + batchId + "/approve")
                        .with(authentication(createAuth(ownerId, "owner1", Role.OWNER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedBy").value(ownerId.toString()));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/cod-settlements/{batchId}/approve forbidden for MANAGER")
    void approveBatch_Manager_Forbidden() throws Exception {
        UUID batchId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/admin/cod-settlements/" + batchId + "/approve")
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER))))
                .andExpect(status().isForbidden());
    }
}
