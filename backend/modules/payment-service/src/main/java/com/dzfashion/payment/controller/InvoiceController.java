package com.dzfashion.payment.controller;

import com.dzfashion.payment.dto.request.GenerateDispatchInvoiceRequest;
import com.dzfashion.payment.dto.request.GenerateReceiptRequest;
import com.dzfashion.payment.dto.response.InvoiceResponse;
import com.dzfashion.payment.dto.response.PageResponse;
import com.dzfashion.payment.security.UserPrincipal;
import com.dzfashion.payment.service.InvoiceService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping("/receipt")
    public ResponseEntity<InvoiceResponse> generateReceipt(@Valid @RequestBody GenerateReceiptRequest request) {
        InvoiceResponse response = invoiceService.generateReceipt(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/dispatch")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<InvoiceResponse> generateDispatchInvoice(
            @Valid @RequestBody GenerateDispatchInvoiceRequest request) {
        InvoiceResponse response = invoiceService.generateDispatchInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable UUID id) {
        InvoiceResponse response = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/number/{invoiceNumber}")
    public ResponseEntity<InvoiceResponse> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        InvoiceResponse response = invoiceService.getInvoiceByNumber(invoiceNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesForOrder(@PathVariable UUID orderId) {
        List<InvoiceResponse> responses = invoiceService.getInvoicesForOrder(orderId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/my-invoices")
    public ResponseEntity<PageResponse<InvoiceResponse>> getMyInvoices(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<InvoiceResponse> page = invoiceService.getCustomerInvoices(principal.getId(), pageable);
        return ResponseEntity.ok(PageResponse.from(page));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<PageResponse<InvoiceResponse>> getAllInvoices(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<InvoiceResponse> page = invoiceService.getAllInvoices(pageable);
        return ResponseEntity.ok(PageResponse.from(page));
    }
}
