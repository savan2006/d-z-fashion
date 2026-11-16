package com.dzfashion.payment.controller;

import com.dzfashion.payment.dto.request.CreateCodSettlementBatchRequest;
import com.dzfashion.payment.dto.response.CodSettlementBatchResponse;
import com.dzfashion.payment.dto.response.PageResponse;
import com.dzfashion.payment.security.UserPrincipal;
import com.dzfashion.payment.service.CodSettlementService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/cod-settlements")
public class CodSettlementController {

    private final CodSettlementService codSettlementService;

    public CodSettlementController(CodSettlementService codSettlementService) {
        this.codSettlementService = codSettlementService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<CodSettlementBatchResponse> createBatch(
            @Valid @RequestBody CreateCodSettlementBatchRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        CodSettlementBatchResponse response = codSettlementService.createBatch(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{batchId}/approve")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<CodSettlementBatchResponse> approveBatch(
            @PathVariable UUID batchId,
            @AuthenticationPrincipal UserPrincipal principal) {
        CodSettlementBatchResponse response = codSettlementService.approveBatch(batchId, principal.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{batchId}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<CodSettlementBatchResponse> getBatchById(@PathVariable UUID batchId) {
        CodSettlementBatchResponse response = codSettlementService.getBatchById(batchId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<PageResponse<CodSettlementBatchResponse>> getAllBatches(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CodSettlementBatchResponse> page = codSettlementService.getAllBatches(pageable);
        return ResponseEntity.ok(PageResponse.from(page));
    }
}
