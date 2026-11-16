package com.dzfashion.payment.controller;

import com.dzfashion.payment.dto.request.RecordCodCollectedRequest;
import com.dzfashion.payment.dto.request.RecordCodPendingRequest;
import com.dzfashion.payment.dto.request.ReversePaymentRequest;
import com.dzfashion.payment.dto.request.SimulatePaymentRequest;
import com.dzfashion.payment.dto.response.PaymentResponse;
import com.dzfashion.payment.service.PaymentRecordService;
import com.dzfashion.payment.security.UserPrincipal;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentRecordService paymentRecordService;

    public PaymentController(PaymentRecordService paymentRecordService) {
        this.paymentRecordService = paymentRecordService;
    }

    @PostMapping("/simulate")
    public ResponseEntity<PaymentResponse> simulatePayment(@Valid @RequestBody SimulatePaymentRequest request) {
        PaymentResponse response = paymentRecordService.simulateOnlinePayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/cod-pending")
    public ResponseEntity<PaymentResponse> recordCodPending(@Valid @RequestBody RecordCodPendingRequest request) {
        PaymentResponse response = paymentRecordService.recordCodPending(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/cod-collected")
    public ResponseEntity<PaymentResponse> recordCodCollected(
            @Valid @RequestBody RecordCodCollectedRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        PaymentResponse response = paymentRecordService.recordCodCollected(request, principal != null ? principal.getId() : null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reverse")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<PaymentResponse> reversePayment(
            @Valid @RequestBody ReversePaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        PaymentResponse response = paymentRecordService.reversePayment(request, principal != null ? principal.getId() : null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(@PathVariable UUID orderId) {
        PaymentResponse response = paymentRecordService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tx/{txRef}")
    public ResponseEntity<PaymentResponse> getPaymentByTxRef(@PathVariable String txRef) {
        PaymentResponse response = paymentRecordService.getPaymentByTransactionReference(txRef);
        return ResponseEntity.ok(response);
    }
}
