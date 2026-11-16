package com.dzfashion.fulfillment.controller;

import com.dzfashion.fulfillment.dto.request.CreateReturnRequest;
import com.dzfashion.fulfillment.dto.response.PageResponse;
import com.dzfashion.fulfillment.dto.response.ReturnEligibilityResponse;
import com.dzfashion.fulfillment.dto.response.ReturnRequestResponse;
import com.dzfashion.fulfillment.exception.InvalidOperationException;
import com.dzfashion.fulfillment.security.UserPrincipal;
import com.dzfashion.fulfillment.service.ReturnEligibilityService;
import com.dzfashion.fulfillment.service.ReturnWorkflowService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shipments/returns")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerReturnController {

    private final ReturnEligibilityService returnEligibilityService;
    private final ReturnWorkflowService returnWorkflowService;

    public CustomerReturnController(ReturnEligibilityService returnEligibilityService,
                                    ReturnWorkflowService returnWorkflowService) {
        this.returnEligibilityService = returnEligibilityService;
        this.returnWorkflowService = returnWorkflowService;
    }

    @GetMapping("/eligibility/{orderId}")
    public ResponseEntity<ReturnEligibilityResponse> checkEligibility(@PathVariable UUID orderId) {
        ReturnEligibilityResponse response = returnEligibilityService.checkEligibility(orderId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ReturnRequestResponse> createReturnRequest(
            @Valid @RequestBody CreateReturnRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReturnRequestResponse response = returnWorkflowService.createReturnRequest(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-returns")
    public ResponseEntity<PageResponse<ReturnRequestResponse>> getMyReturns(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(returnWorkflowService.getReturnsByCustomer(principal.getId(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReturnRequestResponse> getReturnById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReturnRequestResponse response = returnWorkflowService.getReturnById(id);
        if (!response.customerId().equals(principal.getId())) {
            throw new InvalidOperationException("You are not authorized to view this return request.");
        }
        return ResponseEntity.ok(response);
    }
}
