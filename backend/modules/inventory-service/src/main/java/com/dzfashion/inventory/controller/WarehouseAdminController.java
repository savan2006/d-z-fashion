package com.dzfashion.inventory.controller;

import com.dzfashion.inventory.dto.request.InitializeStockRequest;
import com.dzfashion.inventory.dto.request.ManufactureStockRequest;
import com.dzfashion.inventory.dto.request.ReturnInspectionRequest;
import com.dzfashion.inventory.dto.request.StockAdjustmentRequest;
import com.dzfashion.inventory.dto.request.UpdateThresholdRequest;
import com.dzfashion.inventory.dto.response.LowStockAlertResponse;
import com.dzfashion.inventory.dto.response.PageResponse;
import com.dzfashion.inventory.dto.response.StockMovementResponse;
import com.dzfashion.inventory.dto.response.StockResponse;
import com.dzfashion.inventory.dto.response.WarehouseResponse;
import com.dzfashion.inventory.security.UserPrincipal;
import com.dzfashion.inventory.service.AlertNotificationService;
import com.dzfashion.inventory.service.InventoryService;
import com.dzfashion.inventory.service.StockMovementService;
import com.dzfashion.inventory.service.WarehouseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/inventory")
public class WarehouseAdminController {

    private final InventoryService inventoryService;
    private final WarehouseService warehouseService;
    private final StockMovementService movementService;
    private final AlertNotificationService alertService;

    public WarehouseAdminController(InventoryService inventoryService,
                                    WarehouseService warehouseService,
                                    StockMovementService movementService,
                                    AlertNotificationService alertService) {
        this.inventoryService = inventoryService;
        this.warehouseService = warehouseService;
        this.movementService = movementService;
        this.alertService = alertService;
    }

    @PostMapping("/init")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<StockResponse> initializeStock(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody InitializeStockRequest request) {
        StockResponse response = inventoryService.initializeStock(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/manufacture")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<StockResponse> addManufacturedStock(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ManufactureStockRequest request) {
        StockResponse response = inventoryService.addManufacturedStock(request, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<StockResponse> adjustStock(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StockAdjustmentRequest request) {
        StockResponse response = inventoryService.adjustStock(request, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/return-inspection")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<StockResponse> inspectReturnedItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReturnInspectionRequest request) {
        StockResponse response = inventoryService.inspectAndRestockReturnedItem(request, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/variants/{variantId}/threshold")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StockResponse> updateThreshold(
            @PathVariable UUID variantId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateThresholdRequest request) {
        StockResponse response = inventoryService.updateThreshold(variantId, request.lowStockThreshold(), principal.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<PageResponse<StockResponse>> getLowStockQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<StockResponse> response = inventoryService.getLowStockQueue(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<List<LowStockAlertResponse>> getActiveAlerts() {
        List<LowStockAlertResponse> alerts = alertService.getActiveAlerts();
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/movements")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<PageResponse<StockMovementResponse>> getAllMovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<StockMovementResponse> movements = movementService.getAllMovements(pageable);
        return ResponseEntity.ok(movements);
    }

    @GetMapping("/warehouses")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<List<WarehouseResponse>> getWarehouses() {
        List<WarehouseResponse> warehouses = warehouseService.getAllWarehouses();
        return ResponseEntity.ok(warehouses);
    }
}
