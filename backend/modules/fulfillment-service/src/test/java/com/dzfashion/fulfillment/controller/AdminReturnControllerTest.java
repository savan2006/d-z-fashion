package com.dzfashion.fulfillment.controller;

import com.dzfashion.fulfillment.dto.request.PerformInspectionRequest;
import com.dzfashion.fulfillment.dto.request.SchedulePickupRequest;
import com.dzfashion.fulfillment.dto.response.ReturnInspectionResponse;
import com.dzfashion.fulfillment.dto.response.ReturnRequestResponse;
import com.dzfashion.fulfillment.enums.ItemDisposition;
import com.dzfashion.fulfillment.enums.ReturnInspectionResult;
import com.dzfashion.fulfillment.enums.ReturnReason;
import com.dzfashion.fulfillment.enums.ReturnRequestStatus;
import com.dzfashion.fulfillment.enums.ReturnResolutionType;
import com.dzfashion.fulfillment.enums.Role;
import com.dzfashion.fulfillment.security.JwtAuthenticationFilter;
import com.dzfashion.fulfillment.security.JwtTokenService;
import com.dzfashion.fulfillment.security.SecurityConfig;
import com.dzfashion.fulfillment.security.UserPrincipal;
import com.dzfashion.fulfillment.service.ReturnWorkflowService;
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

@WebMvcTest(AdminReturnController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminReturnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private ReturnWorkflowService returnWorkflowService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    private UsernamePasswordAuthenticationToken createAuth(UUID id, String username, Role role) {
        UserPrincipal principal = new UserPrincipal(id, username, username + "@example.com", role);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private ReturnRequestResponse createSampleResponse(UUID returnId, ReturnRequestStatus status) {
        return new ReturnRequestResponse(
                returnId, "RET-20261116-0001", UUID.randomUUID(), "ORD-001", UUID.randomUUID(), UUID.randomUUID(),
                "KURTI-01", "Kurti", 1, new BigDecimal("1200.00"),
                ReturnReason.DAMAGED, ReturnResolutionType.EXCHANGE, status,
                List.of("https://res.cloudinary.com/dz/image/upload/v1/defect.jpg"), "Torn seam",
                null, "Delhivery", "PICKUP-101", LocalDate.now(), null, null, null, null, null, null, null,
                BigDecimal.ZERO, true, null, null, null, null, null, null, false,
                null, Instant.now(), Instant.now()
        );
    }

    @Test
    @DisplayName("POST /api/v1/fulfillment/returns/{id}/schedule-pickup allows MANAGER to schedule pickup")
    void testSchedulePickup_ManagerAllowed() throws Exception {
        UUID returnId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();
        SchedulePickupRequest request = new SchedulePickupRequest(agencyId, LocalDate.now().plusDays(1), "WAYBILL-101", "Notes");

        when(returnWorkflowService.schedulePickup(eq(returnId), any(), any()))
                .thenReturn(createSampleResponse(returnId, ReturnRequestStatus.PICKUP_SCHEDULED));

        mockMvc.perform(post("/api/v1/fulfillment/returns/" + returnId + "/schedule-pickup")
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKUP_SCHEDULED"));
    }

    @Test
    @DisplayName("POST /api/v1/fulfillment/returns/{id}/inspect allows MANAGER to record Surat inspection")
    void testPerformInspection_ManagerAllowed() throws Exception {
        UUID returnId = UUID.randomUUID();
        PerformInspectionRequest request = new PerformInspectionRequest(
                ReturnInspectionResult.PASSED, ItemDisposition.DAMAGED_QUARANTINE, "Defect verified", List.of()
        );

        when(returnWorkflowService.performInspection(eq(returnId), any(), any()))
                .thenReturn(createSampleResponse(returnId, ReturnRequestStatus.EXCHANGE_PREPARED));

        mockMvc.perform(post("/api/v1/fulfillment/returns/" + returnId + "/inspect")
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXCHANGE_PREPARED"));
    }

    @Test
    @DisplayName("POST /api/v1/fulfillment/returns/{id}/approve-refund allows OWNER to approve refund")
    void testApproveRefund_OwnerAllowed() throws Exception {
        UUID returnId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        when(returnWorkflowService.approveRefund(eq(returnId), eq(ownerId)))
                .thenReturn(createSampleResponse(returnId, ReturnRequestStatus.REFUND_APPROVED));

        mockMvc.perform(post("/api/v1/fulfillment/returns/" + returnId + "/approve-refund")
                        .with(authentication(createAuth(ownerId, "owner1", Role.OWNER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUND_APPROVED"));
    }

    @Test
    @DisplayName("POST /api/v1/fulfillment/returns/{id}/approve-refund strictly FORBIDS MANAGER (403 Owner-Only Gate)")
    void testApproveRefund_ManagerForbidden() throws Exception {
        UUID returnId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/fulfillment/returns/" + returnId + "/approve-refund")
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/fulfillment/returns/{id}/approve-refund strictly FORBIDS CUSTOMER (403)")
    void testApproveRefund_CustomerForbidden() throws Exception {
        UUID returnId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/fulfillment/returns/" + returnId + "/approve-refund")
                        .with(authentication(createAuth(UUID.randomUUID(), "customer1", Role.CUSTOMER))))
                .andExpect(status().isForbidden());
    }
}
