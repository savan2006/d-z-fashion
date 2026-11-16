package com.dzfashion.fulfillment.controller;

import com.dzfashion.fulfillment.dto.request.ConfirmExchangePaymentRequest;
import com.dzfashion.fulfillment.dto.request.PerformInspectionRequest;
import com.dzfashion.fulfillment.dto.request.RecordPickupRequest;
import com.dzfashion.fulfillment.dto.request.RecordReturnArrivalRequest;
import com.dzfashion.fulfillment.dto.request.RejectRefundRequest;
import com.dzfashion.fulfillment.dto.request.SchedulePickupRequest;
import com.dzfashion.fulfillment.dto.response.PageResponse;
import com.dzfashion.fulfillment.dto.response.ReturnRequestResponse;
import com.dzfashion.fulfillment.security.UserPrincipal;
import com.dzfashion.fulfillment.service.ReturnWorkflowService;
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
@RequestMapping("/api/v1/fulfillment/returns")
public class AdminReturnController {

    private final ReturnWorkflowService returnWorkflowService;

    public AdminReturnController(ReturnWorkflowService returnWorkflowService) {
        this.returnWorkflowService = returnWorkflowService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<PageResponse<ReturnRequestResponse>> getAllReturns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(returnWorkflowService.getAllReturns(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ReturnRequestResponse> getReturnById(@PathVariable UUID id) {
        return ResponseEntity.ok(returnWorkflowService.getReturnById(id));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<List<ReturnRequestResponse>> getReturnsByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(returnWorkflowService.getReturnsByOrder(orderId));
    }

    @PostMapping("/{id}/schedule-pickup")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ReturnRequestResponse> schedulePickup(
            @PathVariable UUID id,
            @Valid @RequestBody SchedulePickupRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID staffId = principal != null ? principal.getId() : null;
        ReturnRequestResponse response = returnWorkflowService.schedulePickup(id, request, staffId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/record-pickup")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ReturnRequestResponse> recordPickup(
            @PathVariable UUID id,
            @RequestBody(required = false) RecordPickupRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID staffId = principal != null ? principal.getId() : null;
        RecordPickupRequest req = request != null ? request : new RecordPickupRequest(null, null);
        ReturnRequestResponse response = returnWorkflowService.recordPickup(id, req, staffId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/record-arrival")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ReturnRequestResponse> recordArrival(
            @PathVariable UUID id,
            @RequestBody(required = false) RecordReturnArrivalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID staffId = principal != null ? principal.getId() : null;
        RecordReturnArrivalRequest req = request != null ? request : new RecordReturnArrivalRequest(null, null);
        ReturnRequestResponse response = returnWorkflowService.recordArrivalAtSurat(id, req, staffId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/inspect")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ReturnRequestResponse> performInspection(
            @PathVariable UUID id,
            @Valid @RequestBody PerformInspectionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID staffId = principal != null ? principal.getId() : null;
        ReturnRequestResponse response = returnWorkflowService.performInspection(id, request, staffId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/confirm-exchange-payment")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ReturnRequestResponse> confirmExchangePayment(
            @PathVariable UUID id,
            @RequestBody(required = false) ConfirmExchangePaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID staffId = principal != null ? principal.getId() : null;
        ConfirmExchangePaymentRequest req = request != null ? request : new ConfirmExchangePaymentRequest(null, null);
        ReturnRequestResponse response = returnWorkflowService.confirmExchangePayment(id, req, staffId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/dispatch-exchange")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ReturnRequestResponse> dispatchExchangeReplacement(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID staffId = principal != null ? principal.getId() : null;
        ReturnRequestResponse response = returnWorkflowService.dispatchExchangeReplacement(id, staffId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/request-refund")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ReturnRequestResponse> requestRefund(
            @PathVariable UUID id,
            @RequestParam(required = false) String recommendationNotes,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID staffId = principal != null ? principal.getId() : null;
        ReturnRequestResponse response = returnWorkflowService.requestRefund(id, recommendationNotes, staffId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve-refund")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ReturnRequestResponse> approveRefund(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID ownerId = principal != null ? principal.getId() : null;
        ReturnRequestResponse response = returnWorkflowService.approveRefund(id, ownerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject-refund")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ReturnRequestResponse> rejectRefund(
            @PathVariable UUID id,
            @Valid @RequestBody RejectRefundRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID ownerId = principal != null ? principal.getId() : null;
        ReturnRequestResponse response = returnWorkflowService.rejectRefund(id, request, ownerId);
        return ResponseEntity.ok(response);
    }
}
