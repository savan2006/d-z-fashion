package com.dzfashion.fulfillment.controller;

import com.dzfashion.fulfillment.dto.request.CreateReturnRequest;
import com.dzfashion.fulfillment.dto.response.ReturnEligibilityResponse;
import com.dzfashion.fulfillment.dto.response.ReturnRequestResponse;
import com.dzfashion.fulfillment.enums.ReturnReason;
import com.dzfashion.fulfillment.enums.ReturnRequestStatus;
import com.dzfashion.fulfillment.enums.ReturnResolutionType;
import com.dzfashion.fulfillment.enums.Role;
import com.dzfashion.fulfillment.security.JwtAuthenticationFilter;
import com.dzfashion.fulfillment.security.JwtTokenService;
import com.dzfashion.fulfillment.security.SecurityConfig;
import com.dzfashion.fulfillment.security.UserPrincipal;
import com.dzfashion.fulfillment.service.ReturnEligibilityService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerReturnController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CustomerReturnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private ReturnEligibilityService returnEligibilityService;

    @MockitoBean
    private ReturnWorkflowService returnWorkflowService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    private UsernamePasswordAuthenticationToken createAuth(UUID id, String username, Role role) {
        UserPrincipal principal = new UserPrincipal(id, username, username + "@example.com", role);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    @DisplayName("GET /api/v1/shipments/returns/eligibility/{orderId} allows CUSTOMER to check return eligibility")
    void testCheckEligibility_CustomerAllowed() throws Exception {
        UUID orderId = UUID.randomUUID();
        ReturnEligibilityResponse response = new ReturnEligibilityResponse(
                orderId, "ORD-001", true, Instant.now().plusSeconds(86400),
                LocalDate.now(), null, List.of()
        );

        when(returnEligibilityService.checkEligibility(orderId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/shipments/returns/eligibility/" + orderId)
                        .with(authentication(createAuth(UUID.randomUUID(), "customer1", Role.CUSTOMER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.orderNumber").value("ORD-001"));
    }

    @Test
    @DisplayName("POST /api/v1/shipments/returns allows CUSTOMER to create return request")
    void testCreateReturn_CustomerAllowed() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();

        CreateReturnRequest request = new CreateReturnRequest(
                orderId, "ORD-001", orderItemId, "KURTI-01", "Kurti", 1,
                new BigDecimal("1200.00"), ReturnReason.DAMAGED, ReturnResolutionType.EXCHANGE,
                List.of("https://res.cloudinary.com/dz/image/upload/v1/defect.jpg"),
                "Torn seam", null, null, null, null, null
        );

        ReturnRequestResponse response = new ReturnRequestResponse(
                UUID.randomUUID(), "RET-20261116-0001", orderId, "ORD-001", orderItemId, customerId,
                "KURTI-01", "Kurti", 1, new BigDecimal("1200.00"),
                ReturnReason.DAMAGED, ReturnResolutionType.EXCHANGE, ReturnRequestStatus.REQUESTED,
                List.of("https://res.cloudinary.com/dz/image/upload/v1/defect.jpg"), "Torn seam",
                null, null, null, null, null, null, null, null, null, null, null,
                BigDecimal.ZERO, true, null, null, null, null, null, null, false,
                null, Instant.now(), Instant.now()
        );

        when(returnWorkflowService.createReturnRequest(any(), eq(customerId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/shipments/returns")
                        .with(authentication(createAuth(customerId, "customer1", Role.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.returnNumber").value("RET-20261116-0001"))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    @DisplayName("POST /api/v1/shipments/returns denies unauthenticated request (401)")
    void testCreateReturn_UnauthenticatedReturns401() throws Exception {
        CreateReturnRequest request = new CreateReturnRequest(
                UUID.randomUUID(), "ORD-001", UUID.randomUUID(), "KURTI-01", "Kurti", 1,
                new BigDecimal("1200.00"), ReturnReason.DAMAGED, ReturnResolutionType.EXCHANGE,
                List.of("https://res.cloudinary.com/dz/image/upload/v1/defect.jpg"),
                "Torn seam", null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/shipments/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
