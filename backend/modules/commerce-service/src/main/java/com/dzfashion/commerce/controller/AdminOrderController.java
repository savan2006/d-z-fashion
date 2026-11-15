package com.dzfashion.commerce.controller;

import com.dzfashion.commerce.dto.request.ReassignOrderRequest;
import com.dzfashion.commerce.dto.request.ReportShortageRequest;
import com.dzfashion.commerce.dto.request.ResolveShortageRequest;
import com.dzfashion.commerce.dto.response.OrderResponse;
import com.dzfashion.commerce.dto.response.OrderSummaryResponse;
import com.dzfashion.commerce.dto.response.PageResponse;
import com.dzfashion.commerce.dto.response.ShortageIncidentResponse;
import com.dzfashion.commerce.enums.ShortageIncidentStatus;
import com.dzfashion.commerce.security.UserPrincipal;
import com.dzfashion.commerce.service.OrderLifecycleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private final OrderLifecycleService lifecycleService;

    public AdminOrderController(OrderLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<PageResponse<OrderSummaryResponse>> getOrderQueue(@RequestParam(defaultValue = "0") int page,
                                                                           @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        return ResponseEntity.ok(lifecycleService.getManagerOrderQueue(pageable));
    }

    @PostMapping("/{orderNumber}/claim")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<OrderResponse> claimOrder(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable String orderNumber) {
        UUID managerId = getEffectiveUserId(principal);
        return ResponseEntity.ok(lifecycleService.claimOrder(managerId, orderNumber));
    }

    @PostMapping("/{orderNumber}/reassign")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<OrderResponse> reassignOrder(@AuthenticationPrincipal UserPrincipal principal,
                                                       @PathVariable String orderNumber,
                                                       @Valid @RequestBody ReassignOrderRequest request) {
        UUID ownerId = getEffectiveUserId(principal);
        return ResponseEntity.ok(lifecycleService.reassignOrder(ownerId, orderNumber, request));
    }

    @PostMapping("/{orderNumber}/dispatch")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<OrderResponse> dispatchOrder(@AuthenticationPrincipal UserPrincipal principal,
                                                       @PathVariable String orderNumber) {
        UUID actorId = getEffectiveUserId(principal);
        return ResponseEntity.ok(lifecycleService.dispatchOrder(actorId, orderNumber));
    }

    @PostMapping("/{orderNumber}/deliver")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<OrderResponse> deliverOrder(@AuthenticationPrincipal UserPrincipal principal,
                                                      @PathVariable String orderNumber) {
        UUID actorId = getEffectiveUserId(principal);
        return ResponseEntity.ok(lifecycleService.deliverOrder(actorId, orderNumber));
    }

    @PostMapping("/{orderNumber}/shortage")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ShortageIncidentResponse> reportShortage(@AuthenticationPrincipal UserPrincipal principal,
                                                                   @PathVariable String orderNumber,
                                                                   @Valid @RequestBody ReportShortageRequest request) {
        UUID reporterId = getEffectiveUserId(principal);
        return ResponseEntity.ok(lifecycleService.reportShortageIncident(reporterId, orderNumber, request));
    }

    @GetMapping("/shortages")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<List<ShortageIncidentResponse>> listShortages(@RequestParam(required = false) ShortageIncidentStatus status) {
        return ResponseEntity.ok(lifecycleService.listShortageIncidents(status));
    }

    @PostMapping("/shortages/{incidentNumber}/resolve")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ShortageIncidentResponse> resolveShortage(@AuthenticationPrincipal UserPrincipal principal,
                                                                   @PathVariable String incidentNumber,
                                                                   @Valid @RequestBody ResolveShortageRequest request) {
        UUID ownerId = getEffectiveUserId(principal);
        return ResponseEntity.ok(lifecycleService.resolveShortageIncident(ownerId, incidentNumber, request));
    }

    private UUID getEffectiveUserId(UserPrincipal principal) {
        return principal != null ? principal.getId() : UUID.randomUUID();
    }
}
