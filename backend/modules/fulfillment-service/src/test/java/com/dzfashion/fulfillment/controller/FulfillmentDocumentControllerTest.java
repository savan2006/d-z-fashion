package com.dzfashion.fulfillment.controller;

import com.dzfashion.fulfillment.dto.document.DailyPickingListDocument;
import com.dzfashion.fulfillment.dto.document.DispatchManifestDocument;
import com.dzfashion.fulfillment.dto.document.PackingSlipDocument;
import com.dzfashion.fulfillment.enums.Role;
import com.dzfashion.fulfillment.security.JwtAuthenticationFilter;
import com.dzfashion.fulfillment.security.JwtTokenService;
import com.dzfashion.fulfillment.security.SecurityConfig;
import com.dzfashion.fulfillment.security.UserPrincipal;
import com.dzfashion.fulfillment.service.FulfillmentDocumentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FulfillmentDocumentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class FulfillmentDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FulfillmentDocumentService fulfillmentDocumentService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    private UsernamePasswordAuthenticationToken createAuth(UUID id, String username, Role role) {
        UserPrincipal principal = new UserPrincipal(id, username, username + "@example.com", role);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    @DisplayName("GET /api/v1/fulfillment/documents/picking-list allows MANAGER to view combined picking list")
    void testGetPickingList_ManagerAllowed() throws Exception {
        DailyPickingListDocument doc = new DailyPickingListDocument(
                "Surat Central Warehouse",
                Instant.now(),
                5,
                12,
                List.of()
        );
        when(fulfillmentDocumentService.generateCombinedPickingList()).thenReturn(doc);

        mockMvc.perform(get("/api/v1/fulfillment/documents/picking-list")
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warehouseName").value("Surat Central Warehouse"))
                .andExpect(jsonPath("$.totalUnitsToPick").value(12));
    }

    @Test
    @DisplayName("GET /api/v1/fulfillment/documents/packing-slip/{orderId} allows MANAGER to view packing slip")
    void testGetPackingSlip_ManagerAllowed() throws Exception {
        UUID orderId = UUID.randomUUID();
        PackingSlipDocument doc = new PackingSlipDocument(
                "ORD-001",
                "Surat Central Warehouse",
                Instant.now(),
                "Ayesha Khan",
                "98765****0",
                "Flat 402, Lotus Apartments",
                "Mumbai",
                "Maharashtra",
                "400050",
                true,
                new BigDecimal("2500.00"),
                List.of()
        );
        when(fulfillmentDocumentService.generatePackingSlip(orderId)).thenReturn(doc);

        mockMvc.perform(get("/api/v1/fulfillment/documents/packing-slip/" + orderId)
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-001"))
                .andExpect(jsonPath("$.maskedPhone").value("98765****0"));
    }

    @Test
    @DisplayName("GET /api/v1/fulfillment/documents/dispatch-manifest/{batchId} allows MANAGER to view manifest")
    void testGetDispatchManifest_ManagerAllowed() throws Exception {
        UUID batchId = UUID.randomUUID();
        DispatchManifestDocument doc = new DispatchManifestDocument(
                "DISPATCH-20261116-0001",
                LocalDate.of(2026, 11, 16),
                Instant.now(),
                "Delhivery",
                "Surat Central Warehouse",
                10,
                new BigDecimal("15000.00"),
                "MANIFEST-99",
                List.of()
        );
        when(fulfillmentDocumentService.generateDispatchManifest(batchId)).thenReturn(doc);

        mockMvc.perform(get("/api/v1/fulfillment/documents/dispatch-manifest/" + batchId)
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchNumber").value("DISPATCH-20261116-0001"))
                .andExpect(jsonPath("$.totalCodAmount").value(15000.00));
    }

    @Test
    @DisplayName("GET /api/v1/fulfillment/documents/picking-list denies CUSTOMER access (403)")
    void testGetPickingList_CustomerForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/fulfillment/documents/picking-list")
                        .with(authentication(createAuth(UUID.randomUUID(), "customer1", Role.CUSTOMER))))
                .andExpect(status().isForbidden());
    }
}
