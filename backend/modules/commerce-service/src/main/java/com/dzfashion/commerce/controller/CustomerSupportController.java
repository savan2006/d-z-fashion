package com.dzfashion.commerce.controller;

import com.dzfashion.commerce.dto.request.AddTicketMessageRequest;
import com.dzfashion.commerce.dto.request.CreateSupportTicketRequest;
import com.dzfashion.commerce.dto.response.PageResponse;
import com.dzfashion.commerce.dto.response.SupportTicketResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/support/tickets")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerSupportController {

    private final SupportTicketService supportTicketService;

    public CustomerSupportController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @PostMapping
    public ResponseEntity<SupportTicketResponse> createTicket(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateSupportTicketRequest request) {
        SupportTicketResponse response = supportTicketService.createTicket(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<SupportTicketResponse>> getMyTickets(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<SupportTicketResponse> response = supportTicketService.getTicketsForCustomer(principal.getId(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupportTicketResponse> getTicketById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        SupportTicketResponse response = supportTicketService.getCustomerTicketById(principal.getId(), id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<SupportTicketResponse> addMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AddTicketMessageRequest request) {
        SupportTicketResponse response = supportTicketService.addCustomerMessage(principal.getId(), id, request);
        return ResponseEntity.ok(response);
    }
}
