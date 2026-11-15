package com.dzfashion.commerce.controller;

import com.dzfashion.commerce.dto.request.CancelOrderRequest;
import com.dzfashion.commerce.dto.response.OrderResponse;
import com.dzfashion.commerce.dto.response.OrderSummaryResponse;
import com.dzfashion.commerce.dto.response.PageResponse;
import com.dzfashion.commerce.enums.Role;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderLifecycleService lifecycleService;

    public OrderController(OrderLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PageResponse<OrderSummaryResponse>> getMyOrders(@AuthenticationPrincipal UserPrincipal principal,
                                                                         @RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "10") int size) {
        UUID customerId = getEffectiveUserId(principal);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        return ResponseEntity.ok(lifecycleService.getCustomerOrders(customerId, pageable));
    }

    @GetMapping("/{orderNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> getOrderDetails(@AuthenticationPrincipal UserPrincipal principal,
                                                         @PathVariable String orderNumber) {
        UUID actorId = getEffectiveUserId(principal);
        Role actorRole = principal != null ? principal.getRole() : Role.CUSTOMER;
        return ResponseEntity.ok(lifecycleService.getOrderDetails(actorId, actorRole, orderNumber));
    }

    @PostMapping("/{orderNumber}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> cancelOrder(@AuthenticationPrincipal UserPrincipal principal,
                                                     @PathVariable String orderNumber,
                                                     @Valid @RequestBody CancelOrderRequest request) {
        UUID customerId = getEffectiveUserId(principal);
        Role actorRole = principal != null ? principal.getRole() : Role.CUSTOMER;
        return ResponseEntity.ok(lifecycleService.cancelOrder(customerId, actorRole, orderNumber, request));
    }

    private UUID getEffectiveUserId(UserPrincipal principal) {
        return principal != null ? principal.getId() : UUID.randomUUID();
    }
}
