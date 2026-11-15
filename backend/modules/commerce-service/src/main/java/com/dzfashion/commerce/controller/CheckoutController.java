package com.dzfashion.commerce.controller;

import com.dzfashion.commerce.dto.request.CheckoutPreviewRequest;
import com.dzfashion.commerce.dto.request.ConfirmOrderRequest;
import com.dzfashion.commerce.dto.response.CheckoutPreviewResponse;
import com.dzfashion.commerce.dto.response.OrderResponse;
import com.dzfashion.commerce.security.UserPrincipal;
import com.dzfashion.commerce.service.CheckoutOrchestrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/checkout")
@PreAuthorize("hasRole('CUSTOMER')")
public class CheckoutController {

    private final CheckoutOrchestrationService orchestrationService;

    public CheckoutController(CheckoutOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping("/preview")
    public ResponseEntity<CheckoutPreviewResponse> previewCheckout(@AuthenticationPrincipal UserPrincipal principal,
                                                                  @Valid @RequestBody CheckoutPreviewRequest request) {
        UUID customerId = getEffectiveUserId(principal);
        return ResponseEntity.ok(orchestrationService.previewCheckout(customerId, request));
    }

    @PostMapping("/confirm")
    public ResponseEntity<OrderResponse> confirmOrder(@AuthenticationPrincipal UserPrincipal principal,
                                                      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                      @Valid @RequestBody ConfirmOrderRequest request) {
        UUID customerId = getEffectiveUserId(principal);
        OrderResponse response = orchestrationService.confirmOrder(customerId, request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private UUID getEffectiveUserId(UserPrincipal principal) {
        return principal != null ? principal.getId() : UUID.randomUUID();
    }
}
