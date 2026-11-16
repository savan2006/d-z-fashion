package com.dzfashion.fulfillment.controller;

import com.dzfashion.fulfillment.dto.request.CreateAgencyCodSettlementRequest;
import com.dzfashion.fulfillment.dto.response.AgencyCodSettlementResponse;
import com.dzfashion.fulfillment.dto.response.PageResponse;
import com.dzfashion.fulfillment.dto.response.ShipmentResponse;
import com.dzfashion.fulfillment.security.UserPrincipal;
import com.dzfashion.fulfillment.service.AgencyCodSettlementService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fulfillment/cod-settlements")
public class AgencyCodSettlementController {

    private final AgencyCodSettlementService agencyCodSettlementService;

    public AgencyCodSettlementController(AgencyCodSettlementService agencyCodSettlementService) {
        this.agencyCodSettlementService = agencyCodSettlementService;
    }

    @PostMapping("/prepare")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<AgencyCodSettlementResponse> prepareSettlement(
            @Valid @RequestBody CreateAgencyCodSettlementRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID staffId = principal != null ? principal.getId() : null;
        AgencyCodSettlementResponse prepared = agencyCodSettlementService.prepareSettlement(request, staffId);
        return ResponseEntity.status(HttpStatus.CREATED).body(prepared);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<AgencyCodSettlementResponse> approveSettlement(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID ownerId = principal != null ? principal.getId() : null;
        AgencyCodSettlementResponse approved = agencyCodSettlementService.approveSettlement(id, ownerId);
        return ResponseEntity.ok(approved);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<AgencyCodSettlementResponse> getSettlementById(@PathVariable UUID id) {
        return ResponseEntity.ok(agencyCodSettlementService.getSettlementById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<PageResponse<AgencyCodSettlementResponse>> getAllSettlements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(agencyCodSettlementService.getAllSettlements(pageable));
    }

    @GetMapping("/agency/{agencyId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<List<AgencyCodSettlementResponse>> getSettlementsByAgency(@PathVariable UUID agencyId) {
        return ResponseEntity.ok(agencyCodSettlementService.getSettlementsByAgency(agencyId));
    }

    @GetMapping("/eligible/{agencyId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<List<ShipmentResponse>> getEligibleShipmentsForAgency(@PathVariable UUID agencyId) {
        return ResponseEntity.ok(agencyCodSettlementService.getEligibleShipmentsForAgency(agencyId));
    }
}
