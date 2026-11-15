package com.dzfashion.inventory.controller;

import com.dzfashion.inventory.dto.request.CommitReservationRequest;
import com.dzfashion.inventory.dto.request.ReleaseStockRequest;
import com.dzfashion.inventory.dto.request.ReserveStockRequest;
import com.dzfashion.inventory.dto.response.ActionResponse;
import com.dzfashion.inventory.dto.response.ReservationResponse;
import com.dzfashion.inventory.security.UserPrincipal;
import com.dzfashion.inventory.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reserve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationResponse> reserveStock(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReserveStockRequest request) {
        UUID actorId = principal != null ? principal.getId() : UUID.randomUUID();
        ReservationResponse response = reservationService.reserveStock(request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/release")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ActionResponse> releaseStock(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReleaseStockRequest request) {
        UUID actorId = principal != null ? principal.getId() : UUID.randomUUID();
        ActionResponse response = reservationService.releaseStock(request, actorId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/commit")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ActionResponse> commitReservation(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CommitReservationRequest request) {
        UUID actorId = principal != null ? principal.getId() : UUID.randomUUID();
        ActionResponse response = reservationService.commitReservation(request, actorId);
        return ResponseEntity.ok(response);
    }
}
