package com.dzfashion.commerce.controller;

import com.dzfashion.commerce.dto.request.AddTicketMessageRequest;
import com.dzfashion.commerce.dto.request.AssignTicketRequest;
import com.dzfashion.commerce.dto.request.EscalateTicketRequest;
import com.dzfashion.commerce.dto.request.ManualSupportIntakeRequest;
import com.dzfashion.commerce.dto.request.UpdateTicketStatusRequest;
import com.dzfashion.commerce.dto.response.PageResponse;
import com.dzfashion.commerce.dto.response.SupportTicketResponse;
import com.dzfashion.commerce.enums.SupportTicketChannel;
import com.dzfashion.commerce.enums.SupportTicketStatus;
import com.dzfashion.commerce.security.UserPrincipal;
import com.dzfashion.commerce.service.SupportTicketService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/support")
public class AdminSupportController {

    private final SupportTicketService supportTicketService;

    public AdminSupportController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @GetMapping("/tickets")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<PageResponse<SupportTicketResponse>> getAllTickets(
            @RequestParam(required = false) SupportTicketStatus status,
            @RequestParam(required = false) SupportTicketChannel channel,
            @RequestParam(required = false) Boolean escalated,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<SupportTicketResponse> response = supportTicketService.getAllTicketsForStaff(status, channel, escalated, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tickets/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<SupportTicketResponse> getTicketById(@PathVariable UUID id) {
        SupportTicketResponse response = supportTicketService.getStaffTicketById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/tickets/{id}/assign")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<SupportTicketResponse> assignTicket(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AssignTicketRequest request) {
        SupportTicketResponse response = supportTicketService.assignTicket(id, request, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/tickets/{id}/status")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<SupportTicketResponse> updateTicketStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateTicketStatusRequest request) {
        SupportTicketResponse response = supportTicketService.updateTicketStatus(id, request, principal.getId(), principal.getRole().name());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tickets/{id}/messages")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<SupportTicketResponse> addStaffMessage(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddTicketMessageRequest request) {
        SupportTicketResponse response = supportTicketService.addStaffMessage(id, request, principal.getId(), principal.getRole().name());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/manual-intake")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<SupportTicketResponse> recordManualIntake(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ManualSupportIntakeRequest request) {
        SupportTicketResponse response = supportTicketService.recordManualIntake(request, principal.getId(), principal.getRole().name());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/tickets/{id}/escalate")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<SupportTicketResponse> escalateTicket(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EscalateTicketRequest request) {
        SupportTicketResponse response = supportTicketService.escalateTicket(id, request, principal.getId());
        return ResponseEntity.ok(response);
    }
}
