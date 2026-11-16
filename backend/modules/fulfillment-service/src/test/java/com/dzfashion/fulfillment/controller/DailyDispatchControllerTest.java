package com.dzfashion.fulfillment.controller;

import com.dzfashion.fulfillment.dto.request.CreateDispatchBatchRequest;
import com.dzfashion.fulfillment.dto.request.RecordHandoverRequest;
import com.dzfashion.fulfillment.dto.response.DailyDispatchBatchResponse;
import com.dzfashion.fulfillment.enums.DailyDispatchStatus;
import com.dzfashion.fulfillment.enums.Role;
import com.dzfashion.fulfillment.security.JwtAuthenticationFilter;
import com.dzfashion.fulfillment.security.JwtTokenService;
import com.dzfashion.fulfillment.security.SecurityConfig;
import com.dzfashion.fulfillment.security.UserPrincipal;
import com.dzfashion.fulfillment.service.DailyDispatchService;
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

@WebMvcTest(DailyDispatchController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class DailyDispatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private DailyDispatchService dailyDispatchService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    private UsernamePasswordAuthenticationToken createAuth(UUID id, String username, Role role) {
        UserPrincipal principal = new UserPrincipal(id, username, username + "@example.com", role);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    @DisplayName("POST /api/v1/fulfillment/dispatch/batches allows MANAGER to create batch")
    void testCreateBatch_ManagerAllowed() throws Exception {
        UUID agencyId = UUID.randomUUID();
        CreateDispatchBatchRequest request = new CreateDispatchBatchRequest(
                LocalDate.of(2026, 11, 16),
                agencyId,
                "BILL-123",
                "Morning dispatch",
                List.of(UUID.randomUUID())
        );

        DailyDispatchBatchResponse response = new DailyDispatchBatchResponse(
                UUID.randomUUID(),
                "DISPATCH-20261116-0001",
                LocalDate.of(2026, 11, 16),
                agencyId,
                "Delhivery",
                UUID.randomUUID(),
                1,
                "BILL-123",
                DailyDispatchStatus.PREPARED,
                null,
                "Morning dispatch",
                Instant.now()
        );

        when(dailyDispatchService.createDailyDispatchBatch(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/fulfillment/dispatch/batches")
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.batchNumber").value("DISPATCH-20261116-0001"))
                .andExpect(jsonPath("$.status").value("PREPARED"));
    }

    @Test
    @DisplayName("POST /api/v1/fulfillment/dispatch/batches/{id}/handover records agency handover")
    void testRecordHandover_ManagerAllowed() throws Exception {
        UUID batchId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();
        RecordHandoverRequest request = new RecordHandoverRequest(
                agencyId, "WAYBILL-999", Instant.now(), "Driver Ramesh arrived"
        );

        DailyDispatchBatchResponse response = new DailyDispatchBatchResponse(
                batchId,
                "DISPATCH-20261116-0001",
                LocalDate.of(2026, 11, 16),
                agencyId,
                "Delhivery",
                UUID.randomUUID(),
                1,
                "WAYBILL-999",
                DailyDispatchStatus.HANDED_OVER,
                Instant.now(),
                "Morning dispatch",
                Instant.now()
        );

        when(dailyDispatchService.recordAgencyHandover(eq(batchId), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/fulfillment/dispatch/batches/" + batchId + "/handover")
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HANDED_OVER"));
    }

    @Test
    @DisplayName("POST /api/v1/fulfillment/dispatch/batches denies CUSTOMER access (403)")
    void testCreateBatch_CustomerDenied() throws Exception {
        CreateDispatchBatchRequest request = new CreateDispatchBatchRequest(
                LocalDate.of(2026, 11, 16),
                UUID.randomUUID(),
                "BILL-123",
                "Notes",
                List.of(UUID.randomUUID())
        );

        mockMvc.perform(post("/api/v1/fulfillment/dispatch/batches")
                        .with(authentication(createAuth(UUID.randomUUID(), "customer1", Role.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
