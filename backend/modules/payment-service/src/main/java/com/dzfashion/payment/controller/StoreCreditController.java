package com.dzfashion.payment.controller;

import com.dzfashion.payment.dto.request.IssueStoreCreditRequest;
import com.dzfashion.payment.dto.request.RedeemStoreCreditRequest;
import com.dzfashion.payment.dto.response.PageResponse;
import com.dzfashion.payment.dto.response.StoreCreditBalanceResponse;
import com.dzfashion.payment.dto.response.StoreCreditEntryResponse;
import com.dzfashion.payment.security.UserPrincipal;
import com.dzfashion.payment.service.StoreCreditService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/store-credit")
public class StoreCreditController {

    private final StoreCreditService storeCreditService;

    public StoreCreditController(StoreCreditService storeCreditService) {
        this.storeCreditService = storeCreditService;
    }

    @GetMapping("/my-balance")
    public ResponseEntity<StoreCreditBalanceResponse> getMyBalance(@AuthenticationPrincipal UserPrincipal principal) {
        StoreCreditBalanceResponse response = storeCreditService.getBalance(principal.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-ledger")
    public ResponseEntity<PageResponse<StoreCreditEntryResponse>> getMyLedger(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<StoreCreditEntryResponse> page = storeCreditService.getCustomerLedger(principal.getId(), pageable);
        return ResponseEntity.ok(PageResponse.from(page));
    }

    @GetMapping("/customer/{customerId}/balance")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<StoreCreditBalanceResponse> getCustomerBalance(@PathVariable UUID customerId) {
        StoreCreditBalanceResponse response = storeCreditService.getBalance(customerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/{customerId}/ledger")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<PageResponse<StoreCreditEntryResponse>> getCustomerLedger(
            @PathVariable UUID customerId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<StoreCreditEntryResponse> page = storeCreditService.getCustomerLedger(customerId, pageable);
        return ResponseEntity.ok(PageResponse.from(page));
    }

    @PostMapping("/issue")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StoreCreditEntryResponse> issueStoreCredit(
            @Valid @RequestBody IssueStoreCreditRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        StoreCreditEntryResponse response = storeCreditService.issueStoreCredit(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/redeem")
    public ResponseEntity<StoreCreditEntryResponse> redeemStoreCredit(
            @Valid @RequestBody RedeemStoreCreditRequest request) {
        StoreCreditEntryResponse response = storeCreditService.redeemStoreCredit(request);
        return ResponseEntity.ok(response);
    }
}
