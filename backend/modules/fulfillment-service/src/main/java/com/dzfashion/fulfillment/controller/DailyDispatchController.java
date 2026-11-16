package com.dzfashion.fulfillment.controller;

import com.dzfashion.fulfillment.dto.request.CreateDispatchBatchRequest;
import com.dzfashion.fulfillment.dto.request.RecordHandoverRequest;
import com.dzfashion.fulfillment.dto.response.DailyDispatchBatchResponse;
import com.dzfashion.fulfillment.dto.response.PageResponse;
import com.dzfashion.fulfillment.security.UserPrincipal;
import com.dzfashion.fulfillment.service.DailyDispatchService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fulfillment/dispatch")
@PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
public class DailyDispatchController {

    private final DailyDispatchService dailyDispatchService;

    public DailyDispatchController(DailyDispatchService dailyDispatchService) {
        this.dailyDispatchService = dailyDispatchService;
    }

    @PostMapping("/batches")
    public ResponseEntity<DailyDispatchBatchResponse> createBatch(
            @Valid @RequestBody CreateDispatchBatchRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID staffId = principal != null ? principal.getId() : null;
        DailyDispatchBatchResponse created = dailyDispatchService.createDailyDispatchBatch(request, staffId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/batches/{batchId}/handover")
    public ResponseEntity<DailyDispatchBatchResponse> recordHandover(
            @PathVariable UUID batchId,
            @Valid @RequestBody RecordHandoverRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID staffId = principal != null ? principal.getId() : null;
        DailyDispatchBatchResponse updated = dailyDispatchService.recordAgencyHandover(batchId, request, staffId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/batches/{batchId}")
    public ResponseEntity<DailyDispatchBatchResponse> getBatchById(@PathVariable UUID batchId) {
        return ResponseEntity.ok(dailyDispatchService.getBatchById(batchId));
    }

    @GetMapping("/batches/date/{date}")
    public ResponseEntity<List<DailyDispatchBatchResponse>> getBatchesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(dailyDispatchService.getBatchesByDate(date));
    }

    @GetMapping("/batches")
    public ResponseEntity<PageResponse<DailyDispatchBatchResponse>> getAllBatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(dailyDispatchService.getAllBatches(pageable));
    }
}
