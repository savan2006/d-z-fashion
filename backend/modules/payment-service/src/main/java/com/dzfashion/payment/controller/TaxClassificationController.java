package com.dzfashion.payment.controller;

import com.dzfashion.payment.dto.request.CreateTaxClassificationRequest;
import com.dzfashion.payment.dto.response.ActionResponse;
import com.dzfashion.payment.dto.response.TaxClassificationResponse;
import com.dzfashion.payment.service.TaxService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/tax-classifications")
public class TaxClassificationController {

    private final TaxService taxService;

    public TaxClassificationController(TaxService taxService) {
        this.taxService = taxService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<List<TaxClassificationResponse>> getAllTaxClassifications() {
        List<TaxClassificationResponse> responses = taxService.getAllTaxClassifications();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/active")
    public ResponseEntity<List<TaxClassificationResponse>> getActiveTaxClassifications() {
        List<TaxClassificationResponse> responses = taxService.getActiveTaxClassifications();
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<TaxClassificationResponse> createTaxClassification(
            @Valid @RequestBody CreateTaxClassificationRequest request) {
        TaxClassificationResponse response = taxService.createTaxClassification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<TaxClassificationResponse> updateTaxClassification(
            @PathVariable UUID id,
            @Valid @RequestBody CreateTaxClassificationRequest request) {
        TaxClassificationResponse response = taxService.updateTaxClassification(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ActionResponse> deactivateTaxClassification(@PathVariable UUID id) {
        taxService.deactivateTaxClassification(id);
        return ResponseEntity.ok(new ActionResponse(true, "Tax classification deactivated successfully"));
    }
}
