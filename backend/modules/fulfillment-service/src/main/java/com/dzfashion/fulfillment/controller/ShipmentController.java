package com.dzfashion.fulfillment.controller;

import com.dzfashion.fulfillment.dto.request.CreateShipmentRequest;
import com.dzfashion.fulfillment.dto.response.CustomerTrackingResponse;
import com.dzfashion.fulfillment.dto.response.PageResponse;
import com.dzfashion.fulfillment.dto.response.ShipmentResponse;
import com.dzfashion.fulfillment.enums.ShipmentStatus;
import com.dzfashion.fulfillment.security.UserPrincipal;
import com.dzfashion.fulfillment.service.ShipmentService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping("/api/v1/fulfillment/shipments")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ShipmentResponse> createShipment(@Valid @RequestBody CreateShipmentRequest request) {
        ShipmentResponse created = shipmentService.createShipment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/api/v1/fulfillment/shipments/{id}/pick-pack")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ShipmentResponse> pickAndPackShipment(@PathVariable UUID id,
                                                                @AuthenticationPrincipal UserPrincipal principal) {
        UUID staffId = principal != null ? principal.getId() : null;
        ShipmentResponse updated = shipmentService.pickAndPackShipment(id, staffId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/api/v1/fulfillment/shipments/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ShipmentResponse> getShipmentById(@PathVariable UUID id) {
        return ResponseEntity.ok(shipmentService.getShipmentById(id));
    }

    @GetMapping("/api/v1/fulfillment/shipments/by-order/{orderId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ShipmentResponse> getShipmentByOrderId(@PathVariable UUID orderId) {
        return ResponseEntity.ok(shipmentService.getShipmentByOrderId(orderId));
    }

    @GetMapping("/api/v1/fulfillment/shipments")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<PageResponse<ShipmentResponse>> getAllShipments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(shipmentService.getAllShipments(pageable));
    }

    @GetMapping("/api/v1/fulfillment/shipments/status/{status}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<List<ShipmentResponse>> getShipmentsByStatus(@PathVariable ShipmentStatus status) {
        return ResponseEntity.ok(shipmentService.getShipmentsByStatus(status));
    }

    // Customer tracking endpoint: returns sanitized status timeline without internal operational notes
    @GetMapping("/api/v1/shipments/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CustomerTrackingResponse> getCustomerTracking(@PathVariable UUID orderId) {
        CustomerTrackingResponse tracking = shipmentService.getCustomerTracking(orderId);
        return ResponseEntity.ok(tracking);
    }

    // Customer order shipments list
    @GetMapping("/api/v1/shipments/customer/my-orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PageResponse<ShipmentResponse>> getMyShipments(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(shipmentService.getCustomerShipments(principal.getId(), pageable));
    }
}
