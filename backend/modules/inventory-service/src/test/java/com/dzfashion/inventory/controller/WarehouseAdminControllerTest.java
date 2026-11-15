package com.dzfashion.inventory.controller;

import com.dzfashion.inventory.dto.request.ManufactureStockRequest;
import com.dzfashion.inventory.dto.request.ReturnInspectionRequest;
import com.dzfashion.inventory.dto.request.StockAdjustmentRequest;
import com.dzfashion.inventory.dto.request.UpdateThresholdRequest;
import com.dzfashion.inventory.dto.response.PageResponse;
import com.dzfashion.inventory.dto.response.StockMovementResponse;
import com.dzfashion.inventory.dto.response.StockResponse;
import com.dzfashion.inventory.enums.ItemCondition;
import com.dzfashion.inventory.enums.Role;
import com.dzfashion.inventory.enums.StockMovementReason;
import com.dzfashion.inventory.security.JwtAuthenticationFilter;
import com.dzfashion.inventory.security.JwtTokenService;
import com.dzfashion.inventory.security.SecurityConfig;
import com.dzfashion.inventory.security.UserPrincipal;
import com.dzfashion.inventory.service.AlertNotificationService;
import com.dzfashion.inventory.service.InventoryService;
import com.dzfashion.inventory.service.StockMovementService;
import com.dzfashion.inventory.service.WarehouseService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WarehouseAdminController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class WarehouseAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private WarehouseService warehouseService;

    @MockitoBean
    private StockMovementService movementService;

    @MockitoBean
    private AlertNotificationService alertService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("CUSTOMER role is forbidden from adding manufactured stock (403 Forbidden)")
    void customerDeniedFromManufactureStock() throws Exception {
        ManufactureStockRequest request = new ManufactureStockRequest(UUID.randomUUID(), 10, "Unauthorized batch");

        mockMvc.perform(post("/api/v1/admin/inventory/manufacture")
                        .with(user("customer1").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("MANAGER role can add manufactured stock (200 OK)")
    void managerCanAddManufacturedStock() throws Exception {
        UUID managerId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(managerId, "manager1", "mgr@dzfashion.com", Role.MANAGER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        UUID variantId = UUID.randomUUID();
        ManufactureStockRequest request = new ManufactureStockRequest(variantId, 20, "Batch #101");

        when(inventoryService.addManufacturedStock(any(), eq(managerId))).thenReturn(
                new StockResponse(variantId, "DZF-TEST-SKU", 30, 0, 30, 0, 5, Instant.now())
        );

        mockMvc.perform(post("/api/v1/admin/inventory/manufacture")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.physicalQty").value(30))
                .andExpect(jsonPath("$.availableQty").value(30));
    }

    @Test
    @DisplayName("MANAGER role can adjust stock with mandatory reason (200 OK)")
    void managerCanAdjustStock() throws Exception {
        UUID managerId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(managerId, "manager1", "mgr@dzfashion.com", Role.MANAGER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        UUID variantId = UUID.randomUUID();
        StockAdjustmentRequest request = new StockAdjustmentRequest(
                variantId, -2, StockMovementReason.MANUAL_CORRECTION, "Correction after recount"
        );

        when(inventoryService.adjustStock(any(), eq(managerId))).thenReturn(
                new StockResponse(variantId, "DZF-TEST-SKU", 28, 0, 28, 0, 5, Instant.now())
        );

        mockMvc.perform(post("/api/v1/admin/inventory/adjust")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.physicalQty").value(28));
    }

    @Test
    @DisplayName("MANAGER role can inspect returned items and route damaged stock to quarantine (200 OK)")
    void managerCanInspectDamagedReturn() throws Exception {
        UUID managerId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(managerId, "manager1", "mgr@dzfashion.com", Role.MANAGER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        UUID variantId = UUID.randomUUID();
        ReturnInspectionRequest request = new ReturnInspectionRequest(
                variantId, 1, ItemCondition.DAMAGED, "RET-999", "Damaged zip"
        );

        when(inventoryService.inspectAndRestockReturnedItem(any(), eq(managerId))).thenReturn(
                new StockResponse(variantId, "DZF-TEST-SKU", 28, 0, 28, 1, 5, Instant.now())
        );

        mockMvc.perform(post("/api/v1/admin/inventory/return-inspection")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quarantinedQty").value(1));
    }

    @Test
    @DisplayName("MANAGER role is forbidden from setting low-stock threshold (403 Forbidden)")
    void managerDeniedFromThresholdUpdate() throws Exception {
        UUID variantId = UUID.randomUUID();
        UpdateThresholdRequest request = new UpdateThresholdRequest(15);

        mockMvc.perform(put("/api/v1/admin/inventory/variants/" + variantId + "/threshold")
                        .with(user("manager1").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("OWNER role can set low-stock threshold (200 OK)")
    void ownerCanUpdateThreshold() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(ownerId, "owner1", "owner@dzfashion.com", Role.OWNER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        UUID variantId = UUID.randomUUID();
        UpdateThresholdRequest request = new UpdateThresholdRequest(12);

        when(inventoryService.updateThreshold(eq(variantId), eq(12), eq(ownerId))).thenReturn(
                new StockResponse(variantId, "DZF-TEST-SKU", 28, 0, 28, 0, 12, Instant.now())
        );

        mockMvc.perform(put("/api/v1/admin/inventory/variants/" + variantId + "/threshold")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lowStockThreshold").value(12));
    }

    @Test
    @DisplayName("OWNER role can view stock movement audit ledger (200 OK)")
    void ownerCanViewMovements() throws Exception {
        UUID movementId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        when(movementService.getAllMovements(any())).thenReturn(
                new PageResponse<>(List.of(
                        new StockMovementResponse(movementId, variantId, 20, 20, StockMovementReason.NEW_MANUFACTURE, "INIT", "SKU-001", actorId, Instant.now())
                ), 0, 20, 1, 1, true)
        );

        mockMvc.perform(get("/api/v1/admin/inventory/movements")
                        .with(user("owner1").roles("OWNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reason").value("NEW_MANUFACTURE"))
                .andExpect(jsonPath("$.content[0].changeQty").value(20));
    }
}
