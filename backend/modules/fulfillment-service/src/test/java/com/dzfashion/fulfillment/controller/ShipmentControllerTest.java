package com.dzfashion.fulfillment.controller;

import com.dzfashion.fulfillment.dto.request.CreateShipmentItemRequest;
import com.dzfashion.fulfillment.dto.request.CreateShipmentRequest;
import com.dzfashion.fulfillment.dto.response.CustomerTrackingResponse;
import com.dzfashion.fulfillment.dto.response.ShipmentResponse;
import com.dzfashion.fulfillment.enums.CustomerShipmentStatus;
import com.dzfashion.fulfillment.enums.Role;
import com.dzfashion.fulfillment.enums.ShipmentStatus;
import com.dzfashion.fulfillment.security.JwtAuthenticationFilter;
import com.dzfashion.fulfillment.security.JwtTokenService;
import com.dzfashion.fulfillment.security.SecurityConfig;
import com.dzfashion.fulfillment.security.UserPrincipal;
import com.dzfashion.fulfillment.service.ShipmentService;
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

@WebMvcTest(ShipmentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ShipmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private ShipmentService shipmentService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    private UsernamePasswordAuthenticationToken createAuth(UUID id, String username, Role role) {
        UserPrincipal principal = new UserPrincipal(id, username, username + "@example.com", role);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    @DisplayName("POST /api/v1/fulfillment/shipments allows MANAGER to create shipment")
    void testCreateShipment_ManagerAllowed() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CreateShipmentItemRequest item = new CreateShipmentItemRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Kurti", "SKU-001", "M", "Navy", "BIN-1", 1
        );
        CreateShipmentRequest request = new CreateShipmentRequest(
                orderId, "ORD-001", customerId, "Ayesha", "9876543210",
                "Address", "Surat", "Gujarat", "395002", false, BigDecimal.ZERO, List.of(item)
        );

        ShipmentResponse response = new ShipmentResponse(
                UUID.randomUUID(), orderId, "ORD-001", customerId, "Ayesha", "9876543210",
                "Address", "Surat", "Gujarat", "395002", null, null, null,
                ShipmentStatus.PROCESSING, null, null, 0, false, BigDecimal.ZERO, BigDecimal.ZERO,
                null, null, null, null, null, null, null, null, null, null, List.of(), List.of()
        );

        when(shipmentService.createShipment(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/fulfillment/shipments")
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("ORD-001"))
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    @DisplayName("POST /api/v1/fulfillment/shipments denies CUSTOMER access (403)")
    void testCreateShipment_CustomerDenied() throws Exception {
        CreateShipmentRequest request = new CreateShipmentRequest(
                UUID.randomUUID(), "ORD-001", UUID.randomUUID(), "Ayesha", "9876543210",
                "Address", "Surat", "Gujarat", "395002", false, BigDecimal.ZERO, List.of()
        );

        mockMvc.perform(post("/api/v1/fulfillment/shipments")
                        .with(authentication(createAuth(UUID.randomUUID(), "customer1", Role.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/fulfillment/shipments/{id}/pick-pack allows MANAGER to mark picked and packed")
    void testPickPack_ManagerAllowed() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        ShipmentResponse response = new ShipmentResponse(
                shipmentId, UUID.randomUUID(), "ORD-001", UUID.randomUUID(), "Ayesha", "9876543210",
                "Address", "Surat", "Gujarat", "395002", null, null, null,
                ShipmentStatus.PROCESSING, null, null, 0, false, BigDecimal.ZERO, BigDecimal.ZERO,
                null, null, null, null, null, null, null, null, null, null, List.of(), List.of()
        );

        when(shipmentService.pickAndPackShipment(eq(shipmentId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/fulfillment/shipments/" + shipmentId + "/pick-pack")
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(shipmentId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/shipments/order/{orderId} returns sanitized tracking timeline for authenticated user")
    void testGetCustomerTracking_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        CustomerTrackingResponse tracking = new CustomerTrackingResponse(
                orderId, "ORD-001", CustomerShipmentStatus.SHIPPED, "Delhivery",
                "DEL-12345", LocalDate.of(2026, 11, 16), null, 0, null
        );

        when(shipmentService.getCustomerTracking(orderId)).thenReturn(tracking);

        mockMvc.perform(get("/api/v1/shipments/order/" + orderId)
                        .with(authentication(createAuth(UUID.randomUUID(), "customer1", Role.CUSTOMER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-001"))
                .andExpect(jsonPath("$.customerStatus").value("SHIPPED"))
                .andExpect(jsonPath("$.transportAgencyName").value("Delhivery"));
    }

    @Test
    @DisplayName("GET /api/v1/shipments/order/{orderId} requires authentication (401)")
    void testGetCustomerTracking_UnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/shipments/order/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
