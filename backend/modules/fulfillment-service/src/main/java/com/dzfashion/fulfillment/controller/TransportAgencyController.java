package com.dzfashion.fulfillment.controller;

import com.dzfashion.fulfillment.dto.request.CreateTransportAgencyRequest;
import com.dzfashion.fulfillment.dto.request.UpdateTransportAgencyRequest;
import com.dzfashion.fulfillment.dto.response.TransportAgencyResponse;
import com.dzfashion.fulfillment.service.TransportAgencyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/fulfillment/agencies")
@PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
public class TransportAgencyController {

    private final TransportAgencyService transportAgencyService;

    public TransportAgencyController(TransportAgencyService transportAgencyService) {
        this.transportAgencyService = transportAgencyService;
    }

    @PostMapping
    public ResponseEntity<TransportAgencyResponse> createAgency(@Valid @RequestBody CreateTransportAgencyRequest request) {
        TransportAgencyResponse created = transportAgencyService.createAgency(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransportAgencyResponse> updateAgency(@PathVariable UUID id,
                                                                @Valid @RequestBody UpdateTransportAgencyRequest request) {
        TransportAgencyResponse updated = transportAgencyService.updateAgency(id, request);
        return ResponseEntity.ok(updated);
    }

    @GetMapping
    public ResponseEntity<List<TransportAgencyResponse>> getAllAgencies() {
        return ResponseEntity.ok(transportAgencyService.getAllAgencies());
    }

    @GetMapping("/active")
    public ResponseEntity<List<TransportAgencyResponse>> getActiveAgencies() {
        return ResponseEntity.ok(transportAgencyService.getActiveAgencies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransportAgencyResponse> getAgencyById(@PathVariable UUID id) {
        return ResponseEntity.ok(transportAgencyService.getAgencyById(id));
    }
}
