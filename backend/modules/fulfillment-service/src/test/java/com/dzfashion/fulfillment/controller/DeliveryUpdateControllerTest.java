package com.dzfashion.fulfillment.controller;

import com.dzfashion.fulfillment.dto.request.ReceiveReturnAtWarehouseRequest;
import com.dzfashion.fulfillment.dto.request.RecordDeliveryAttemptRequest;
import com.dzfashion.fulfillment.dto.request.RecordDeliverySuccessRequest;
import com.dzfashion.fulfillment.dto.response.ShipmentResponse;
import com.dzfashion.fulfillment.enums.DeliveryAttemptResult;
import com.dzfashion.fulfillment.enums.Role;
import com.dzfashion.fulfillment.enums.ShipmentStatus;
import com.dzfashion.fulfillment.security.JwtAuthenticationFilter;
import com.dzfashion.fulfillment.security.JwtTokenService;
import com.dzfashion.fulfillment.security.SecurityConfig;
import com.dzfashion.fulfillment.security.UserPrincipal;
import com.dzfashion.fulfillment.service.ShipmentDeliveryService;
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

@WebMvcTest(DeliveryUpdateController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class DeliveryUpdateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private ShipmentDeliveryService shipmentDeliveryService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    private UsernamePasswordAuthenticationToken createAuth(UUID id, String username, Role role) {
        UserPrincipal principal = new UserPrincipal(id, username, username + "@example.com", role);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private ShipmentResponse createSampleResponse(UUID shipmentId, ShipmentStatus status, int attempts) {
        return new ShipmentResponse(
                shipmentId, UUID.randomUUID(), "ORD-001", UUID.randomUUID(), "Customer A", "9876543210",
                "Address", "Surat", "Gujarat", "395002", null, null, null,
                status, null, null, attempts, true, new BigDecimal("1000.00"), new BigDecimal("1000.00"),
                null, null, null, null, null, null, null, null, null, null, List.of(), List.of()
        );
    }

    @Test
    @DisplayName("POST /api/v1/fulfillment/shipments/{id}/attempts records delivery attempt")
    void testRecordAttempt_ManagerAllowed() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        RecordDeliveryAttemptRequest request = new RecordDeliveryAttemptRequest(
                Instant.now(), DeliveryAttemptResult.CUSTOMER_UNAVAILABLE, "First attempt failed"
        );

        when(shipmentDeliveryService.recordDeliveryAttempt(eq(shipmentId), any(), any()))
                .thenReturn(createSampleResponse(shipmentId, ShipmentStatus.DELIVERY_ATTEMPTED_1, 1));

        mockMvc.perform(post("/api/v1/fulfillment/shipments/" + shipmentId + "/attempts")
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERY_ATTEMPTED_1"))
                .andExpect(jsonPath("$.deliveryAttempts").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/fulfillment/shipments/{id}/delivery-success records delivery success")
    void testRecordDeliverySuccess_ManagerAllowed() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        RecordDeliverySuccessRequest request = new RecordDeliverySuccessRequest(
                LocalDate.of(2026, 11, 16), new BigDecimal("1000.00"), "Delivered to customer"
        );

        when(shipmentDeliveryService.recordDeliverySuccess(eq(shipmentId), any(), any()))
                .thenReturn(createSampleResponse(shipmentId, ShipmentStatus.DELIVERED, 1));

        mockMvc.perform(post("/api/v1/fulfillment/shipments/" + shipmentId + "/delivery-success")
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    @DisplayName("POST /api/v1/fulfillment/shipments/{id}/warehouse-return records return intake at Surat warehouse")
    void testReceiveReturnAtWarehouse_ManagerAllowed() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        ReceiveReturnAtWarehouseRequest request = new ReceiveReturnAtWarehouseRequest("Parcel returned intact");

        when(shipmentDeliveryService.receiveReturnAtWarehouse(eq(shipmentId), any(), any()))
                .thenReturn(createSampleResponse(shipmentId, ShipmentStatus.RETURNED_TO_WAREHOUSE, 2));

        mockMvc.perform(post("/api/v1/fulfillment/shipments/" + shipmentId + "/warehouse-return")
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED_TO_WAREHOUSE"));
    }

    @Test
    @DisplayName("POST /api/v1/fulfillment/shipments/{id}/attempts denies CUSTOMER (403)")
    void testRecordAttempt_CustomerDenied() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        RecordDeliveryAttemptRequest request = new RecordDeliveryAttemptRequest(
                Instant.now(), DeliveryAttemptResult.CUSTOMER_UNAVAILABLE, "First attempt failed"
        );

        mockMvc.perform(post("/api/v1/fulfillment/shipments/" + shipmentId + "/attempts")
                        .with(authentication(createAuth(UUID.randomUUID(), "customer1", Role.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
