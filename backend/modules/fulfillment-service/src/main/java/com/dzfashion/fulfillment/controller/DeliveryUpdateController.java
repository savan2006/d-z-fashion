package com.dzfashion.fulfillment.controller;

import com.dzfashion.fulfillment.dto.request.ReceiveReturnAtWarehouseRequest;
import com.dzfashion.fulfillment.dto.request.RecordDeliveryAttemptRequest;
import com.dzfashion.fulfillment.dto.request.RecordDeliverySuccessRequest;
import com.dzfashion.fulfillment.dto.response.ShipmentResponse;
import com.dzfashion.fulfillment.security.UserPrincipal;
import com.dzfashion.fulfillment.service.ShipmentDeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fulfillment/shipments")
@PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
public class DeliveryUpdateController {

    private final ShipmentDeliveryService shipmentDeliveryService;

    public DeliveryUpdateController(ShipmentDeliveryService shipmentDeliveryService) {
        this.shipmentDeliveryService = shipmentDeliveryService;
    }

    @PostMapping("/{id}/attempts")
    public ResponseEntity<ShipmentResponse> recordDeliveryAttempt(
            @PathVariable UUID id,
            @Valid @RequestBody RecordDeliveryAttemptRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID staffId = principal != null ? principal.getId() : null;
        ShipmentResponse updated = shipmentDeliveryService.recordDeliveryAttempt(id, request, staffId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/delivery-success")
    public ResponseEntity<ShipmentResponse> recordDeliverySuccess(
            @PathVariable UUID id,
            @Valid @RequestBody RecordDeliverySuccessRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID staffId = principal != null ? principal.getId() : null;
        ShipmentResponse updated = shipmentDeliveryService.recordDeliverySuccess(id, request, staffId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/warehouse-return")
    public ResponseEntity<ShipmentResponse> receiveReturnAtWarehouse(
            @PathVariable UUID id,
            @Valid @RequestBody ReceiveReturnAtWarehouseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID staffId = principal != null ? principal.getId() : null;
        ShipmentResponse updated = shipmentDeliveryService.receiveReturnAtWarehouse(id, request, staffId);
        return ResponseEntity.ok(updated);
    }
}
